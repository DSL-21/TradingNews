package com.tradingnews.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class EconomicCalendarParserTest {
    @Test
    fun parsesScheduleWithoutInventingTimesOrFigures() {
        val payload = snapshot(
            event(
                id = "us-cpi",
                name = "US CPI",
                date = "2026-11-14",
                impact = "high",
                releaseAtUtc = null,
                timePrecision = "date"
            )
        )

        val parsed = EconomicCalendarParser.parse(
            payload,
            now = Instant.parse("2026-11-14T01:00:00Z")
        )
        val result = parsed.events.single()

        assertEquals("us-cpi", result.id)
        assertEquals("HIGH", result.impact)
        assertEquals(LocalDate.parse("2026-11-14"), result.date)
        assertNull(result.releaseAtUtc)
        assertEquals("date", result.timePrecision)
        assertNull(result.figuresAvailable)
        assertNull(result.resultAvailable)
        assertEquals("Free Developer", parsed.freeDeveloperUsage?.plan)
        assertEquals(10, parsed.freeDeveloperUsage?.dailyLimit)
        assertEquals(90, parsed.freeDeveloperUsage?.remainingThisMonth)
        assertEquals("UTC", parsed.freeDeveloperUsage?.windowTimezone)
    }

    @Test
    fun keepsOriginalSourceLinksAndLicenceObjects() {
        val record = event(
            id = "fomc",
            name = "FOMC rate decision",
            date = "2026-11-15",
            impact = "high",
            releaseAtUtc = "2026-11-15T19:00:00Z",
            timePrecision = "minute"
        ).put("source_name", "Federal Reserve")
            .put("source_url", "https://www.federalreserve.gov/")
            .put("url", "https://www.financecalendar.com/event/fomc/")
            .put("source_license", JSONObject().put("attribution", "Federal Reserve").put("notice", "Keep source credit"))

        val parsed = EconomicCalendarParser.parse(
            snapshot(record),
            now = Instant.parse("2026-11-14T01:00:00Z")
        )
        val result = parsed.events.single()

        assertEquals("Federal Reserve", result.sourceName)
        assertEquals("https://www.federalreserve.gov/", result.sourceUrl)
        assertTrue(result.sourceLicenseJson!!.contains("Keep source credit"))
    }

    @Test
    fun sortsEventsAndReportsTruncatedCoverage() {
        val late = event("late", "Later event", "2026-11-16", "low", null, "date")
        val early = event("early", "Earlier event", "2026-11-14", "medium", null, "date")
        val parsed = EconomicCalendarParser.parse(
            snapshot(late, early).put("truncated", true).put("more_on_paid_plans", 4),
            now = Instant.parse("2026-11-14T01:00:00Z")
        )

        assertEquals(listOf("early", "late"), parsed.events.map { it.id })
        assertTrue(parsed.truncated)
        assertEquals(4, parsed.moreOnPaidPlans)
    }

    @Test
    fun marksOldSnapshotsAsStale() {
        val oldSnapshot = snapshot(event("old", "Old schedule", "2026-11-10", "low", null, "date"))
            .put("fetched_at", "2026-11-10T00:00:00Z")
        val parsed = EconomicCalendarParser.parse(
            oldSnapshot,
            now = Instant.parse("2026-11-14T12:00:00Z")
        )
        assertTrue(parsed.isStale)
    }

    @Test
    fun rejectsMissingAttributionTermsOrInvalidAttributionHost() {
        val missingTerms = snapshot(event("a", "Event", "2026-11-14", "low", null, "date"))
            .put("attribution", JSONObject().put("source", "Finance Calendar").put("url", "https://www.financecalendar.com/"))
        val unsafeUrl = snapshot(event("a", "Event", "2026-11-14", "low", null, "date"))
            .put("attribution", JSONObject().put("source", "Finance Calendar").put("terms", "terms").put("url", "https://evil.example/"))

        assertFalse(runCatching { EconomicCalendarParser.parse(missingTerms) }.isSuccess)
        assertFalse(runCatching { EconomicCalendarParser.parse(unsafeUrl) }.isSuccess)
    }

    @Test
    fun rejectsUnsafePublisherLinksButKeepsTheCalendarRecord() {
        val event = event("a", "Event", "2026-11-14", "low", null, "date")
            .put("url", "javascript:alert(1)")
            .put("source_url", "http://example.com/")
        val parsed = EconomicCalendarParser.parse(snapshot(event))

        assertEquals(1, parsed.events.size)
        assertNull(parsed.events.single().eventUrl)
        assertNull(parsed.events.single().sourceUrl)
    }

    private fun snapshot(vararg events: JSONObject): JSONObject =
        JSONObject()
            .put("fetched_at", "2026-11-14T00:00:00Z")
            .put("generated_at", "2026-11-14T00:00:00Z")
            .put("truncated", false)
            .put("events", JSONArray().apply { events.forEach(::put) })
            .put(
                "attribution",
                JSONObject()
                    .put("source", "financecalendar.com")
                    .put("terms", "Use is governed by https://www.financecalendar.com/api/terms/")
                    .put("url", "https://www.financecalendar.com/")
            )
            .put("free_developer", usage())


    private fun usage(): JSONObject =
        JSONObject()
            .put("plan", "Free Developer")
            .put("access_scope", "schedule_json")
            .put("quota_basis", "successful responses")
            .put("daily_limit", 10)
            .put("monthly_limit", 100)
            .put("used_today", 1)
            .put("remaining_today", 9)
            .put("used_this_month", 10)
            .put("remaining_this_month", 90)
            .put("resets_at", "2026-11-15T00:00:00Z")
            .put("monthly_resets_at", "2026-12-01T00:00:00Z")
            .put("max_events_per_response", 20)
            .put(
                "window",
                JSONObject()
                    .put("from", "2026-11-14")
                    .put("to", "2026-11-20")
                    .put("timezone", "UTC")
            )
            .put("upgrade_url", "https://www.financecalendar.com/api/")
            .put("resume_at_utc", "2026-11-15T00:00:00Z")

    private fun event(
        id: String,
        name: String,
        date: String,
        impact: String,
        releaseAtUtc: String?,
        timePrecision: String?
    ): JSONObject = JSONObject()
        .put("id", id)
        .put("series_code", id)
        .put("name", name)
        .put("country", "US")
        .put("category", "economic-indicators")
        .put("impact", impact)
        .put("date", date)
        .put("release_at_utc", releaseAtUtc)
        .put("time_precision", timePrecision)
        .put("all_day", false)
}
