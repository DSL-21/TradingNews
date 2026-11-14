package com.tradingnews.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.tradingnews.app.data.FeedStatus
import com.tradingnews.app.data.NewsCategories
import com.tradingnews.app.data.NewsItem
import com.tradingnews.app.data.NewsWorkScheduler
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AppBackground = Color(0xFF0B1017)
private val Panel = Color(0xFF121B25)
private val PanelRaised = Color(0xFF192634)
private val Border = Color(0xFF263544)
private val TextMain = Color(0xFFE8F0F4)
private val TextMuted = Color(0xFF91A4B3)
private val Accent = Color(0xFF35D6B0)
private val Warning = Color(0xFFFFB454)
private val Blue = Color(0xFF79AFFF)
private val Error = Color(0xFFFF647C)
private const val FOREX_FACTORY_CALENDAR = "https://www.forexfactory.com/calendar"
private const val GDELT_ABOUT = "https://www.gdeltproject.org/about.html"

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TradingNewsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        NewsWorkScheduler.ensureScheduled(this)
        setContent { TradingNewsTheme { TradingNewsApp(viewModel) } }
    }
}

private enum class AppTab(val title: String) {
    HOME("Overview"), CALENDAR("Calendar"), NEWS("Headlines"), SETTINGS("Settings")
}

@Composable
private fun TradingNewsTheme(content: @Composable () -> Unit) {
    val scheme = androidx.compose.material3.darkColorScheme(
        primary = Accent,
        onPrimary = Color(0xFF06231D),
        secondary = Blue,
        background = AppBackground,
        onBackground = TextMain,
        surface = Panel,
        onSurface = TextMain,
        surfaceVariant = PanelRaised,
        onSurfaceVariant = TextMuted,
        outline = Border,
        error = Error
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun TradingNewsApp(viewModel: TradingNewsViewModel) {
    val state = viewModel.uiState
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101821), contentColor = TextMuted) {
                AppTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        AppTab.HOME -> Icons.Outlined.ShowChart
                        AppTab.CALENDAR -> Icons.Outlined.CalendarMonth
                        AppTab.NEWS -> Icons.Outlined.Newspaper
                        AppTab.SETTINGS -> Icons.Outlined.Settings
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
        ) {
            AppHeader(state.status, state.isRefreshing) { viewModel.refresh(forceRefresh = true) }
            if (state.status == FeedStatus.CACHED || state.status == FeedStatus.OFFLINE) {
                StatusBanner(state.notice, state.status == FeedStatus.OFFLINE)
            }
            when (selectedTab) {
                AppTab.HOME -> DashboardScreen(
                    state = state,
                    onCalendar = { selectedTab = AppTab.CALENDAR },
                    onNews = { selectedTab = AppTab.NEWS }
                )
                AppTab.CALENDAR -> CalendarScreen()
                AppTab.NEWS -> NewsScreen(state)
                AppTab.SETTINGS -> SettingsScreen(state)
            }
        }
    }
}

@Composable
private fun AppHeader(status: FeedStatus, refreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).background(Accent.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ShowChart, contentDescription = null, tint = Accent, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("TRADING NEWS", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = TextMain)
                Text("MARKET INTELLIGENCE", fontSize = 9.sp, letterSpacing = 1.3.sp, color = TextMuted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(status)
            TextButton(onClick = onRefresh, contentPadding = PaddingValues(4.dp), enabled = !refreshing) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh headlines", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: FeedStatus) {
    val label = when (status) {
        FeedStatus.CONNECTING -> "CONNECTING"
        FeedStatus.LIVE -> "LIVE"
        FeedStatus.CACHED -> "CACHED"
        FeedStatus.OFFLINE -> "OFFLINE"
    }
    val color = when (status) {
        FeedStatus.LIVE -> Accent
        FeedStatus.CONNECTING -> Blue
        FeedStatus.CACHED -> Warning
        FeedStatus.OFFLINE -> Error
    }
    Surface(color = color.copy(alpha = 0.13f), shape = RoundedCornerShape(6.dp)) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean) {
    val color = if (isError) Error else Warning
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(color.copy(alpha = 0.10f), RoundedCornerShape(9.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, RoundedCornerShape(50)))
        Text(message, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun DashboardScreen(state: TradingNewsUiState, onCalendar: () -> Unit, onNews: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MARKET OVERVIEW", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                Text("Stay ahead of market-moving headlines.", fontSize = 25.sp, color = TextMain, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                Text("Global news discovery • Original publisher links", fontSize = 11.sp, color = TextMuted)
            }
        }
        item { CalendarLinkCard(compact = true, onClick = onCalendar) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("LATEST HEADLINES", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Text("Global market and geopolitical coverage", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onNews) { Text("View all", color = Accent, fontSize = 12.sp) }
            }
        }
        when {
            state.isLoading && state.articles.isEmpty() -> item { LoadingPanel() }
            state.articles.isEmpty() -> item { EmptyState("No headlines available", state.notice) }
            else -> items(state.articles.take(4), key = { it.id }) { article -> NewsCard(article) }
        }
        item { DisclaimerLine() }
    }
}

