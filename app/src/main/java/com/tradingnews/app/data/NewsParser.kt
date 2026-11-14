package com.tradingnews.app.data

import org.json.JSONObject
import java.net.IDN
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
    private val whitespaceRegex = Regex("\\s+")
    private val categoryPatterns by lazy {
        mapOf(
            NewsCategories.CRYPTO to CRYPTO_TERMS.map(::wordPattern),
            NewsCategories.ENERGY to ENERGY_TERMS.map(::wordPattern),
            NewsCategories.GEOPOLITICS to GEOPOLITICS_TERMS.map(::wordPattern),
            NewsCategories.COMPANIES to COMPANY_TERMS.map(::wordPattern),
            NewsCategories.MARKETS to MARKET_TERMS.map(::wordPattern)
        )
    }

    private fun wordPattern(term: String) =
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])")

    fun parse(
        root: JSONObject,
        watchlist: Set<String> = InstrumentRelevanceEngine.supportedSymbols.toSet()
    ): List<NewsItem> {
        val articles = root.optJSONArray("articles") ?: return emptyList()
        val seenUrls = HashSet<String>()
        val parsed = ArrayList<NewsItem>(minOf(articles.length(), MAX_ARTICLES))

        for (index in 0 until minOf(articles.length(), MAX_INPUT_RECORDS)) {
            val item = articles.optJSONObject(index) ?: continue
            val title = item.optString("title", "").trim().replace(whitespaceRegex, " ")
            val rawUrl = item.optString("url", "").trim()
            val url = normalizedSafeHttpsUrl(rawUrl) ?: continue
            if (title.isBlank() || !seenUrls.add(url)) continue

            val seenAt = parseGdeltSeenDate(item.optString("seendate", "")) ?: continue
            val source = URI(url).host?.trimEnd('.')?.takeIf { it.isNotBlank() } ?: continue

            parsed += NewsItem(
                id = sha256(url).take(20),
                title = title.take(MAX_TITLE_LENGTH),
                source = source.take(MAX_SOURCE_LENGTH),
                seenAt = seenAt,
                url = url,
                category = classifyCategory(title),
                instrumentMatches = InstrumentRelevanceEngine.match(title, watchlist)
            )
        }

        return parsed.sortedByDescending { it.seenAt }.take(MAX_ARTICLES)
    }

    internal fun normalizedSafeHttpsUrl(value: String): String? {
        return try {
            val uri = URI(value)
            val rawHost = uri.host ?: return null
            val host = IDN.toASCII(rawHost.trimEnd('.'), IDN.USE_STD3_ASCII_RULES)
            val labels = host.lowercase(Locale.ROOT).split('.')

            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (uri.rawUserInfo != null || (uri.rawFragment?.length ?: 0) > MAX_FRAGMENT_LENGTH) return null
            if (host.isBlank() || host.length > 253 || !host.contains('.')) return null
            if (labels.any { it.isBlank() || it.length > 63 || it.startsWith('-') || it.endsWith('-') }) return null
            if (labels.any { !it.matches(DNS_LABEL_REGEX) }) return null

            val lowerHost = host.lowercase(Locale.ROOT)
            if (lowerHost == "localhost" ||
                lowerHost.endsWith(".localhost") ||
                lowerHost.endsWith(".local") ||
                lowerHost.endsWith(".internal") ||
                lowerHost.endsWith(".test") ||
                lowerHost.endsWith(".invalid")
            ) return null

            // Reject numeric, octal, hexadecimal and shortened IPv4 host forms.
            if (looksLikeNonDnsIp(labels)) return null
            if (uri.port != -1 && uri.port != 443) return null
            uri.normalize().toASCIIString()
        } catch (_: Exception) {
            null
        }
    }

    private fun looksLikeNonDnsIp(labels: List<String>): Boolean {
        if (labels.size in 1..4 && labels.all { label ->
                label.toLongOrNull() != null ||
                    label.startsWith("0x", ignoreCase = true) && label.drop(2).isNotEmpty() &&
                    label.drop(2).all { it.digitToIntOrNull(16) != null }
            }
        ) return true
        return false
    }

    internal fun classifyCategory(title: String): String {
        val value = title.lowercase(Locale.ROOT)
        return listOf(
            NewsCategories.CRYPTO,
            NewsCategories.ENERGY,
            NewsCategories.GEOPOLITICS,
            NewsCategories.COMPANIES,
            NewsCategories.MARKETS
        ).firstOrNull { category ->
            categoryPatterns[category].orEmpty().any { it.containsMatchIn(value) }
        } ?: NewsCategories.OTHER
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
    private const val MAX_ARTICLES = 50
    private const val MAX_INPUT_RECORDS = 250
    private const val MAX_FRAGMENT_LENGTH = 2_048
    private val DNS_LABEL_REGEX = Regex("[a-z0-9-]+")
}
