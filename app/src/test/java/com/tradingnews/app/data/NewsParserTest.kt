package com.tradingnews.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsParserTest {
    @Test
    fun parsesValidHttpsArticleAndClassifiesCategory() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "Central bank signals a new interest rate decision",
            url = "https://news.example.com/story?id=1",
            seen = "20261111T123400Z",
            domain = "attacker.example"
        )))

        val parsed = NewsParser.parse(root)

        assertEquals(1, parsed.size)
        assertEquals(NewsCategories.MARKETS, parsed.single().category)
        assertEquals("news.example.com", parsed.single().source)
        assertEquals("2026-11-11T12:34:00Z", parsed.single().seenAt.toString())
    }

    @Test
    fun rejectsInsecureAndNonPublicArticleUrls() {
        val unsafeUrls = listOf(
            "http://example.com/story",
            "javascript:alert(1)",
            "https://user:pass@example.com/story",
            "https://localhost/story",
            "https://news.local/story",
            "https://127.0.0.1/story",
            "https://192.168.1.10/story",
            "https://[::1]/story",
            "https://example.com:8443/story"
        )
        val articles = JSONArray()
        unsafeUrls.forEachIndexed { index, url ->
            articles.put(article("Unsafe $index", url, "20261111T123400Z"))
        }
        articles.put(article("Good story", "https://example.com/good", "20261111T123400Z"))

        val parsed = NewsParser.parse(JSONObject().put("articles", articles))

        assertEquals(1, parsed.size)
        assertEquals("Good story", parsed.single().title)
    }

    @Test
    fun ignoresRecordsWithoutUsableTimestamp() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "Missing timestamp",
            url = "https://example.com/story",
            seen = "not-a-date"
        )))
        assertTrue(NewsParser.parse(root).isEmpty())
    }

    @Test
    fun acceptsIsoTimestampFromCompactLocalCache() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "Cached inflation report",
            url = "https://example.com/cache",
            seen = "2026-11-11T12:34:00Z"
        )))
        val parsed = NewsParser.parse(root)
        assertEquals(1, parsed.size)
        assertEquals("2026-11-11T12:34:00Z", parsed.single().seenAt.toString())
    }

    @Test
    fun sortsNewestFirstAndDeduplicatesUrls() {
        val root = JSONObject().put("articles", JSONArray()
            .put(article("Old", "https://example.com/old", "20261111T100000Z"))
            .put(article("New", "https://example.com/new", "20261111T120000Z"))
            .put(article("Duplicate", "https://example.com/new", "20261111T120100Z")))

        val parsed = NewsParser.parse(root)

        assertEquals(listOf("New", "Old"), parsed.map { it.title })
    }

    @Test
    fun limitsParsedResultsToFiftyArticles() {
        val articles = JSONArray()
        repeat(260) { index ->
            articles.put(article(
                title = "Market story $index",
                url = "https://example.com/story-$index",
                seen = "20261111T123400Z"
            ))
        }

        val parsed = NewsParser.parse(JSONObject().put("articles", articles))

        assertEquals(50, parsed.size)
    }

    @Test
    fun classifiesCoreMarketTopicsWithoutAssigningImpactScores() {
        assertEquals(NewsCategories.CRYPTO, NewsParser.classifyCategory("Bitcoin ETF decision approaches"))
        assertEquals(NewsCategories.ENERGY, NewsParser.classifyCategory("OPEC discusses crude oil output"))
        assertEquals(NewsCategories.GEOPOLITICS, NewsParser.classifyCategory("New sanctions follow a regional conflict"))
        assertEquals(NewsCategories.COMPANIES, NewsParser.classifyCategory("Company announces quarterly earnings"))
        assertEquals(NewsCategories.OTHER, NewsParser.classifyCategory("New museum opens in the city"))
    }

    @Test
    fun limitsInstrumentMatchesToTheSelectedWatchlist() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "US CPI inflation and Federal Reserve interest rates",
            url = "https://example.com/us-macro",
            seen = "20261111T123400Z"
        )))

        val parsed = NewsParser.parse(root, watchlist = setOf("EURUSD"))

        assertEquals(listOf("EURUSD"), parsed.single().instrumentMatches.map { it.symbol })
    }

    @Test
    fun selectedEmptyWatchlistDisablesInstrumentMatches() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "Bitcoin rises after market update",
            url = "https://example.com/bitcoin",
            seen = "20261111T123400Z"
        )))

        val parsed = NewsParser.parse(root, watchlist = emptySet())

        assertTrue(parsed.single().instrumentMatches.isEmpty())
    }

    @Test
    fun parserRejectsUrlsContainingCredentialsOrUnsafeSchemes() {
        assertNull(NewsParser.normalizedSafeHttpsUrl("javascript:alert(1)"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("http://example.com"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("https://user:pass@example.com"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("https://localhost/path"))
        assertNull(NewsParser.normalizedSafeHttpsUrl(""))
        assertTrue(NewsParser.normalizedSafeHttpsUrl("https://example.com/news")!!.startsWith("https://"))
    }

    @Test
    fun normalizesPathsAndAcceptsDefaultHttpsPort() {
        val normalized = NewsParser.normalizedSafeHttpsUrl("https://example.com/a/../b")
        assertEquals("https://example.com/b", normalized)
        assertEquals("https://example.com:443/path", NewsParser.normalizedSafeHttpsUrl("https://example.com:443/path"))
    }

    private fun article(
        title: String,
        url: String,
        seen: String,
        domain: String = ""
    ) = JSONObject()
        .put("title", title)
        .put("url", url)
        .put("domain", domain)
        .put("seendate", seen)
}
