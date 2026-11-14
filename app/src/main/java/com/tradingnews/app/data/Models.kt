package com.tradingnews.app.data

import java.time.Instant

enum class FeedStatus {
    CONNECTING,
    LIVE,
    CACHED,
    OFFLINE
}

object NewsCategories {
    const val ALL = "all"
    const val MARKETS = "markets"
    const val GEOPOLITICS = "geopolitics"
    const val ENERGY = "energy"
    const val CRYPTO = "crypto"
    const val COMPANIES = "companies"
    const val OTHER = "other"
}

data class NewsItem(
    val id: String,
    val title: String,
    val source: String,
    val seenAt: Instant,
    val url: String,
    val category: String,
    val instrumentMatches: List<InstrumentMatch> = emptyList()
)

data class NewsFeed(
    val articles: List<NewsItem>,
    val retrievedAt: Instant,
    val status: FeedStatus,
    val notice: String
)


/** Approximate size of app-owned headline metadata and preference values. */
data class NewsStorageInfo(
    val cachedArticleCount: Int,
    val cacheBytes: Long,
    val settingsBytes: Long,
    val retrievedAt: Instant?
) {
    val estimatedUserDataBytes: Long
        get() = cacheBytes + settingsBytes
}
