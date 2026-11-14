package com.tradingnews.app.data

import android.content.Context

/**
 * Stores the user's instrument watchlist on this device.
 * This is a local preference; no personal or account data is sent to GDELT.
 */
object NewsPreferences {
    private const val PREFERENCES = "trading_news_preferences"
    private const val KEY_WATCHLIST = "instrument_watchlist"

    fun getWatchlist(context: Context): Set<String> {
        val stored = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(KEY_WATCHLIST, null)

        val allowed = InstrumentRelevanceEngine.supportedSymbols.toSet()
        return (stored ?: allowed).filter { it in allowed }.toSet()
    }

    fun setWatchlist(context: Context, symbols: Set<String>) {
        val allowed = InstrumentRelevanceEngine.supportedSymbols.toSet()
        val safeSelection = symbols.intersect(allowed)
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_WATCHLIST, safeSelection.toSet())
            .apply()
    }
}
