# Data layer

- NewsParser validates GDELT article records, bounds text and parser work, rejects unsafe URLs, removes duplicate links, classifies broad topics, and applies the locally stored instrument watchlist.
- TradingNewsRepository performs bounded HTTPS requests to GDELT DOC 2.0, stores compact article metadata on the device, and distinguishes live, cached, and offline states.
- NewsPreferences stores the user's supported-instrument watchlist locally. It is not sent to the news provider.
- InstrumentRelevanceEngine performs deterministic keyword relevance matching. It is not a sentiment, prediction, or market-impact model.
- NewsWorkScheduler and NewsRefreshWorker provide best-effort periodic background refresh; NewsNotifications posts optional local Android notifications.

No broker connection, broker credentials, price feed, sample/demo records, or synthetic market data are included. The client does not scrape or locally copy Forex Factory schedules. See DATA_SOURCES.md for data licensing and interpretation notes.
