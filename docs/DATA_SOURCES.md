# Trading News — Data Sources

## 1. GDELT DOC 2.0 — market and Forex headlines

**Purpose:** Discover recent stories related to Forex, currencies, central banks, monetary policy, inflation, interest rates, bonds, equity markets, commodities, energy, crypto, company results and geopolitics.

**Endpoint:** https://api.gdeltproject.org/api/v2/doc/doc  
**Project information:** https://www.gdeltproject.org/  
**DOC 2.0 announcement and query operators:** https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/

The Android client requests article-list mode for a market-related query over the previous 24 hours, sorted newest first, and asks for at most 50 records. It displays a headline, publisher hostname, time GDELT first saw the article (seendate), and a validated HTTPS link to the original publisher.

The app does not copy or display full third-party article bodies. Topic categories and instrument matches are keyword-based navigation/relevance aids only. They are not market-impact ratings, sentiment, price-direction forecasts or trading recommendations.

### Search scope

The app currently performs client-side search over the loaded set of up to 50 headlines. It searches titles, publisher hostnames, matched instrument symbols and relevance-reason labels. Topic and instrument filters are applied locally. A future version may add separate on-demand GDELT queries, but the current UI must not imply that its search scans the entire web or the full GDELT archive.

### Limits and interpretation

- GDELT coverage is not exhaustive. Articles may be duplicated, late-indexed, inaccurate, low-quality or irrelevant.
- `seendate` is when GDELT first saw an article. It is not guaranteed to equal the publisher's publication time.
- The client uses HTTPS, bounded response sizes, timeouts, URL validation, duplicate removal and a compact local cache.
- Each installed device queries GDELT directly. A large public audience may require a shared cache and central request monitoring.
- Important stories should be checked at their linked original publisher.

## 2. Forex Factory — official economic calendar

**Official calendar:** https://www.forexfactory.com/calendar  
**Official notices and terms:** https://www.forexfactory.com/notices

Forex Factory's calendar page provides weekly export links for JSON, ICS, CSV and XML, hosted on its own `nfs.faireconomy.media` domain. The page adds a changing `version` query parameter to the export links. Trading News opens the calendar page so the user can select the current provider-generated export URL, rather than embedding a stale hard-coded export URL in the app.

These are source-hosted files. Trading News does not automatically fetch, parse, cache, rehost, copy or redistribute their contents.

### Why the app does not import the export

The data is technically accessible over the internet, but technical accessibility is not the same as permission to republish it inside another product. Forex Factory's notices state that copying, republication or redistribution of its content — including calendar schedules and the Fair Economy Economic Database (FEED) — is prohibited without prior written consent.

For that reason, the native app opens the original calendar/export URLs rather than creating its own calendar database. The user still gets the official schedule, impact levels, event details and publisher-maintained changes directly from Forex Factory.

Before implementing an in-app calendar that stores or republishes these events, the project owner must obtain written permission from Fair Economy, Inc. covering that use.

## 3. Official central-bank and macroeconomic sources

These are useful primary sources for future source-specific news feeds or direct verification. They are not currently separate in-app RSS integrations; GDELT is the current aggregated discovery source.

- European Central Bank RSS feeds: https://www.ecb.europa.eu/home/html/rss.en.html
- Federal Reserve RSS feeds: https://www.federalreserve.gov/feeds/feeds.htm
- U.S. Bureau of Labor Statistics: https://www.bls.gov/feed/

Official-source RSS is a good next expansion if direct central-bank and macro release headlines are needed. Any additional feed should be added with source attribution, link validation, response limits and tests.

## 4. Market price history

The repository contains a standalone OHLC reaction calculator with tests. No licensed live or historical market-price provider is connected to the app. The app therefore does not display measured post-event price reactions or synthetic prices.

## 5. Data interpretation

- A headline/instrument keyword match does not establish causality or likely price direction.
- A publisher hostname is derived from the validated article URL.
- The app links to original publishers and does not substitute its own copy of third-party articles or calendar records.
- Users should verify important news and event details with the original source.
