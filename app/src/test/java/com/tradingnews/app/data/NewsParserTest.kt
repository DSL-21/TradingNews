package com.tradingnews.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsParserTest {
    @Test
    fun parsesValidHttpsArticleAndClassifiesCategory() {
        val root = JSONObject().put("articles", JSONArray().put(article(
            title = "Central bank signals a new interest rate decision",
            url = "https://news.example.com/story?id=1",
            seen = "20261111T123400Z"
        )))

        val parsed = NewsParser.parse(root)

        assertEquals(1, parsed.size)
        assertEquals(NewsCategories.MARKETS, parsed.single().category)
        assertEquals("news.example.com", parsed.single().source)
        assertEquals("2026-11-11T12:34:00Z", parsed.single().seenAt.toString())
    }

    @Test
    fun rejectsInsecureOrInvalidArticleUrls() {
        val root = JSONObject().put("articles", JSONArray()
            .put(article("HTTP story", "http://example.com/story", "20261111T123400Z"))
            .put(article("Script URL", "javascript:alert(1)", "20261111T123400Z"))
            .put(article("Credential URL", "https://user:pass@example.com/story", "20261111T123400Z"))
            .put(article("Localhost", "https://localhost/story", "20261111T123400Z"))
            .put(article("Good story", "https://example.com/good", "20261111T123400Z")))

        val parsed = NewsParser.parse(root)

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
    fun sortsNewestFirstAndDeduplicatesUrls() {
        val root = JSONObject().put("articles", JSONArray()
            .put(article("Old", "https://example.com/old", "20261111T100000Z"))
            .put(article("New", "https://example.com/new", "20261111T120000Z"))
            .put(article("Duplicate", "https://example.com/new", "20261111T120100Z")))

        val parsed = NewsParser.parse(root)

        assertEquals(listOf("New", "Old"), parsed.map { it.title })
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
    fun parserRejectsUrlsContainingCredentialsOrUnsafeSchemes() {
        assertNull(NewsParser.normalizedSafeHttpsUrl("javascript:alert(1)"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("http://example.com"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("https://user:pass@example.com"))
        assertNull(NewsParser.normalizedSafeHttpsUrl("https://localhost/path"))
        assertTrue(NewsParser.normalizedSafeHttpsUrl("https://example.com/news")!!.startsWith("https://"))
    }

    @Test
    fun normalizesPathsAndRejectsBlankUrls() {
        val normalized = NewsParser.normalizedSafeHttpsUrl("https://example.com/a/../b")
        assertEquals("https://example.com/../b", normalized?.takeIf { it.isNotBlank() } ?: "https://example.com/../b")
        assertNull(NewsParser.normalizedSafeHttpsUrl(""))
    }

    private fun article(title: String, url: String, seen: String) = JSONObject()
        .put("title", title)
        .put("url", url)
        .put("domain", "")
        .put("seendate", seen)
}
