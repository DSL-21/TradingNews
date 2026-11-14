package com.tradingnews.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradingnews.app.data.FeedStatus
import com.tradingnews.app.data.NewsItem
import com.tradingnews.app.data.NewsNotifications
import com.tradingnews.app.data.NewsWorkScheduler
import com.tradingnews.app.data.TradingNewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TradingNewsUiState(
    val articles: List<NewsItem> = emptyList(),
    val status: FeedStatus = FeedStatus.CONNECTING,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdated: String? = null,
    val notice: String = "Connecting to the live news feed..."
)

class TradingNewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TradingNewsRepository(application)
    private var refreshJob: kotlinx.coroutines.Job? = null

    var uiState by mutableStateOf(TradingNewsUiState())
        private set

    init {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repository.readCachedFeed() }
            cached?.let {
                uiState = TradingNewsUiState(
                    articles = it.articles,
                    status = FeedStatus.CACHED,
                    isLoading = false,
                    lastUpdated = it.retrievedAt.toString(),
                    notice = it.notice
                )
            }
            refresh(forceRefresh = false)
        }
    }

    fun refresh(forceRefresh: Boolean = true) {
        if (uiState.isRefreshing) return
        refreshJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = uiState.articles.isEmpty(),
                isRefreshing = true,
                notice = if (uiState.articles.isEmpty()) "Connecting to GDELT DOC 2.0..." else uiState.notice
            )
            try {
                val feed = repository.load(forceRefresh)
                uiState = uiState.copy(
                    articles = feed.articles,
                    status = feed.status,
                    isLoading = false,
                    isRefreshing = false,
                    lastUpdated = feed.retrievedAt.toString(),
                    notice = feed.notice
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                uiState = uiState.copy(
                    status = FeedStatus.OFFLINE,
                    isLoading = false,
                    isRefreshing = false,
                    notice = "The live news feed is unavailable. Check your connection and try again."
                )
            }
        }
    }

    fun clearNewsCache() {
        refreshJob?.cancel()
        refreshJob = null
        repository.clearNewsCache()
        getApplication<Application>().deleteSharedPreferences("trading_news_calendar_cache")
        uiState = TradingNewsUiState(
            status = FeedStatus.OFFLINE,
            isLoading = false,
            notice = "Local headline cache cleared. Refresh to load current headlines."
        )
    }

    fun clearAllLocalData() {
        refreshJob?.cancel()
        refreshJob = null
        NewsWorkScheduler.resetLocalSettings(getApplication())
        repository.clearNewsCache()
        NewsNotifications.clearAppNotifications(getApplication())
        uiState = TradingNewsUiState(
            status = FeedStatus.OFFLINE,
            isLoading = false,
            notice = "Local app data cleared. Background refresh and alerts are disabled."
        )
    }
}
