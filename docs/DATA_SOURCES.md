# Trading News — Data Sources

## GDELT DOC 2.0

**Purpose:** Discover recent news headlines related to financial markets, macroeconomics, energy, crypto, companies, and geopolitical events.

**Endpoint:** https://api.gdeltproject.org/api/v2/doc/doc

The Android client requests article-list mode for the previous 24 hours and asks for at most 50 records. The app displays the headline, publisher hostname, time GDELT first saw the article (seendate), and a validated HTTPS link to the original publisher.

The app does not copy or display full third-party article bodies. Category labels and instrument matches are keyword-based navigation/relevance aids only. They are not market-impact ratings, sentiment, price-direction forecasts, or trading recommendations.

Attribution and project information:
- https://www.gdeltproject.org/
- https://www.gdeltproject.org/about.html
- https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/

The API is queried directly by each device. The client applies a 10-minute local cache, a 24-hour maximum cache age, and a 60-second manual-refresh throttle. The user's instrument watchlist is stored locally and is not sent as personal data to GDELT. For a larger public audience, move aggregation behind a shared monitored backend with central rate limits and abuse controls.

## Forex Factory

**Purpose:** Economic calendar.

The Calendar tab opens the official website:
- https://www.forexfactory.com/calendar
- https://www.forexfactory.com/notices

The app does not scrape, download, cache, copy, or republish the calendar schedule or FEED database. Native economic-event rows, forecasts, previous/actual values, impact levels, and in-app reminders must not be added until the operator obtains written permission covering public mobile-app display and redistribution from Fair Economy, Inc., or chooses another provider whose licence explicitly allows these uses.

## Market price history

The OHLC reaction calculator accepts price bars supplied to it and is covered by unit tests. No licensed market-price/history provider is connected in this release. The app therefore does not show measured price reactions or synthetic prices.

## Data interpretation

- seendate is the time GDELT first saw the article. It is not guaranteed to equal the publisher's publication time.
- GDELT search coverage is not exhaustive and can include duplicate, inaccurate, low-quality, or late-indexed sources.
- An instrument keyword match does not establish causality or likely price direction.
- Users should verify important stories on the linked original publisher website.
