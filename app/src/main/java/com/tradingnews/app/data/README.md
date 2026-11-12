# Data layer

- `NewsParser` validates GDELT article records, limits displayed text lengths, discards invalid timestamps and unsafe URLs, de-duplicates URLs, and classifies headlines into broad topic labels.
- `TradingNewsRepository` performs bounded HTTPS requests to GDELT DOC 2.0, stores the last successful feed locally, and clearly distinguishes live data from cached/offline states.
- `Models` contains the feed and article data structures.

No sample/demo records are included. The client does not scrape or locally copy Forex Factory schedules. See [DATA_SOURCES.md](../../../../../../../docs/DATA_SOURCES.md) for data licensing and interpretation notes.
