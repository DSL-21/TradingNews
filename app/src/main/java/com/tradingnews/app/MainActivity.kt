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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.remember
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
import com.tradingnews.app.data.InstrumentRelevanceEngine
import com.tradingnews.app.data.NewsCategories
import com.tradingnews.app.data.NewsItem
import com.tradingnews.app.data.NewsPreferences
import com.tradingnews.app.data.NewsWorkScheduler
import com.tradingnews.app.data.TradingNewsRepository
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
                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onRefresh = { viewModel.refresh(forceRefresh = true) },
                    onClearNewsCache = { viewModel.clearNewsCache() },
                    onClearAllLocalData = { viewModel.clearAllLocalData() }
                )
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
                Text("FOREX & MARKET NEWS", fontSize = 9.sp, letterSpacing = 1.3.sp, color = TextMuted)
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
    val context = LocalContext.current
    var selectedCategory by rememberSaveable { mutableStateOf(NewsCategories.ALL) }
    var selectedInstrument by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val watchlist = remember { NewsPreferences.getWatchlist(context).sorted() }
    val filters = listOf(
        NewsCategories.ALL to "All",
        NewsCategories.MARKETS to "Markets",
        NewsCategories.GEOPOLITICS to "Geopolitics",
        NewsCategories.ENERGY to "Energy",
        NewsCategories.CRYPTO to "Crypto",
        NewsCategories.COMPANIES to "Companies",
        NewsCategories.OTHER to "Other"
    )
    val query = searchQuery.trim()
    val filtered = state.articles.filter { article ->
        val matchesSearch = query.isBlank() ||
            article.title.contains(query, ignoreCase = true) ||
            article.source.contains(query, ignoreCase = true) ||
            article.instrumentMatches.any { match ->
                match.symbol.contains(query, ignoreCase = true) ||
                    match.reasons.any { it.contains(query, ignoreCase = true) }
            }
        val matchesCategory = selectedCategory == NewsCategories.ALL || article.category == selectedCategory
        val matchesInstrument = selectedInstrument == "ALL" ||
            article.instrumentMatches.any { it.symbol == selectedInstrument }
        matchesSearch && matchesCategory && matchesInstrument
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SectionTitle("MARKET NEWS", "Search headlines and filter by topic or instrument")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.take(120) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search headlines, sources or symbols", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(
                            onClick = { searchQuery = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", color = Accent, fontSize = 10.sp)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    cursorColor = Accent,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                )
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                FilterChip(
                    selected = selectedInstrument == "ALL",
                    onClick = { selectedInstrument = "ALL" },
                    label = { Text("All instruments", fontSize = 10.sp) }
                )
                watchlist.forEach { symbol ->
                    FilterChip(
                        selected = selectedInstrument == symbol,
                        onClick = { selectedInstrument = symbol },
                        label = { Text(symbol, fontSize = 10.sp) }
                    )
                }
            }
            Text(
                "${filtered.size} matching headlines",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        when {
            state.isLoading && state.articles.isEmpty() -> LoadingPanel()
            filtered.isEmpty() -> EmptyState(
                if (state.articles.isEmpty()) "No headlines available" else "No matching headlines",
                if (state.articles.isEmpty()) state.notice else "Try a different search, category or instrument, or refresh the feed."
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filtered, key = { it.id }) { NewsCard(it, expanded = true) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: TradingNewsUiState,
    onRefresh: () -> Unit,
    onClearNewsCache: () -> Unit,
    onClearAllLocalData: () -> Unit
) {
    val context = LocalContext.current
    var autoRefresh by rememberSaveable {
        mutableStateOf(NewsWorkScheduler.isAutoRefreshEnabled(context))
    }
    var alertsEnabled by rememberSaveable {
        mutableStateOf(NewsWorkScheduler.isAlertsEnabled(context))
    }
    var selectedInstruments by remember {
        mutableStateOf(NewsPreferences.getWatchlist(context))
    }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var storageRevision by rememberSaveable { mutableStateOf(0) }
    val storageInfo = remember(state.lastUpdated, storageRevision) {
        TradingNewsRepository(context).getStorageInfo()
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
                    Text("Instrument watchlist", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Choose which instruments appear as headline relevance matches. Matches are keyword hints, not trading signals.",
                        color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    Text(
                        "${selectedInstruments.size} of ${InstrumentRelevanceEngine.supportedSymbols.size} instruments selected",
                        color = Accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InstrumentRelevanceEngine.supportedSymbols.forEach { symbol ->
                            FilterChip(
                                selected = symbol in selectedInstruments,
                                onClick = {
                                    val updated = if (symbol in selectedInstruments) {
                                        selectedInstruments - symbol
                                    } else {
                                        selectedInstruments + symbol
                                    }
                                    selectedInstruments = updated
                                    NewsPreferences.setWatchlist(context, updated)
                                    onRefresh()
                                },
                                label = { Text(symbol, fontSize = 9.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                selectedInstruments = InstrumentRelevanceEngine.supportedSymbols.toSet()
                                NewsPreferences.setWatchlist(context, selectedInstruments)
                                onRefresh()
                            }
                        ) { Text("Select all", color = Accent, fontSize = 11.sp) }
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
                    Text("Forex Factory calendar", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text(
                        "The Calendar tab opens the official Forex Factory calendar and its weekly JSON, ICS and CSV exports directly from the original host. Trading News does not copy or rehost the FEED database. No provider API key is required.",
                        color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp
                    )
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy and storage", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("Trading News keeps only a small rolling cache of headline metadata. It does not download full articles or build a historical archive.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Headline cache", color = TextMuted, fontSize = 11.sp)
                        Text("${storageInfo.cachedArticleCount} / 50 • ${formatBytes(storageInfo.cacheBytes)}", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Local preferences", color = TextMuted, fontSize = 11.sp)
                        Text(formatBytes(storageInfo.settingsBytes), color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated local user data", color = TextMuted, fontSize = 11.sp)
                        Text(formatBytes(storageInfo.estimatedUserDataBytes), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Last cache update: ${storageInfo.retrievedAt?.toString()?.let(::formatDateTime) ?: "No cached headlines"}", color = TextMuted, fontSize = 10.sp)
                    Text("This is an estimate of saved values, not the APK size. It excludes Android/WorkManager system records and your browser's own cache. Expired cache is discarded after 24 hours; a refresh replaces the previous cache.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
                    HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showClearCacheDialog = true }) {
                            Text("Clear news cache", color = Accent, fontSize = 11.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text("Clear all local data", color = Error, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About Trading News", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("Version 0.6.0 • Android", color = TextMuted, fontSize = 11.sp)
                    Text("Informational use only. Relevance scores are deterministic keyword matches. Observed event returns describe price movement after a timestamp; they do not prove causality or predict future prices.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    Text("Feed status: ${statusText(state.status)}", color = TextMuted, fontSize = 10.sp)
                    state.lastUpdated?.let { Text("Last retrieved: ${formatDateTime(it)}", color = TextMuted, fontSize = 10.sp) }
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = Panel,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Clear news cache?") },
            text = { Text("This removes saved headlines and their refresh timestamp. Your instrument watchlist and refresh settings remain unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    onClearNewsCache()
                    storageRevision += 1
                }) { Text("Clear cache", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = Panel,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Clear all local data?") },
            text = { Text("This removes saved headlines and preferences, resets the instrument watchlist to defaults, disables and cancels background refresh, disables alerts, and clears Trading News notifications. Android's notification permission is managed separately in system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllDialog = false
                    onClearAllLocalData()
                    autoRefresh = false
                    alertsEnabled = false
                    selectedInstruments = InstrumentRelevanceEngine.supportedSymbols.toSet()
                    storageRevision += 1
                }) { Text("Clear all data", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
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
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
            Spacer(Modifier.width(10.dp))
            Text("Loading live headlines...", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(message, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
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

private fun formatBytes(value: Long): String {
    val safe = value.coerceAtLeast(0L)
    return if (safe < 1_024L) "$safe B"
    else String.format(Locale.US, "%.1f KB", safe / 1_024.0)
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
