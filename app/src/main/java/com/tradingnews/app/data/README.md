# Data layer

- NewsParser validates GDELT article records, bounds text and parser work, rejects unsafe URLs, removes duplicate links, classifies broad topics, and applies the locally stored instrument watchlist.
- TradingNewsRepository performs bounded HTTPS requests to GDELT DOC 2.0, stores compact article metadata on the device, and distinguishes live, cached, and offline states.
- NewsPreferences stores the user's supported-instrument watchlist locally. It is not sent to the news provider.
- InstrumentRelevanceEngine performs deterministic keyword relevance matching. It is not a sentiment, prediction, or market-impact model.
- MarketImpactCalculator is a standalone utility for calculating observed returns from real OHLC bars supplied by a future licensed price-data integration. It is not currently connected to a provider or displayed in the UI.
- NewsWorkScheduler and NewsRefreshWorker provide best-effort periodic background refresh; NewsNotifications posts optional local Android notifications.

No sample/demo records are included. The client does not scrape or locally copy Forex Factory schedules. See DATA_SOURCES.md for data licensing and interpretation notes.
