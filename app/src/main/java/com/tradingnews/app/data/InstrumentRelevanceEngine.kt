package com.tradingnews.app.data

import java.util.Locale

data class InstrumentMatch(
    val symbol: String,
    val score: Int,
    val reasons: List<String>
) {
    val band: String
        get() = when {
            score >= 70 -> "HIGH RELEVANCE"
            score >= 40 -> "MEDIUM RELEVANCE"
            else -> "LOW RELEVANCE"
        }
}

/**
 * Deterministic title-to-instrument relevance mapping.
 * This is a navigation aid, not sentiment, direction, or a trading signal.
 */
object InstrumentRelevanceEngine {
    val supportedSymbols = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD", "AUDUSD", "NZDUSD",
        "XAUUSD", "XAGUSD", "US500", "US100", "US30", "WTI", "BTCUSD", "ETHUSD"
    )

    private data class Rule(val points: Int, val label: String, val terms: List<String>)
    private data class Profile(val symbol: String, val rules: List<Rule>)

    private val commonUsdMacro = Rule(
        28, "US macro / USD",
        listOf("federal reserve", "fed rate", "us inflation", "us cpi", "nonfarm payrolls",
            "non-farm payrolls", "us jobs", "treasury yield", "us unemployment", "fomc")
    )
    private val profiles = listOf(
        Profile("EURUSD", listOf(
            Rule(62, "EUR / ECB", listOf("ecb", "european central bank", "eurozone", "euro area", "german inflation", "german pmi", "eurozone inflation")),
            Rule(48, "EUR macro", listOf("euro", "germany gdp", "france inflation", "eurozone gdp")),
            commonUsdMacro
        )),
        Profile("GBPUSD", listOf(
            Rule(62, "GBP / BoE", listOf("bank of england", "boe", "uk inflation", "uk cpi", "uk gdp", "british pound")),
            Rule(45, "UK macro", listOf("united kingdom", "britain", "sterling")),
            commonUsdMacro
        )),
        Profile("USDJPY", listOf(
            Rule(62, "JPY / BoJ", listOf("bank of japan", "boj", "japanese yen", "japan inflation", "japan cpi", "currency intervention")),
            Rule(45, "Japan macro", listOf("japan gdp", "japanese wage", "tokyo inflation")),
            commonUsdMacro
        )),
        Profile("USDCHF", listOf(
            Rule(60, "CHF / SNB", listOf("swiss national bank", "snb", "swiss inflation", "swiss franc", "switzerland gdp")),
            commonUsdMacro
        )),
        Profile("USDCAD", listOf(
            Rule(60, "CAD / BoC", listOf("bank of canada", "boc", "canadian inflation", "canada cpi", "canada employment", "canadian dollar")),
            Rule(40, "Oil / CAD", listOf("wti crude", "canadian oil", "oil price", "opec")),
            commonUsdMacro
        )),
        Profile("AUDUSD", listOf(
            Rule(60, "AUD / RBA", listOf("reserve bank of australia", "rba", "australian inflation", "australia jobs", "australian dollar")),
            Rule(38, "China / AUD", listOf("china pmi", "china gdp", "chinese stimulus", "iron ore")),
            commonUsdMacro
        )),
        Profile("NZDUSD", listOf(
            Rule(60, "NZD / RBNZ", listOf("reserve bank of new zealand", "rbnz", "new zealand inflation", "new zealand gdp", "kiwi dollar")),
            commonUsdMacro
        )),
        Profile("XAUUSD", listOf(
            Rule(58, "Gold / bullion", listOf("gold price", "gold rises", "gold falls", "bullion", "precious metals", "gold futures")),
            Rule(42, "Real rates / safe haven", listOf("real yields", "treasury yields", "safe haven", "geopolitical risk", "central bank buying")),
            commonUsdMacro
        )),
        Profile("XAGUSD", listOf(
            Rule(58, "Silver / metals", listOf("silver price", "silver rises", "silver falls", "silver futures")),
            Rule(35, "Industrial metals", listOf("industrial metals", "copper demand", "precious metals")),
            commonUsdMacro
        )),
        Profile("US500", listOf(
            Rule(62, "S&P 500 / US equities", listOf("s&p 500", "sp500", "wall street", "us equities", "dow jones", "us stocks")),
            Rule(45, "Rates / macro", listOf("federal reserve", "fomc", "us inflation", "us cpi", "nonfarm payrolls", "treasury yield", "earnings season"))
        )),
        Profile("US100", listOf(
            Rule(62, "Nasdaq / technology", listOf("nasdaq", "nasdaq 100", "semiconductor", "nvidia", "microsoft", "apple shares", "technology stocks")),
            Rule(45, "Rates / macro", listOf("federal reserve", "fomc", "us inflation", "us cpi", "treasury yield", "bond yields"))
        )),
        Profile("US30", listOf(
            Rule(62, "Dow / industrials", listOf("dow jones", "dow industrials", "industrial stocks", "blue chip stocks")),
            Rule(45, "US macro", listOf("federal reserve", "us inflation", "us cpi", "nonfarm payrolls", "treasury yield"))
        )),
        Profile("WTI", listOf(
            Rule(65, "Crude oil", listOf("wti", "crude oil", "oil prices", "oil futures", "opec", "opec+")),
            Rule(45, "Supply / geopolitics", listOf("oil supply", "oil production", "oil inventory", "strategic petroleum reserve", "middle east oil", "strait of hormuz"))
        )),
        Profile("BTCUSD", listOf(
            Rule(65, "Bitcoin / BTC", listOf("bitcoin", "btc", "bitcoin etf", "bitcoin reserve", "bitcoin halving")),
            Rule(42, "Crypto regulation", listOf("crypto regulation", "digital assets", "cryptocurrency exchange", "sec crypto"))
        )),
        Profile("ETHUSD", listOf(
            Rule(65, "Ethereum / ETH", listOf("ethereum", "ether", "eth etf", "ethereum upgrade")),
            Rule(42, "Crypto regulation", listOf("crypto regulation", "digital assets", "cryptocurrency exchange", "sec crypto"))
        ))
    )

    fun match(title: String, watchlist: Set<String> = supportedSymbols.toSet()): List<InstrumentMatch> {
        val normalized = " ${title.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ")} "
        return profiles.asSequence()
            .filter { it.symbol in watchlist }
            .mapNotNull { profile ->
                val matchedRules = profile.rules.filter { rule ->
                    rule.terms.any { term ->
                        val normalizedTerm = " ${term.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ")} "
                        normalized.contains(normalizedTerm)
                    }
                }
                if (matchedRules.isEmpty()) return@mapNotNull null
                val score = matchedRules.sumOf { it.points }.coerceAtMost(100)
                InstrumentMatch(
                    symbol = profile.symbol,
                    score = score,
                    reasons = matchedRules.map { it.label }.distinct().take(2)
                )
            }
            .sortedWith(compareByDescending<InstrumentMatch> { it.score }.thenBy { it.symbol })
            .toList()
    }
}
