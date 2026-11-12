# Trading News — Data Sources

## GDELT DOC 2.0

**Purpose:** Discover recent news headlines related to financial markets, macroeconomics, energy, crypto, companies, and geopolitical events.

**Endpoint:** `https://api.gdeltproject.org/api/v2/doc/doc`

The Android client requests the article-list mode for the previous 24 hours. Results are limited to 50 records. The app displays the headline, publisher domain, time GDELT first saw the article (`seendate`), and a safe HTTPS link to the original publisher.

The app does not copy or display full third-party article bodies. Category tags are keyword-based navigation labels only. They are not a market-impact rating, a sentiment model, a price-direction forecast, or a trading recommendation.

Attribution and project information:
- https://www.gdeltproject.org/
- https://www.gdeltproject.org/about.html
- https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/

The API is queried directly by each device in this MVP. The client applies a 10-minute local cache and a 60-second manual-refresh throttle. Before broad public distribution, move aggregation behind a shared monitored backend with rate limiting, request logging that avoids collecting unnecessary personal data, and abuse controls.

## Forex Factory

**Purpose:** Economic calendar.

The Calendar tab opens the official website:
- https://www.forexfactory.com/calendar
- https://www.forexfactory.com/notices

The app does not scrape, download, cache, copy, or republish the calendar schedule or FEED database. Native economic-event data, forecasts, previous/actual values, impact levels, and in-app reminders must not be added until the operator obtains written permission covering public mobile-app display and redistribution from Fair Economy, Inc.

## Data interpretation

- `seendate` is the time GDELT first saw the article. It is not guaranteed to equal the publisher's publication time.
- GDELT search coverage is not exhaustive and can include duplicate, inaccurate, low-quality, or late-indexed sources.
- A headline keyword match does not establish causality or likely price direction.
- Users should verify important stories on the linked original publisher website.
