package com.tradingnews.app

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Security
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

private const val OFFICIAL_CALENDAR_URL = "https://www.forexfactory.com/calendar"
private const val OFFICIAL_NOTICES_URL = "https://www.forexfactory.com/notices"

private val CalendarBackground = Color(0xFF0B1017)
private val CalendarPanel = Color(0xFF121B25)
private val CalendarAccent = Color(0xFF35D6B0)
private val CalendarMuted = Color(0xFF91A4B3)

/**
 * Forex Factory calendar entry point.
 *
 * This screen deliberately opens the provider-hosted calendar instead of scraping,
 * copying, caching, or redistributing FEED calendar data inside the app.
 */
@Composable
fun EconomicCalendarScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CalendarBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    "Official Forex Factory schedule",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Open the live calendar directly from its original publisher.",
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
                            "FOREX FACTORY",
                            color = CalendarAccent,
                            fontSize = 11.sp,
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
                        "View event times, currencies, impact levels, previous values, forecasts and actual releases on the official Forex Factory website.",
                        color = Color(0xFFB4C2CB),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { openOfficialForexFactoryPage(context, OFFICIAL_CALENDAR_URL) },
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
                        "The schedule is hosted by Forex Factory and is not copied into this app.",
                        color = CalendarMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            InfoCard(
                title = "Why does the calendar open externally?",
                body = "Forex Factory's notices restrict copying, republication and redistribution of its calendar schedules and FEED database without prior written consent. Trading News therefore opens the provider's official page instead of scraping or republishing its data."
            )
        }

        item {
            InfoCard(
                title = "Native calendar and event alerts",
                body = "In-app economic-event rows, forecasts, actual values and event reminders are not enabled in this build. They can only be added after written data-display and redistribution permission has been obtained from the provider, or after integrating another calendar source whose licence explicitly allows public app display."
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarPanel),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = CalendarAccent)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Data-use notice",
                            color = Color(0xFFE8F0F4),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Read the source provider's current notices before using or redistributing its data.",
                        color = CalendarMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    TextButton(
                        onClick = { openOfficialForexFactoryPage(context, OFFICIAL_NOTICES_URL) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                    ) {
                        Text("Read Forex Factory notices", color = CalendarAccent)
                        Spacer(Modifier.size(5.dp))
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = CalendarAccent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = CalendarMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(7.dp))
                Text(
                    "Trading News does not generate sample economic events. If a licensed in-app calendar is not configured, the app shows this source link rather than fabricated data.",
                    color = CalendarMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CalendarPanel),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color(0xFFE8F0F4), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(body, color = CalendarMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

private fun openOfficialForexFactoryPage(context: android.content.Context, url: String) {
    val uri = Uri.parse(url)
    if (uri.scheme != "https" || uri.host !in setOf("www.forexfactory.com", "forexfactory.com")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No browser is installed; keep the app stable and do not use an unsafe fallback.
    }
}
