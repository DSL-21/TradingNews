package com.tradingnews.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.CancellationException

class NewsRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!NewsWorkScheduler.isAutoRefreshEnabled(applicationContext)) return Result.success()

        val repository = TradingNewsRepository(applicationContext)
        return try {
            val cacheGeneration = repository.getCacheGeneration()
            val previous = repository.readCachedFeed()
            val previousIds = previous?.articles?.mapTo(HashSet()) { it.id }.orEmpty()
            val updated = repository.load(forceRefresh = true)

            if (updated.status == FeedStatus.LIVE &&
                previous != null &&
                NewsWorkScheduler.isAlertsEnabled(applicationContext) &&
                repository.getCacheGeneration() == cacheGeneration
            ) {
                val newArticles = updated.articles
                    .filterNot { it.id in previousIds }
                    .filter { it.instrumentMatches.any { match -> match.score >= 40 } }
                    .take(5)
                NewsNotifications.showNewHeadlines(applicationContext, newArticles)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // This is periodic work: retry with WorkManager backoff instead of
            // imposing an arbitrary attempt limit on a temporary provider outage.
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
