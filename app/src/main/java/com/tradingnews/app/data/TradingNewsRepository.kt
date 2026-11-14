package com.tradingnews.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

class TradingNewsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)

    fun getStorageInfo(): NewsStorageInfo {
        val payload = safeString(KEY_PAYLOAD)
        val retrievedAtValue = safeString(KEY_RETRIEVED_AT)
        val lastFetchEpoch = safeLong(KEY_LAST_FETCH_EPOCH)
        val cacheGeneration = safeLong(KEY_CACHE_GENERATION)
        val cacheBytes = (
            (payload?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0) +
                (retrievedAtValue?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0) +
                (if (lastFetchEpoch > 0L) 8 else 0) +
                (if (cacheGeneration > 0L) 8 else 0)
            ).toLong()
        val count = payload?.let {
            runCatching {
                JSONObject(it).optJSONArray("articles")?.length()?.coerceAtMost(MAX_CACHE_ARTICLE_COUNT) ?: 0
            }.getOrDefault(0)
        } ?: 0
        val settings = appContext.getSharedPreferences(NewsWorkScheduler.PREFERENCES, Context.MODE_PRIVATE).all
        return NewsStorageInfo(
            cachedArticleCount = count,
            cacheBytes = cacheBytes,
            settingsBytes = estimatePreferenceBytes(settings),
            retrievedAt = retrievedAtValue?.let(::parseInstant)
        )
    }

    /**
     * Clears the news cache and advances its generation. An in-flight request that
     * started before this clear is not allowed to recreate the deleted cache.
     */
    fun clearNewsCache() {
        synchronized(CACHE_MUTATION_LOCK) {
            val current = safeLong(KEY_CACHE_GENERATION)
            val next = if (current == Long.MAX_VALUE) 1L else current + 1L
            preferences.edit()
                .clear()
                .putLong(KEY_CACHE_GENERATION, next)
                .apply()
        }
    }

    fun getCacheGeneration(): Long = synchronized(CACHE_MUTATION_LOCK) {
        safeLong(KEY_CACHE_GENERATION)
    }

    private fun safeString(key: String): String? =
        runCatching { preferences.getString(key, null) }.getOrNull()

    private fun safeLong(key: String): Long =
        runCatching { preferences.getLong(key, 0L) }.getOrDefault(0L)

    private fun estimatePreferenceBytes(values: Map<String, *>): Long =
        values.entries.sumOf { (key, value) ->
            key.toByteArray(StandardCharsets.UTF_8).size.toLong() + when (value) {
                is String -> value.toByteArray(StandardCharsets.UTF_8).size.toLong()
                is Set<*> -> value.sumOf {
                    it?.toString()?.toByteArray(StandardCharsets.UTF_8)?.size?.toLong() ?: 0L
                }
                is Boolean -> 1L
                is Int -> 4L
                is Long -> 8L
                is Float -> 4L
                else -> 0L
            }
        }

    fun readCachedFeed(): NewsFeed? = synchronized(CACHE_MUTATION_LOCK) {
        try {
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
                articles = NewsParser.parse(root, NewsPreferences.getWatchlist(appContext)),
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

    suspend fun load(forceRefresh: Boolean): NewsFeed = fetchMutex.withLock {
        withContext(Dispatchers.IO) {
        val (generationAtStart, cached) = synchronized(CACHE_MUTATION_LOCK) {
            val cachedFeed = readCachedFeed()
            safeLong(KEY_CACHE_GENERATION) to cachedFeed
        }
        val now = Instant.now()
        val lastFetch = safeLong(KEY_LAST_FETCH_EPOCH)
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

        val fetchMayProceed = synchronized(CACHE_MUTATION_LOCK) {
            if (safeLong(KEY_CACHE_GENERATION) != generationAtStart) {
                false
            } else {
                preferences.edit().putLong(KEY_LAST_FETCH_EPOCH, now.epochSecond).apply()
                true
            }
        }
        if (!fetchMayProceed) {
            return@withContext cached ?: NewsFeed(
                articles = emptyList(),
                retrievedAt = now,
                status = FeedStatus.OFFLINE,
                notice = "The news cache was cleared before the refresh started."
            )
        }

        try {
            val rawPayload = fetchGdeltPayload()
            val root = JSONObject(rawPayload)
            if (root.optJSONArray("articles") == null) {
                throw IOException("News provider returned an unexpected JSON structure")
            }

            val articles = NewsParser.parse(root, NewsPreferences.getWatchlist(appContext))
            val retrievedAt = Instant.now()
            val compactCache = serializeCache(articles)
            // A cancelled foreground refresh must not recreate data after a user-triggered clear.
            currentCoroutineContext().ensureActive()
            synchronized(CACHE_MUTATION_LOCK) {
                if (safeLong(KEY_CACHE_GENERATION) == generationAtStart) {
                    preferences.edit()
                        .putString(KEY_PAYLOAD, compactCache)
                        .putString(KEY_RETRIEVED_AT, retrievedAt.toString())
                        .apply()
                }
            }

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
    }

    private fun fetchGdeltPayload(): String {
        val encodedQuery = URLEncoder.encode(MARKET_NEWS_QUERY, StandardCharsets.UTF_8.name())
        val requestUrl = "${GDELT_DOC_URL}?query=${encodedQuery}&mode=ArtList&format=JSON&maxrecords=50&sort=DateDesc&timespan=24h"
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

            val mediaType = connection.contentType
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase()
            if (mediaType !in ACCEPTED_JSON_MEDIA_TYPES) {
                throw IOException("News provider returned a non-JSON response")
            }

            val body = connection.inputStream.use { input ->
                readBounded(input, MAX_RESPONSE_BYTES)
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

    private fun readBounded(input: java.io.InputStream, maxBytes: Long): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (output.size().toLong() + read.toLong() > maxBytes) {
                throw IOException("News response exceeded the size limit")
            }
            output.write(buffer, 0, read)
        }
        return String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun clearCache() {
        synchronized(CACHE_MUTATION_LOCK) {
            val current = safeLong(KEY_CACHE_GENERATION)
            val next = if (current == Long.MAX_VALUE) 1L else current + 1L
            preferences.edit()
                .remove(KEY_PAYLOAD)
                .remove(KEY_RETRIEVED_AT)
                .remove(KEY_LAST_FETCH_EPOCH)
                .putLong(KEY_CACHE_GENERATION, next)
                .apply()
        }
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
        const val KEY_CACHE_GENERATION = "cache_generation"
        val CACHE_MUTATION_LOCK = Any()
        val fetchMutex = Mutex()
        const val GDELT_DOC_URL = "https://api.gdeltproject.org/api/v2/doc/doc"
        const val MARKET_NEWS_QUERY = """(forex OR currency OR "exchange rate" OR inflation OR CPI OR PCE OR "central bank" OR "Federal Reserve" OR FOMC OR ECB OR "Bank of England" OR BOE OR "Bank of Japan" OR BOJ OR "interest rate" OR "monetary policy" OR "bond yield" OR "treasury yield" OR unemployment OR "nonfarm payrolls" OR "jobs report" OR GDP OR PMI OR "retail sales" OR recession OR "stock market" OR equities OR "company earnings" OR "oil price" OR OPEC OR "natural gas" OR "gold price" OR commodities OR "supply chain" OR shipping OR "port strike" OR "trade war" OR tariffs OR sanctions OR election OR "government shutdown" OR "debt ceiling" OR "sovereign debt" OR "banking crisis" OR "credit crisis" OR war OR conflict OR "strait of hormuz" OR "red sea" OR "suez canal" OR earthquake OR flood OR "natural disaster" OR bitcoin OR cryptocurrency)"""
        const val USER_AGENT = "TradingNews-Android/0.7.1"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_CACHE_ARTICLE_COUNT = 50
        const val MAX_RESPONSE_BYTES = 2_000_000L
        val ACCEPTED_JSON_MEDIA_TYPES = setOf("application/json", "text/json")
        const val CACHE_TTL_SECONDS = 10 * 60L
        const val MAX_CACHE_AGE_SECONDS = 24 * 60 * 60L
        const val MIN_REFRESH_INTERVAL_SECONDS = 60L
    }
}
