package com.tradingnews.app.data

import java.time.Duration
import java.time.Instant

data class PriceBar(
    val symbol: String,
    val time: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

data class ObservedPriceReaction(
    val symbol: String,
    val eventTime: Instant,
    val baselinePrice: Double,
    val returnsPercent: Map<Int, Double>,
    val maxRangePercent: Double,
    val measuredAt: Instant,
    val sampleCount: Int
)

/**
 * Computes observed post-event price movement from real minute bars.
 * It does not infer causality or predict direction.
 */
object MarketImpactCalculator {
    fun calculate(
        symbol: String,
        eventTime: Instant,
        bars: List<PriceBar>,
        horizonsMinutes: List<Int> = listOf(5, 15, 30),
        tolerance: Duration = Duration.ofMinutes(2)
    ): ObservedPriceReaction? {
        if (symbol.isBlank() || bars.isEmpty() || horizonsMinutes.isEmpty()) return null
        if (horizonsMinutes.any { it <= 0 || it > 240 }) return null

        val valid = bars
            .filter {
                it.symbol.equals(symbol, ignoreCase = true) &&
                    it.open.isFinite() && it.high.isFinite() && it.low.isFinite() &&
                    it.close.isFinite() && it.open > 0.0 && it.close > 0.0 &&
                    it.high >= it.low && it.high >= it.open && it.high >= it.close &&
                    it.low <= it.open && it.low <= it.close
            }
            .distinctBy { it.time }
            .sortedBy { it.time }

        if (valid.size < 2) return null

        val baseline = nearestAtOrBefore(valid, eventTime, tolerance) ?: return null
        val returns = linkedMapOf<Int, Double>()
        for (horizon in horizonsMinutes.sorted()) {
            val target = eventTime.plus(Duration.ofMinutes(horizon.toLong()))
            val after = nearestAtOrBefore(valid, target, tolerance) ?: continue
            if (after.time <= baseline.time) continue
            returns[horizon] = ((after.close - baseline.close) / baseline.close) * 100.0
        }
        if (returns.isEmpty()) return null

        val lastHorizon = returns.keys.maxOrNull() ?: return null
        val lastBarTime = eventTime.plus(Duration.ofMinutes(lastHorizon.toLong()))
        val observedBars = valid.filter { !it.time.isBefore(baseline.time) && !it.time.isAfter(lastBarTime) }
        if (observedBars.isEmpty()) return null

        val maxHigh = observedBars.maxOf { it.high }
        val minLow = observedBars.minOf { it.low }
        val rangePercent = ((maxHigh - minLow) / baseline.close) * 100.0

        return ObservedPriceReaction(
            symbol = symbol.uppercase(),
            eventTime = eventTime,
            baselinePrice = baseline.close,
            returnsPercent = returns,
            maxRangePercent = rangePercent,
            measuredAt = observedBars.maxOf { it.time },
            sampleCount = observedBars.size
        )
    }

    private fun nearestAtOrBefore(
        bars: List<PriceBar>,
        target: Instant,
        tolerance: Duration
    ): PriceBar? {
        val result = bars.lastOrNull { !it.time.isAfter(target) } ?: return null
        val difference = Duration.between(result.time, target)
        return result.takeIf { difference <= tolerance }
    }
}
