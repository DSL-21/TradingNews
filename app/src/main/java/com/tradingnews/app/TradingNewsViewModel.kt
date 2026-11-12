package com.tradingnews.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradingnews.app.data.FeedStatus
import com.tradingnews.app.data.NewsItem
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
        viewModelScope.launch {
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
}
