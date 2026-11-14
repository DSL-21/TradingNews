package com.tradingnews.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NewsWorkScheduler {
    const val PREFERENCES = "trading_news_preferences"
    const val KEY_AUTO_REFRESH = "auto_refresh_enabled"
    const val KEY_ALERTS_ENABLED = "background_alerts_enabled"
    const val UNIQUE_WORK_NAME = "trading_news_periodic_refresh"

    fun isAutoRefreshEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_REFRESH, true)

    fun isAlertsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALERTS_ENABLED, false)

    fun setAutoRefreshEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_REFRESH, enabled)
            // Background alerts require the periodic refresh worker.
            .apply {
                if (!enabled) putBoolean(KEY_ALERTS_ENABLED, false)
            }
            .apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        // If enabled programmatically while refresh is off, ensure delivery can run.
        if (enabled && !isAutoRefreshEnabled(context)) {
            setAutoRefreshEnabled(context, true)
        }
    }

    fun ensureScheduled(context: Context) {
        if (isAutoRefreshEnabled(context)) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<NewsRefreshWorker>(
            MIN_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private const val MIN_INTERVAL_MINUTES = 15L
}
