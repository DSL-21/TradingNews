package com.tradingnews.app.data

import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Parses GDELT DOC article-list data. Invalid or unsafe article records are discarded. */
internal object NewsParser {
    private val gdeltDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

    fun parse(root: JSONObject): List<NewsItem> {
        val articles = root.optJSONArray("articles") ?: return emptyList()
        val seenUrls = HashSet<String>()
        val parsed = ArrayList<NewsItem>(articles.length())

        for (index in 0 until articles.length()) {
            val item = articles.optJSONObject(index) ?: continue
            val title = item.optString("title", "").trim().replace(Regex("\\s+"), " ")
            val rawUrl = item.optString("url", "").trim()
            val url = normalizedSafeHttpsUrl(rawUrl) ?: continue
            if (title.isBlank() || !seenUrls.add(url)) continue

            val seenAt = parseGdeltSeenDate(item.optString("seendate", "")) ?: continue
            val source = item.optString("domain", "").trim().ifBlank {
                runCatching { URI(url).host.orEmpty() }.getOrDefault("")
            }
            if (source.isBlank()) continue

            parsed += NewsItem(
                id = sha256(url).take(20),
                title = title.take(MAX_TITLE_LENGTH),
                source = source.take(MAX_SOURCE_LENGTH),
                seenAt = seenAt,
                url = url,
                category = classifyCategory(title)
            )
        }

        return parsed.sortedByDescending { it.seenAt }
    }

    internal fun normalizedSafeHttpsUrl(value: String): String? {
        return try {
            val uri = URI(value)
            val host = uri.host ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (uri.rawUserInfo != null || host.equals("localhost", ignoreCase = true)) return null
            if (host.isBlank() || host.any { it.isWhitespace() }) return null
            if (uri.port !in -1..65535) return null
            uri.normalize().toASCIIString()
        } catch (_: Exception) {
            null
        }
    }

    internal fun classifyCategory(title: String): String {
        val value = title.lowercase(Locale.ROOT)
        return when {
            containsAny(value, CRYPTO_TERMS) -> NewsCategories.CRYPTO
            containsAny(value, ENERGY_TERMS) -> NewsCategories.ENERGY
            containsAny(value, GEOPOLITICS_TERMS) -> NewsCategories.GEOPOLITICS
            containsAny(value, COMPANY_TERMS) -> NewsCategories.COMPANIES
            containsAny(value, MARKET_TERMS) -> NewsCategories.MARKETS
            else -> NewsCategories.OTHER
        }
    }

    private fun containsAny(text: String, terms: List<String>): Boolean = terms.any { term ->
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])").containsMatchIn(text)
    }

    private fun parseGdeltSeenDate(value: String): Instant? {
        val cleaned = value.trim()
        if (cleaned.isEmpty()) return null
        return try {
            LocalDateTime.parse(cleaned, gdeltDateFormatter).toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(cleaned)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private val CRYPTO_TERMS = listOf("bitcoin", "ethereum", "crypto", "cryptocurrency", "blockchain", "digital asset")
    private val ENERGY_TERMS = listOf("opec", "oil price", "crude oil", "natural gas", "lng", "energy market", "pipeline", "refinery")
    private val GEOPOLITICS_TERMS = listOf("war", "conflict", "invasion", "sanction", "tariff", "missile", "ceasefire", "coup", "blockade", "election")
    private val COMPANY_TERMS = listOf("earnings", "profit warning", "quarterly results", "merger", "acquisition", "bankruptcy", "ipo", "restructuring")
    private val MARKET_TERMS = listOf("stock market", "equities", "forex", "currency", "bond yield", "treasury", "inflation", "central bank", "interest rate", "federal reserve", "ecb", "boj", "boe", "cpi", "pmi", "nonfarm", "unemployment", "gdp")

    private const val MAX_TITLE_LENGTH = 300
    private const val MAX_SOURCE_LENGTH = 120
}