@Composable
private fun CalendarScreen() {
    EconomicCalendarScreen()
}

@Composable
private fun NewsScreen(state: TradingNewsUiState) {
    var selectedCategory by rememberSaveable { mutableStateOf(NewsCategories.ALL) }
    val filters = listOf(
        NewsCategories.ALL to "All",
        NewsCategories.MARKETS to "Markets",
        NewsCategories.GEOPOLITICS to "Geopolitics",
        NewsCategories.ENERGY to "Energy",
        NewsCategories.CRYPTO to "Crypto",
        NewsCategories.COMPANIES to "Companies",
        NewsCategories.OTHER to "Other"
    )
    val filtered = state.articles.filter { selectedCategory == NewsCategories.ALL || it.category == selectedCategory }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("BREAKING NEWS", "Headlines across markets and global events")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
        when {
            state.isLoading && state.articles.isEmpty() -> LoadingPanel()
            filtered.isEmpty() -> EmptyState("No headlines in this category", if (state.articles.isEmpty()) state.notice else "Try another category or refresh the feed.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) { items(filtered, key = { it.id }) { NewsCard(it, expanded = true) } }
        }
    }
}

@Composable
private fun SettingsScreen(state: TradingNewsUiState) {
    val context = LocalContext.current
    var autoRefresh by rememberSaveable {
        mutableStateOf(NewsWorkScheduler.isAutoRefreshEnabled(context))
    }
    var alertsEnabled by rememberSaveable {
        mutableStateOf(NewsWorkScheduler.isAlertsEnabled(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        alertsEnabled = granted
        NewsWorkScheduler.setAlertsEnabled(context, granted)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 17.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { SectionTitle("SETTINGS", "Refresh, alerts, data sources and app information") }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Automatic news refresh", color = TextMain, fontWeight = FontWeight.SemiBold)
                            Text("Background sync every 15 minutes when Android allows it. Battery saver and Doze may delay execution.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Switch(
                            checked = autoRefresh,
                            onCheckedChange = { enabled ->
                                autoRefresh = enabled
                                NewsWorkScheduler.setAutoRefreshEnabled(context, enabled)
                                if (!enabled) alertsEnabled = false
                            }
                        )
                    }
                    HorizontalDivider(color = Border)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Background headline alerts", color = TextMain, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (autoRefresh) {
                                    "Local Android notifications for keyword-matched headlines. This is not remote Firebase push and may be delayed by Android battery controls."
                                } else {
                                    "Enable automatic news refresh to receive background headline alerts."
                                },
                                color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = alertsEnabled,
                            enabled = autoRefresh,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    alertsEnabled = false
                                    NewsWorkScheduler.setAlertsEnabled(context, false)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    alertsEnabled = true
                                    NewsWorkScheduler.setAlertsEnabled(context, true)
                                }
                            }
                        )
                    }
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("News source", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("GDELT DOC 2.0", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("The feed shows genuine headlines, publisher hosts, and the time GDELT first saw an article. Instrument matches are keyword-based relevance hints, not validated impact or trading signals.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    TextButton(onClick = { openExternalUrl(context, GDELT_ABOUT) }, modifier = Modifier.align(Alignment.End)) {
                        Text("GDELT terms and attribution", color = Accent)
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Accent, modifier = Modifier.padding(start = 5.dp).size(15.dp))
                    }
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Economic calendar and market data", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (BuildConfig.BACKEND_BASE_URL.isBlank()) {
                            "Native in-app calendar data is not connected in this release. The Calendar tab opens the official Forex Factory website. A native feed needs written redistribution permission or another licensed provider and a maintained HTTPS backend."
                        } else {
                            "A custom HTTPS backend URL is configured, but this release does not currently display native calendar events or measured price reactions. Verify the backend, provider licence, and data-display rights before enabling an integration."
                        },
                        color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp
                    )
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy and storage", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("The app stores a compact local GDELT headline cache and refresh preferences. Provider API credentials belong on the backend and must never be embedded in the APK.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About Trading News", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("Version 0.3.0 • Android", color = TextMuted, fontSize = 11.sp)
                    Text("Informational use only. Relevance scores are deterministic keyword matches. Observed event returns describe price movement after a timestamp; they do not prove causality or predict future prices.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    Text("Feed status: ${statusText(state.status)}", color = TextMuted, fontSize = 10.sp)
                    state.lastUpdated?.let { Text("Last retrieved: ${formatDateTime(it)}", color = TextMuted, fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun CalendarLinkCard(compact: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF142B2B)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(Modifier.size(38.dp).background(Accent.copy(alpha = 0.12f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Accent, modifier = Modifier.size(21.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("FOREX FACTORY", color = Accent, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("Open the official economic calendar", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 13.sp else 14.sp)
                    if (!compact) Text("Live schedule • source-hosted data", color = TextMuted, fontSize = 10.sp)
                }
            }
            Icon(Icons.Outlined.OpenInNew, contentDescription = "Open official Forex Factory calendar", tint = Accent)
        }
    }
}

@Composable
private fun NewsCard(article: NewsItem, expanded: Boolean = false) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { openExternalUrl(context, article.url) }
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CategoryBadge(article.category)
                Text("GDELT SEEN", color = TextMuted, fontSize = 8.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)
            }
            Text(article.title, color = TextMain, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            if (article.instrumentMatches.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    article.instrumentMatches.take(5).forEach { match ->
                        Surface(color = Accent.copy(alpha = 0.10f), shape = RoundedCornerShape(5.dp)) {
                            Text(
                                "${match.symbol} · ${match.band}",
                                color = Accent,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${article.source} • ${relativeTime(article.seenAt)}", color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Open publisher article", tint = Accent, modifier = Modifier.size(18.dp))
            }
            if (expanded) Text("Open original publisher", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    val label = category.uppercase(Locale.ROOT)
    val color = when (category) {
        NewsCategories.GEOPOLITICS -> Error
        NewsCategories.ENERGY -> Warning
        NewsCategories.CRYPTO -> Blue
        NewsCategories.COMPANIES -> Color(0xFFB69CFF)
        else -> Accent
    }
    Surface(color = color.copy(alpha = 0.13f), shape = RoundedCornerShape(5.dp)) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Text(subtitle, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) { content() }
    }
}

@Composable
private fun LoadingPanel() {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Loading live headlines...", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = TextMuted, modifier = Modifier.size(30.dp))
        Text(title, color = TextMain, fontWeight = FontWeight.SemiBold)
        Text(description, color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun DisclaimerLine() {
    Text(
        "News headlines are informational only. Keyword categories do not indicate price direction or expected market impact.",
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        modifier = Modifier.padding(top = 5.dp, bottom = 6.dp)
    )
}

private fun statusText(status: FeedStatus): String = when (status) {
    FeedStatus.CONNECTING -> "Connecting"
    FeedStatus.LIVE -> "Live"
    FeedStatus.CACHED -> "Cached"
    FeedStatus.OFFLINE -> "Offline"
}

private fun openExternalUrl(context: android.content.Context, value: String) {
    val safe = try {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null
    } catch (_: Exception) {
        false
    }
    if (!safe) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
    } catch (_: ActivityNotFoundException) {
        // No browser is installed; keep the app stable and do not open an unsafe fallback.
    }
}

private fun formatDateTime(value: String): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return "Unknown"
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm", Locale.US).withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun relativeTime(value: Instant): String = relativeTime(value.toString())

private fun relativeTime(value: String): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return "time unavailable"
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}
