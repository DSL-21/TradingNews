package com.tradingnews.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant

class EconomicCalendarRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    suspend fun load(forceRefresh: Boolean = false): EconomicCalendarSnapshot = withContext(Dispatchers.IO) {
        val cached = readCache()
        val now = Instant.now()
        if (!forceRefresh && cached != null &&
            Duration.between(cached.fetchedAt, now).seconds in 0 until CACHE_TTL_SECONDS
        ) return@withContext cached

        try {
            val raw = fetchSnapshot()
            val parsed = EconomicCalendarParser.parse(JSONObject(raw))
            preferences.edit()
                .putString(KEY_PAYLOAD, raw)
                .putString(KEY_FETCHED_AT, parsed.fetchedAt.toString())
                .apply()
            parsed
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (cached != null) {
                cached.copy(isStale = true)
            } else {
                throw IOException(
                    "The native calendar is not published yet. The repository needs the one-time Finance Calendar API setup.",
                    error
                )
            }
        }
    }

    private fun readCache(): EconomicCalendarSnapshot? {
        val raw = preferences.getString(KEY_PAYLOAD, null) ?: return null
        return try {
            EconomicCalendarParser.parse(JSONObject(raw))
        } catch (_: Exception) {
            preferences.edit().remove(KEY_PAYLOAD).remove(KEY_FETCHED_AT).apply()
            null
        }
    }

    private fun fetchSnapshot(): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(SNAPSHOT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Cache-Control", "no-cache")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Calendar snapshot returned HTTP $code")
            if (connection.contentLengthLong > MAX_RESPONSE_BYTES) {
                throw IOException("Calendar snapshot exceeded the size limit")
            }
            val body = InputStreamReader(connection.inputStream, Charsets.UTF_8).use { reader ->
                val out = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (out.length + count > MAX_RESPONSE_CHARS) {
                        throw IOException("Calendar snapshot exceeded the size limit")
                    }
                    out.append(buffer, 0, count)
                }
                out.toString()
            }
            if (body.isBlank()) throw IOException("Calendar snapshot is empty")
            return body
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val PREFERENCES = "trading_news_calendar_cache"
        const val KEY_PAYLOAD = "snapshot"
        const val KEY_FETCHED_AT = "fetched_at"
        const val SNAPSHOT_URL = "https://raw.githubusercontent.com/DSL-21/TradingNews/main/data/economic-calendar.json"
        const val USER_AGENT = "TradingNews-Android/0.4.0"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_RESPONSE_BYTES = 1_000_000L
        const val MAX_RESPONSE_CHARS = 1_000_000
        const val CACHE_TTL_SECONDS = 60 * 60L
    }
}
