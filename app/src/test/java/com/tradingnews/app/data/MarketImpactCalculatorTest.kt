package com.tradingnews.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MarketImpactCalculatorTest {
    @Test
    fun calculatesObservedReturnsAtRequestedHorizons() {
        val start = Instant.parse("2026-11-14T10:00:00Z")
        val bars = listOf(
            bar("EURUSD", start, 1.1000, 1.1010, 1.0990, 1.1000),
            bar("EURUSD", start.plusSeconds(300), 1.1000, 1.1020, 1.0990, 1.1011),
            bar("EURUSD", start.plusSeconds(900), 1.1011, 1.1030, 1.1000, 1.1022)
        )

        val result = MarketImpactCalculator.calculate("EURUSD", start, bars)

        assertNotNull(result)
        assertEquals(2, result!!.returnsPercent.size)
        assertTrue(result.returnsPercent.getValue(5) > 0.0)
        assertTrue(result.returnsPercent.getValue(15) > result.returnsPercent.getValue(5))
        assertEquals(3, result.sampleCount)
    }

    @Test
    fun refusesInsufficientOrInvalidPriceData() {
        val start = Instant.parse("2026-11-14T10:00:00Z")
        assertNull(
            MarketImpactCalculator.calculate(
                "EURUSD",
                start,
                listOf(bar("GBPUSD", start, 1.2, 1.3, 1.1, 1.2))
            )
        )
        assertNull(
            MarketImpactCalculator.calculate(
                "EURUSD",
                start,
                listOf(bar("EURUSD", start, 1.1, 1.0, 1.2, 1.1))
            )
        )
    }

    private fun bar(
        symbol: String,
        time: Instant,
        open: Double,
        high: Double,
        low: Double,
        close: Double
    ) = PriceBar(symbol, time, open, high, low, close)
}
