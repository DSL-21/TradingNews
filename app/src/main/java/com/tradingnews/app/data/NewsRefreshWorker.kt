package com.tradingnews.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException

class NewsRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!NewsWorkScheduler.isAutoRefreshEnabled(applicationContext)) return Result.success()

        val repository = TradingNewsRepository(applicationContext)
        return try {
            val previous = repository.readCachedFeed()
            val previousIds = previous?.articles?.mapTo(HashSet()) { it.id }.orEmpty()
            val updated = repository.load(forceRefresh = true)

            if (updated.status == FeedStatus.LIVE && previous != null && NewsWorkScheduler.isAlertsEnabled(applicationContext)) {
                val newArticles = updated.articles
                    .filterNot { it.id in previousIds }
                    .filter { it.instrumentMatches.any { match -> match.score >= 40 } }
                    .take(5)
                NewsNotifications.showNewHeadlines(applicationContext, newArticles)
            }
            Result.success()
        } catch (error: IOException) {
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        } catch (_: Exception) {
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val MAX_RETRIES = 5
    }
}
