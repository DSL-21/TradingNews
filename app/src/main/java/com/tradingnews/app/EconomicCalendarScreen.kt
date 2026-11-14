package com.tradingnews.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradingnews.app.data.EconomicCalendarRepository
import com.tradingnews.app.data.EconomicCalendarSnapshot
import com.tradingnews.app.data.EconomicEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CalendarBackground = Color(0xFF0B1017)
private val CalendarPanel = Color(0xFF121B25)
private val CalendarAccent = Color(0xFF35D6B0)
private val CalendarMuted = Color(0xFF91A4B3)
private val CalendarWarning = Color(0xFFFFB454)
private val CalendarDanger = Color(0xFFFF647C)
private val CalendarBlue = Color(0xFF79AFFF)
private const val FOREX_FACTORY_URL = "https://www.forexfactory.com/calendar"

private enum class CalendarFilter(val label: String, val impact: String?) {
    ALL("All", null),
    HIGH("High impact", "HIGH"),
    MEDIUM("Medium", "MEDIUM"),
    LOW("Low", "LOW")
}

/** Native calendar backed by the daily, attributed public schedule snapshot. */
@Composable
fun EconomicCalendarScreen() {
    val context = LocalContext.current
    val repository = remember { EconomicCalendarRepository(context) }
    var snapshot by remember { mutableStateOf<EconomicCalendarSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshToken by rememberSaveable { mutableStateOf(0) }
    var selectedFilter by rememberSaveable { mutableStateOf(CalendarFilter.ALL) }

    LaunchedEffect(refreshToken) {
        isLoading = true
        errorMessage = null
        try {
            snapshot = repository.load(forceRefresh = refreshToken > 0)
        } catch (error: Exception) {
            errorMessage = error.message?.take(200) ?: "Calendar data is temporarily unavailable."
        } finally {
            isLoading = false
        }
    }

    val allEvents = snapshot?.events.orEmpty()
    val visibleEvents = if (selectedFilter == CalendarFilter.ALL) {
        allEvents
    } else {
        allEvents.filter { it.impact == selectedFilter.impact }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CalendarBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text("ECONOMIC CALENDAR", color = CalendarMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Upcoming market events", color = MaterialTheme.colorScheme.onBackground, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Schedule: today through the next six UTC days", color = CalendarMuted, fontSize = 11.sp)
                }
                TextButton(onClick = { refreshToken++ }, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = CalendarAccent)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh calendar", tint = CalendarAccent)
                    }
                }
            }
        }

        snapshot?.let { current ->
            item { CalendarStatusCard(current) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CalendarFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
            if (current.truncated || current.moreOnPaidPlans > 0) {
                item {
                    InfoCard(
                        title = "Free Developer coverage",
                        body = if (current.moreOnPaidPlans > 0) {
                            "The provider reports ${current.moreOnPaidPlans} additional matching releases on paid plans. The free feed is limited to 20 records per response and does not include forecast, previous or actual figures."
                        } else {
                            "The provider marked this schedule response as truncated. Some matching events may not be included."
                        },
                        accent = CalendarWarning
                    )
                }
            }
            if (visibleEvents.isEmpty() && !isLoading) {
                item {
                    EmptyCard(
                        title = if (allEvents.isEmpty()) "No calendar events in the current snapshot" else "No events for this impact filter",
                        body = if (allEvents.isEmpty()) {
                            "The calendar will populate after the repository's one-time API key setup and the first successful scheduled refresh. No example events are inserted."
                        } else {
                            "Choose another impact filter to view the available events."
                        }
                    )
                }
            } else {
                items(visibleEvents, key = { it.id }) { event -> EconomicEventCard(event) }
            }
            item {
                AttributionCard(
                    name = current.attributionName,
                    docsUrl = current.attributionDocsUrl,
                    terms = current.attributionTerms
                )
            }
        }

        if (isLoading && snapshot == null) {
            item { LoadingCard() }
        }

        errorMessage?.let { message ->
            item {
                EmptyCard(
                    title = if (snapshot == null) "Calendar feed is not available yet" else "Showing cached calendar data",
                    body = message
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = CalendarMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(7.dp))
                Text(
                    "Free Developer schedules omit forecast, prior and actual figures. Event times are shown only when the provider supplies a confirmed instant; missing times are never treated as midnight.",
                    color = CalendarMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        item {
            TextButton(
                onClick = { openHttpsUrl(context, FOREX_FACTORY_URL) },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
            ) {
                Text("Open Forex Factory calendar", color = CalendarAccent)
                Spacer(Modifier.size(5.dp))
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = CalendarAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CalendarStatusCard(snapshot: EconomicCalendarSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (snapshot.isStale) Color(0xFF302719) else Color(0xFF142B2B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (snapshot.isStale) "CACHED · STALE" else "SCHEDULE SNAPSHOT",
                color = if (snapshot.isStale) CalendarWarning else CalendarAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text("${snapshot.events.size} scheduled events", color = Color(0xFFE8F0F4), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Snapshot retrieved ${formatInstant(snapshot.fetchedAt)}", color = CalendarMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EconomicEventCard(event: EconomicEvent) {
    val context = LocalContext.current
    val impactColor = when (event.impact) {
        "HIGH" -> CalendarDanger
        "MEDIUM" -> CalendarWarning
        "LOW" -> CalendarBlue
        else -> CalendarMuted
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = CalendarPanel),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${event.country ?: "GLOBAL"} · ${event.category.replace('-', ' ').uppercase(Locale.ROOT)}",
                    color = CalendarMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(event.impact, color = impactColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(event.name, color = Color(0xFFE8F0F4), fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(formatEventDateTime(event), color = CalendarAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            event.status?.takeIf { it.isNotBlank() }?.let {
                Text("Status: ${it.replace('-', ' ')}", color = CalendarMuted, fontSize = 9.sp)
            }
            if (event.figuresAvailable == true || event.resultAvailable == true) {
                Text(
                    "The provider indicates figures or a result may be available on a paid plan; they are not included in this schedule-only response.",
                    color = CalendarMuted,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
            val credit = sourceCredit(event.sourceLicenseJson, event.sourceRightsJson)
            if (!event.sourceName.isNullOrBlank() || !credit.isNullOrBlank()) {
                Text(listOfNotNull(event.sourceName, credit).joinToString(" · ").take(260), color = CalendarMuted, fontSize = 9.sp, lineHeight = 13.sp)
            }
            val link = event.sourceUrl ?: event.eventUrl
            if (link != null) {
                TextButton(onClick = { openHttpsUrl(context, link) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                    Text(if (event.sourceUrl != null) "Open original source" else "Open event details", color = CalendarAccent, fontSize = 10.sp)
                    Spacer(Modifier.size(4.dp))
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = CalendarAccent, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun AttributionCard(name: String, docsUrl: String, terms: String) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = CalendarPanel),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("DATA BY $name", color = CalendarAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                "Finance Calendar schedule data is cached from the provider API. Original publisher credits, links and record-level rights metadata are retained.",
                color = CalendarMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Text(terms.take(500), color = CalendarMuted, fontSize = 9.sp, lineHeight = 13.sp)
            TextButton(onClick = { openHttpsUrl(context, "https://www.financecalendar.com/") }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text("FinanceCalendar.com", color = CalendarAccent)
                Spacer(Modifier.size(5.dp))
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = CalendarAccent, modifier = Modifier.size(13.dp))
            }
            TextButton(onClick = { openHttpsUrl(context, docsUrl) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text("API documentation", color = CalendarAccent)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color = CalendarAccent) {
    Card(colors = CardDefaults.cardColors(containerColor = CalendarPanel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(body, color = CalendarMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CalendarPanel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color(0xFFE8F0F4), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(body, color = CalendarMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(colors = CardDefaults.cardColors(containerColor = CalendarPanel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = CalendarAccent)
            Spacer(Modifier.size(9.dp))
            Text("Loading economic calendar...", color = CalendarMuted, fontSize = 11.sp)
        }
    }
}

private fun formatEventDateTime(event: EconomicEvent): String {
    val zone = ZoneId.systemDefault()
    val time = when {
        event.releaseAtUtc != null && event.timePrecision in setOf("second", "minute") ->
            DateTimeFormatter.ofPattern("HH:mm z", Locale.US).withZone(zone).format(event.releaseAtUtc)
        event.timePrecision == "range" ->
            "Time window: ${event.timeWindowStart ?: "TBA"} – ${event.timeWindowEnd ?: "TBA"}"
        event.timePrecision in setOf("month", "quarter", "year") ->
            "Estimated window: ${event.dateWindowStart ?: event.date} – ${event.dateWindowEnd ?: event.date}"
        event.allDay -> "All day · time not specified"
        else -> "Time not confirmed"
    }
    return "${event.date} · $time"
}

private fun formatInstant(value: Instant): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm z", Locale.US).withZone(ZoneId.systemDefault()).format(value)

private fun sourceCredit(sourceLicenseJson: String?, sourceRightsJson: String?): String? {
    val raw = sourceLicenseJson ?: sourceRightsJson ?: return null
    return runCatching {
        val json = JSONObject(raw)
        listOf("attribution", "notice").mapNotNull { key ->
            json.optString(key).takeIf { it.isNotBlank() && it != "null" }
        }.joinToString(" ")
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun openHttpsUrl(context: Context, value: String) {
    val safe = runCatching {
        val uri = Uri.parse(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
    if (!safe) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
    } catch (_: ActivityNotFoundException) {
        // No browser is installed; keep the app stable without an unsafe fallback.
    }
}
