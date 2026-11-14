package com.tradingnews.app.data

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tradingnews.app.MainActivity
import com.tradingnews.app.R

object NewsNotifications {
    const val CHANNEL_ID = "trading_news_market_updates"
    private const val CHANNEL_NAME = "Trading news updates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New market headlines relevant to your watched instruments"
            }
        )
    }

    fun clearAppNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.deleteNotificationChannel(CHANNEL_ID)
        }
    }

    fun canPost(context: Context): Boolean {
        if (!NewsWorkScheduler.isAlertsEnabled(context)) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun showNewHeadlines(context: Context, articles: List<NewsItem>) {
        if (articles.isEmpty() || !canPost(context)) return
        ensureChannel(context)
        val relevant = articles.filter { it.instrumentMatches.any { match -> match.score >= 40 } }
        if (relevant.isEmpty()) return

        val first = relevant.first()
        val symbols = first.instrumentMatches.filter { it.score >= 40 }.take(3).joinToString(", ") { it.symbol }
        val title = if (relevant.size == 1) "New market headline" else "${relevant.size} new market headlines"
        val body = if (symbols.isBlank()) first.title else "$symbols • ${first.title}"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trading_news)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            // canPost() checks notification settings and POST_NOTIFICATIONS. The
            // permission can still be revoked between the check and notify().
            NotificationManagerCompat.from(context).notify(NEWS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications were disabled while the worker was preparing this alert.
        }
    }

    private const val NEWS_NOTIFICATION_ID = 301
}
