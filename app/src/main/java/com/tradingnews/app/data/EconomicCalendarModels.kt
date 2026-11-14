package com.tradingnews.app.data

import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class EconomicEvent(
    val id: String,
    val seriesCode: String,
    val name: String,
    val country: String?,
    val category: String,
    val impact: String,
    val date: LocalDate,
    val releaseAtUtc: Instant?,
    val timePrecision: String?,
    val allDay: Boolean,
    val timeWindowStart: String?,
    val timeWindowEnd: String?,
    val dateWindowStart: String?,
    val dateWindowEnd: String?,
    val status: String?,
    val eventUrl: String?,
    val sourceName: String?,
    val sourceUrl: String?,
    val sourceLicenseJson: String?,
    val sourceRightsJson: String?,
    val figuresAvailable: Boolean?,
    val resultAvailable: Boolean?,
    val lastVerifiedAt: Instant?,
    val dataStale: Boolean?
)

data class FreeDeveloperUsage(
    val plan: String,
    val accessScope: String,
    val quotaBasis: String,
    val dailyLimit: Int,
    val monthlyLimit: Int,
    val usedToday: Int,
    val remainingToday: Int,
    val usedThisMonth: Int,
    val remainingThisMonth: Int,
    val dailyResetsAt: Instant?,
    val monthlyResetsAt: Instant?,
    val maxEventsPerResponse: Int,
    val windowFrom: LocalDate?,
    val windowTo: LocalDate?,
    val windowTimezone: String?,
    val upgradeUrl: String?,
    val resumeAtUtc: Instant?
)

data class EconomicCalendarSnapshot(
    val events: List<EconomicEvent>,
    val fetchedAt: Instant,
    val generatedAt: Instant?,
    val truncated: Boolean,
    val moreOnPaidPlans: Int,
    val attributionName: String,
    val attributionUrl: String,
    val attributionTerms: String,
    val freeDeveloperUsage: FreeDeveloperUsage?,
    val isStale: Boolean
)

internal object EconomicCalendarParser {
    fun parse(
        root: JSONObject,
        now: Instant = Instant.now(),
        staleAfterSeconds: Long = 36 * 60 * 60L
    ): EconomicCalendarSnapshot {
        val eventsJson = root.optJSONArray("events")
            ?: throw IllegalArgumentException("Calendar snapshot is missing its events array")
        require(eventsJson.length() <= MAX_EVENTS) { "Calendar snapshot contains too many events" }

        val events = LinkedHashMap<String, EconomicEvent>()
        for (index in 0 until eventsJson.length()) {
            val item = eventsJson.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            val seriesCode = item.optString("series_code").trim()
            val dateText = item.optString("date").trim()
            if (id.isBlank() || name.isBlank() || seriesCode.isBlank()) continue

            val date = try {
                LocalDate.parse(dateText)
            } catch (_: DateTimeParseException) {
                continue
            }

            events[id] = EconomicEvent(
                id = id.take(160),
                seriesCode = seriesCode.take(100),
                name = name.take(240),
                country = optionalString(item, "country")?.take(12),
                category = item.optString("category").take(100),
                impact = optionalString(item, "impact")?.uppercase()?.take(20) ?: "UNKNOWN",
                date = date,
                releaseAtUtc = optionalString(item, "release_at_utc")?.let(::parseInstant),
                timePrecision = optionalString(item, "time_precision")?.take(20),
                allDay = item.optBoolean("all_day", false),
                timeWindowStart = optionalString(item, "time_window_start")?.take(100),
                timeWindowEnd = optionalString(item, "time_window_end")?.take(100),
                dateWindowStart = optionalString(item, "date_window_start")?.take(20),
                dateWindowEnd = optionalString(item, "date_window_end")?.take(20),
                status = optionalString(item, "status")?.take(60),
                eventUrl = safePublisherUrl(optionalString(item, "url")),
                sourceName = optionalString(item, "source_name")?.take(160),
                sourceUrl = safePublisherUrl(optionalString(item, "source_url")),
                sourceLicenseJson = item.optJSONObject("source_license")?.toString()
                    ?: optionalString(item, "source_license"),
                sourceRightsJson = item.optJSONObject("source_rights")?.toString()
                    ?: optionalString(item, "source_rights"),
                figuresAvailable = optionalBoolean(item, "figures_available"),
                resultAvailable = optionalBoolean(item, "result_available"),
                lastVerifiedAt = optionalString(item, "last_verified_at")?.let(::parseInstant),
                dataStale = optionalBoolean(item, "data_stale")
            )
        }

        val attribution = root.optJSONObject("attribution")
            ?: throw IllegalArgumentException("Calendar snapshot is missing provider attribution")
        val attributionName = attribution.optString("source").trim()
        val attributionTerms = attribution.optString("terms").trim()
        val attributionUrl = safeFinanceCalendarUrl(optionalString(attribution, "url"))
            ?: throw IllegalArgumentException("Calendar attribution URL is invalid")
        require(attributionName.isNotBlank()) { "Calendar attribution source is missing" }
        require(attributionTerms.isNotBlank()) { "Calendar terms notice is missing" }

        val fetchedAt = optionalString(root, "fetched_at")?.let(::parseInstant)
            ?: throw IllegalArgumentException("Calendar snapshot fetch timestamp is missing")
        val generatedAt = optionalString(root, "generated_at")?.let(::parseInstant)
        val ageSeconds = Duration.between(fetchedAt, now).seconds
        val usage = root.optJSONObject("free_developer")?.let(::parseUsage)

        return EconomicCalendarSnapshot(
            events = events.values.sortedWith(
                compareBy<EconomicEvent> { it.date }
                    .thenBy { it.releaseAtUtc ?: Instant.MAX }
                    .thenBy { it.name }
            ),
            fetchedAt = fetchedAt,
            generatedAt = generatedAt,
            truncated = root.optBoolean("truncated", false),
            moreOnPaidPlans = root.optInt("more_on_paid_plans", 0).coerceAtLeast(0),
            attributionName = attributionName,
            attributionUrl = attributionUrl,
            attributionTerms = attributionTerms,
            freeDeveloperUsage = usage,
            isStale = ageSeconds < 0 || ageSeconds > staleAfterSeconds
        )
    }

