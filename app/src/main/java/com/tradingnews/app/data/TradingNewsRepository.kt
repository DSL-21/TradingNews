package com.tradingnews.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

class TradingNewsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)

    fun readCachedFeed(): NewsFeed? {
        return try {
            val payload = preferences.getString(KEY_PAYLOAD, null) ?: return null
            val retrievedAtValue = preferences.getString(KEY_RETRIEVED_AT, null)
            val retrievedAt = retrievedAtValue?.let(::parseInstant)
                ?: throw IOException("Cached news timestamp is missing or invalid")

            val ageSeconds = Duration.between(retrievedAt, Instant.now()).seconds
            if (ageSeconds < 0 || ageSeconds > MAX_CACHE_AGE_SECONDS) {
                clearCache()
                return null
            }

            val root = JSONObject(payload)
            if (root.optJSONArray("articles") == null) {
                throw IOException("Cached news data has an invalid structure")
            }

            NewsFeed(
                articles = NewsParser.parse(root),
                retrievedAt = retrievedAt,
                status = FeedStatus.CACHED,
                notice = "Showing locally cached headlines. Refresh to check for newer stories."
            )
        } catch (_: Exception) {
            // A malformed or type-corrupted preference must never crash feed startup.
            clearCache()
            null
        }
    }

    suspend fun load(forceRefresh: Boolean): NewsFeed = withContext(Dispatchers.IO) {
        val cached = readCachedFeed()
        val now = Instant.now()
        val lastFetch = preferences.getLong(KEY_LAST_FETCH_EPOCH, 0L)
        val secondsSinceLastFetch = now.epochSecond - lastFetch

        if (cached != null && !forceRefresh && Duration.between(cached.retrievedAt, now).seconds in 0 until CACHE_TTL_SECONDS) {
            return@withContext cached.copy(notice = "Cached headlines retrieved at ${cached.retrievedAt}.")
        }

        if (lastFetch > 0L && secondsSinceLastFetch in 0 until MIN_REFRESH_INTERVAL_SECONDS) {
            if (cached != null) {
                return@withContext cached.copy(
                    notice = "Refresh is rate-limited on this device. Showing cached headlines from ${cached.retrievedAt}."
                )
            }
            throw IOException("Please wait before retrying the live news feed.")
        }

        preferences.edit().putLong(KEY_LAST_FETCH_EPOCH, now.epochSecond).apply()

        try {
            val rawPayload = fetchGdeltPayload()
            val root = JSONObject(rawPayload)
            if (root.optJSONArray("articles") == null) {
                throw IOException("News provider returned an unexpected JSON structure")
            }

            val articles = NewsParser.parse(root)
            val retrievedAt = Instant.now()
            val compactCache = serializeCache(articles)
            preferences.edit()
                .putString(KEY_PAYLOAD, compactCache)
                .putString(KEY_RETRIEVED_AT, retrievedAt.toString())
                .apply()

            NewsFeed(
                articles = articles,
                retrievedAt = retrievedAt,
                status = FeedStatus.LIVE,
                notice = if (articles.isEmpty()) {
                    "GDELT returned no valid headlines in the selected 24-hour window."
                } else {
                    "Live headlines from GDELT DOC 2.0."
                }
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (cached != null) {
                cached.copy(
                    status = FeedStatus.CACHED,
                    notice = "The live feed could not be refreshed (${safeErrorName(error)}). Showing cached headlines from ${cached.retrievedAt}."
                )
            } else {
                throw IOException("The live news feed is currently unavailable.", error)
            }
        }
    }

    private fun fetchGdeltPayload(): String {
        val encodedQuery = URLEncoder.encode(MARKET_NEWS_QUERY, StandardCharsets.UTF_8.name())
        val requestUrl = "${GDELT_DOC_URL}?query=${encodedQuery}&mode=artlist&format=json&maxrecords=50&sort=datedesc&timespan=24h"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("News provider returned HTTP $responseCode")
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_RESPONSE_BYTES) {
                throw IOException("News response exceeded the size limit")
            }

            val body = InputStreamReader(connection.inputStream, Charsets.UTF_8).use { reader ->
                readBounded(reader, MAX_RESPONSE_CHARS)
            }
            if (body.isBlank()) throw IOException("News provider returned an empty response")
            return body
        } finally {
            connection?.disconnect()
        }
    }

    private fun serializeCache(articles: List<NewsItem>): String {
        val cachedArticles = JSONArray()
        articles.forEach { article ->
            cachedArticles.put(
                JSONObject()
                    .put("title", article.title)
                    .put("url", article.url)
                    .put("domain", article.source)
                    .put("seendate", article.seenAt.toString())
            )
        }
        return JSONObject().put("articles", cachedArticles).toString()
    }

    private fun readBounded(reader: InputStreamReader, maxChars: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(8_192)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            if (output.length + read > maxChars) {
                throw IOException("News response exceeded the size limit")
            }
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private fun clearCache() {
        preferences.edit()
            .remove(KEY_PAYLOAD)
            .remove(KEY_RETRIEVED_AT)
            .apply()
    }

    private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

    private fun safeErrorName(error: Exception): String = when (error) {
        is java.net.SocketTimeoutException -> "network timeout"
        is java.net.UnknownHostException -> "network unavailable"
        is IOException -> error.message?.take(80) ?: "network error"
        else -> "temporary error"
    }

    private companion object {
        const val CACHE_PREFS = "trading_news_cache_v2"
        const val KEY_PAYLOAD = "gdelt_payload"
        const val KEY_RETRIEVED_AT = "retrieved_at"
        const val KEY_LAST_FETCH_EPOCH = "last_fetch_epoch"
        const val GDELT_DOC_URL = "https://api.gdeltproject.org/api/v2/doc/doc"
        const val MARKET_NEWS_QUERY = """(inflation OR "central bank" OR "interest rate" OR "monetary policy" OR "stock market" OR forex OR currency OR bonds OR "oil price" OR OPEC OR "natural gas" OR gold OR war OR conflict OR sanctions OR tariffs OR election OR bitcoin OR cryptocurrency OR "company earnings" OR bankruptcy OR "banking crisis")"""
        const val USER_AGENT = "TradingNews-Android/0.2.0"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_RESPONSE_BYTES = 2_000_000L
        const val MAX_RESPONSE_CHARS = 2_000_000
        const val CACHE_TTL_SECONDS = 10 * 60L
        const val MAX_CACHE_AGE_SECONDS = 24 * 60 * 60L
        const val MIN_REFRESH_INTERVAL_SECONDS = 60L
    }
}
