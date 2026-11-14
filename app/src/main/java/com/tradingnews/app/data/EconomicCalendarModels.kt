package com.tradingnews.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class EconomicEvent(
    val id: String,
    val seriesCode: String,
    val name: String,
    val country: String,
    val currency: String,
    val category: String,
    val impact: String,
    val date: LocalDate,
    val releaseAtUtc: Instant?,
    val timePrecision: String?,
    val allDay: Boolean,
    val consensus: String?,
    val prior: String?,
    val actual: String?,
    val url: String?,
    val sourceCredit: String?
)

data class EconomicCalendarSnapshot(
    val events: List<EconomicEvent>,
    val fetchedAt: Instant,
    val generatedAt: Instant?,
    val truncated: Boolean,
    val attributionName: String,
    val attributionUrl: String,
    val isStale: Boolean
)

internal object EconomicCalendarParser {
    private val currencyByCountry = mapOf(
        "US" to "USD", "GB" to "GBP", "UK" to "GBP", "EU" to "EUR",
        "DE" to "EUR", "JP" to "JPY", "CA" to "CAD", "AU" to "AUD",
        "NZ" to "NZD", "CH" to "CHF", "CN" to "CNY", "IN" to "INR",
        "SE" to "SEK", "NO" to "NOK", "HK" to "HKD"
    )

    fun parse(root: JSONObject, now: Instant = Instant.now(), staleAfterSeconds: Long = 24 * 60 * 60L): EconomicCalendarSnapshot {
        val eventsJson = root.optJSONArray("events")
            ?: throw IllegalArgumentException("Calendar payload is missing its events array")
        require(eventsJson.length() <= MAX_EVENTS) { "Calendar payload contains too many events" }

        val events = LinkedHashMap<String, EconomicEvent>()
        for (index in 0 until eventsJson.length()) {
            val item = eventsJson.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            val series = item.optString("series_code").trim()
            val dateText = item.optString("date").trim()
            if (id.isBlank() || name.isBlank() || series.isBlank()) continue

            val date = try {
                LocalDate.parse(dateText)
            } catch (_: DateTimeParseException) {
                continue
            }
            val country = if (item.isNull("country")) "Global" else item.optString("country").trim().ifBlank { "Global" }
            val countryCode = country.uppercase()
            val releaseAt = optionalString(item, "release_at_utc")?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            }
            val url = optionalString(item, "url")?.takeIf(::isSafeFinanceCalendarUrl)

            events[id] = EconomicEvent(
                id = id.take(160),
                seriesCode = series.take(100),
                name = name.take(240),
                country = country.take(80),
                currency = currencyByCountry[countryCode] ?: countryCode.take(4),
                category = item.optString("category").take(100),
                impact = optionalString(item, "impact")?.uppercase()?.take(20) ?: "UNKNOWN",
                date = date,
                releaseAtUtc = releaseAt,
                timePrecision = optionalString(item, "time_precision")?.take(60),
                allDay = item.optBoolean("all_day", false),
                consensus = optionalString(item, "consensus")?.take(80),
                prior = optionalString(item, "prior")?.take(80),
                actual = optionalString(item, "actual")?.take(80),
                url = url,
                sourceCredit = optionalString(item, "source_credit")?.take(160)
                    ?: optionalString(item, "source")?.take(160)
            )
        }

        val attribution = root.optJSONObject("attribution")
            ?: throw IllegalArgumentException("Calendar payload is missing provider attribution")
        val attributionName = attribution.optString("source").trim()
        val attributionUrl = attribution.optString("url").trim()
        require(attributionName.isNotBlank()) { "Calendar attribution source is missing" }
        require(isSafeFinanceCalendarUrl(attributionUrl)) { "Calendar attribution URL is invalid" }

        val fetchedAt = optionalString(root, "fetched_at")?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        } ?: throw IllegalArgumentException("Calendar snapshot timestamp is missing")
        val generatedAt = optionalString(root, "generated_at")?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        }
        val age = java.time.Duration.between(fetchedAt, now).seconds

        return EconomicCalendarSnapshot(
            events = events.values.sortedWith(compareBy<EconomicEvent> { it.date }
                .thenBy { it.releaseAtUtc ?: Instant.MAX }
                .thenBy { it.name }),
            fetchedAt = fetchedAt,
            generatedAt = generatedAt,
            truncated = root.optBoolean("truncated", false),
            attributionName = attributionName,
            attributionUrl = attributionUrl,
            isStale = age < 0 || age > staleAfterSeconds
        )
    }

    private fun optionalString(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun isSafeFinanceCalendarUrl(value: String): Boolean {
        return runCatching {
            val uri = java.net.URI(value)
            uri.scheme.equals("https", true) &&
                uri.host?.lowercase() in setOf("financecalendar.com", "www.financecalendar.com") &&
                uri.rawUserInfo == null
        }.getOrDefault(false)
    }

    private const val MAX_EVENTS = 200
}
