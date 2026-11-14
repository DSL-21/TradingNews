package com.tradingnews.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentRelevanceEngineTest {
    @Test
    fun matchesUsInflationToUsdPairsAndGoldAsKeywordRelevance() {
        val matches = InstrumentRelevanceEngine.match("US CPI inflation surprises markets")

        assertTrue(matches.isNotEmpty())
        assertTrue(matches.any { it.symbol == "EURUSD" })
        assertTrue(matches.any { it.symbol == "XAUUSD" })
        assertTrue(matches.all { it.score in 1..100 })
    }

    @Test
    fun respectsTheUserWatchlist() {
        val matches = InstrumentRelevanceEngine.match(
            title = "Federal Reserve signals a change in interest rates",
            watchlist = setOf("EURUSD")
        )

        assertEquals(listOf("EURUSD"), matches.map { it.symbol })
    }

    @Test
    fun unrelatedHeadlineDoesNotProduceInstrumentMatches() {
        val matches = InstrumentRelevanceEngine.match("Local art museum opens a new exhibition")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun returnsDeterministicOrderedResults() {
        val headline = "US CPI rises as Federal Reserve debates interest rates and gold rallies"
        val first = InstrumentRelevanceEngine.match(headline)
        val second = InstrumentRelevanceEngine.match(headline)

        assertEquals(first, second)
        assertEquals(first.sortedWith(compareByDescending<InstrumentMatch> { it.score }.thenBy { it.symbol }), first)
    }
}