    private fun parseUsage(json: JSONObject): FreeDeveloperUsage {
        val window = json.optJSONObject("window")
            ?: throw IllegalArgumentException("Free Developer quota window is missing")
        val plan = json.optString("plan").trim()
        require(plan.isNotBlank()) { "Free Developer plan name is missing" }
        return FreeDeveloperUsage(
            plan = plan.take(80),
            accessScope = json.optString("access_scope").take(120),
            quotaBasis = json.optString("quota_basis").take(120),
            dailyLimit = json.optInt("daily_limit", 0).coerceAtLeast(0),
            monthlyLimit = json.optInt("monthly_limit", 0).coerceAtLeast(0),
            usedToday = json.optInt("used_today", 0).coerceAtLeast(0),
            remainingToday = json.optInt("remaining_today", 0).coerceAtLeast(0),
            usedThisMonth = json.optInt("used_this_month", 0).coerceAtLeast(0),
            remainingThisMonth = json.optInt("remaining_this_month", 0).coerceAtLeast(0),
            dailyResetsAt = optionalString(json, "resets_at")?.let(::parseInstant),
            monthlyResetsAt = optionalString(json, "monthly_resets_at")?.let(::parseInstant),
            maxEventsPerResponse = json.optInt("max_events_per_response", 20).coerceAtLeast(0),
            windowFrom = optionalString(window, "from")?.let(::parseDate),
            windowTo = optionalString(window, "to")?.let(::parseDate),
            windowTimezone = optionalString(window, "timezone")?.take(40),
            upgradeUrl = safeFinanceCalendarUrl(optionalString(json, "upgrade_url")),
            resumeAtUtc = optionalString(json, "resume_at_utc")?.let(::parseInstant)
        )
    }

    private fun optionalString(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun optionalBoolean(json: JSONObject, key: String): Boolean? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optBoolean(key)
    }

    private fun parseInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    private fun safePublisherUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val uri = java.net.URI(value)
            if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null
            ) null else uri.toASCIIString()
        }.getOrNull()
    }

    private fun safeFinanceCalendarUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val uri = java.net.URI(value)
            if (uri.scheme.equals("https", true) &&
                uri.host?.lowercase() in setOf("financecalendar.com", "www.financecalendar.com") &&
                uri.rawUserInfo == null
            ) uri.toASCIIString() else null
        }.getOrNull()
    }

    private const val MAX_EVENTS = 200
}
