package com.tradingnews.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FOREX_FACTORY_CALENDAR = "https://www.forexfactory.com/calendar"
private const val FOREX_FACTORY_JSON = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"
private const val FOREX_FACTORY_ICS = "https://nfs.faireconomy.media/ff_calendar_thisweek.ics"
private const val FOREX_FACTORY_CSV = "https://nfs.faireconomy.media/ff_calendar_thisweek.csv"

private val CalendarBackground = Color(0xFF0B1017)
private val CalendarPanel = Color(0xFF121B25)
private val CalendarAccent = Color(0xFF35D6B0)
private val CalendarMuted = Color(0xFF91A4B3)

/**
 * Official Forex Factory calendar launcher.
 *
 * Trading News opens the provider's own calendar and weekly export links.
 * It does not scrape, cache, parse, copy, or republish FEED records.
 */
@Composable
fun EconomicCalendarScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CalendarBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "ECONOMIC CALENDAR",
                    color = CalendarMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    "Forex Factory",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "The original weekly schedule, impact ratings and release values.",
                    color = CalendarMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142B2B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = CalendarAccent,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "OFFICIAL FOREX FACTORY",
                            color = CalendarAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        "Live economic calendar",
                        color = Color(0xFFE8F0F4),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Open Forex Factory's own calendar with its currency and impact filters, event details, actual, forecast and previous values, plus its device-time-zone settings.",
                        color = Color(0xFFB4C2CB),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { openOfficialUrl(context, FOREX_FACTORY_CALENDAR) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalendarAccent,
                            contentColor = Color(0xFF06231D)
                        )
                    ) {
                        Text("Open official calendar", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                    Text(
                        "The calendar remains on Forex Factory so its original data, filters and updates are shown directly by the publisher.",
                        color = CalendarMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarPanel),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "OFFICIAL WEEKLY EXPORTS",
                        color = Color(0xFFE8F0F4),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Forex Factory also publishes this week's schedule as ICS, CSV and JSON. These links open the files directly from the original host; Trading News does not import or republish their contents.",
                        color = CalendarMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    ExportLink("JSON", "Structured weekly schedule", FOREX_FACTORY_JSON, context)
                    ExportLink("ICS", "Add the weekly schedule to a calendar app", FOREX_FACTORY_ICS, context)
                    ExportLink("CSV", "Open the weekly schedule as a table", FOREX_FACTORY_CSV, context)
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = CalendarMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    "Forex Factory's notices restrict copying and redistributing its calendar database without prior written consent. This app therefore links to the original calendar and official exports instead of creating an unofficial copy.",
                    color = CalendarMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ExportLink(label: String, description: String, url: String, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = CalendarAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = CalendarMuted, fontSize = 10.sp)
        }
        TextButton(
            onClick = { openOfficialUrl(context, url) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Outlined.Download, contentDescription = null, tint = CalendarAccent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.size(4.dp))
            Text("Open", color = CalendarAccent, fontSize = 10.sp)
        }
    }
}

private fun openOfficialUrl(context: Context, value: String) {
    val safe = runCatching {
        val uri = Uri.parse(value)
        uri.scheme == "https" &&
            uri.host?.lowercase() in setOf("www.forexfactory.com", "forexfactory.com", "nfs.faireconomy.media") &&
            uri.userInfo == null
    }.getOrDefault(false)
    if (!safe) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
    } catch (_: ActivityNotFoundException) {
        // No browser is installed; keep the app stable without an unsafe fallback.
    }
}
