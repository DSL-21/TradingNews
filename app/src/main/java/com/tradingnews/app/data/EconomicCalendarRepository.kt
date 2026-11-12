package com.tradingnews.app.data

import com.tradingnews.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class EconomicCalendarRepository {
    fun isConfigured(): Boolean = BuildConfig.BACKEND_BASE_URL.isNotBlank()

    fun load(from: LocalDate = LocalDate.now(), to: LocalDate = from.plusDays(7)): List<EconomicEvent> {
        val base = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')
        if (base.isBlank()) throw BackendNotConfiguredException()
        if (!base.startsWith("https://", ignoreCase = true)) {
            throw IOException("The configured API endpoint must use HTTPS.")
        }
        if (to.isBefore(from) || to.isAfter(from.plusDays(31))) {
            throw IOException("Calendar date range is invalid.")
        }

        val query = "from=${URLEncoder.encode(from.toString(), "UTF-8")}&to=${URLEncoder.encode(to.toString(), "UTF-8")}"
        val url = URL("$base/api/v1/calendar?$query")
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
            }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Calendar backend returned HTTP $status")
            if (connection.contentLengthLong > MAX_RESPONSE_BYTES) {
                throw IOException("Calendar response exceeded the size limit")
            }
            val payload = InputStreamReader(connection.inputStream, Charsets.UTF_8).use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (output.length + count > MAX_RESPONSE_CHARS) throw IOException("Calendar response exceeded the size limit")
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
            val root = JSONObject(payload)
            if (root.optString("status") != "ok") {
                throw IOException(root.optString("message", "Calendar backend reported an error"))
            }
            val records = root.optJSONArray("events") ?: JSONArray()
            return buildList {
                for (index in 0 until minOf(records.length(), MAX_EVENTS)) {
                    val item = records.optJSONObject(index) ?: continue
                    val id = item.optString("id", "").trim()
                    val name = item.optString("event", "").trim()
                    val time = parseProviderTime(item.optString("date", "")) ?: continue
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        EconomicEvent(
                            id = id.take(40),
                            time = time,
                            country = item.optString("country", "").take(80),
                            category = item.optString("category", "").take(100),
                            event = name.take(180),
                            previous = item.optString("previous", "").take(60),
                            forecast = item.optString("forecast", "").take(60),
                            actual = item.optString("actual", "").take(60),
                            importance = item.optInt("importance", 1).coerceIn(1, 3),
                            currency = item.optString("currency", "").take(8),
                            timeIsEstimated = item.optBoolean("time_is_estimated", false),
                            updatedAt = parseProviderTime(item.optString("last_update", ""))
                        )
                    )
                }
            }.sortedBy { it.time }
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseProviderTime(value: String): Instant? {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return null
        return runCatching { Instant.parse(cleaned) }.getOrElse {
            runCatching { LocalDateTime.parse(cleaned).toInstant(ZoneOffset.UTC) }.getOrNull()
        }
    }

    class BackendNotConfiguredException : IOException("Trading News backend is not configured.")

    private companion object {
        const val MAX_RESPONSE_BYTES = 1_500_000L
        const val MAX_RESPONSE_CHARS = 1_500_000
        const val MAX_EVENTS = 500
    }
}
