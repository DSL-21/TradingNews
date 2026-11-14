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
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val stored = try {
            // SharedPreferences can contain values from an older/corrupted install.
            // Copy the set immediately; Android warns against mutating its returned set.
            preferences.getStringSet(KEY_WATCHLIST, null)?.toSet()
        } catch (_: ClassCastException) {
            preferences.edit().remove(KEY_WATCHLIST).apply()
            null
        }

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
