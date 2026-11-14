# Trading News — Android 0.3.0

Trading News is an English-language, dark-mode Android app for discovering market-related news headlines and opening the official Forex Factory calendar.

## Working functionality

- **Live news discovery:** GDELT DOC 2.0 article-list queries for a rolling 24-hour window.
- **Publisher links:** the app displays a headline, publisher host, GDELT-seen timestamp, and an HTTPS link to the original publisher. It does not republish full article bodies or images.
- **Feed reliability:** explicit LIVE, CACHED, OFFLINE, and CONNECTING states; bounded response parsing; compact local cache; 10-minute cache lifetime; 24-hour maximum cache age; 60-second per-device manual-refresh throttle.
- **Topic filters:** Markets, Geopolitics, Energy, Crypto, Companies, and Other. These are keyword-based navigation labels.
- **Instrument relevance:** deterministic keyword matches for supported forex pairs, metals, indices, oil, and crypto symbols. Scores are relevance hints only, not impact ratings, sentiment, price direction, or trading signals.
- **Background refresh:** optional WorkManager refresh on a best-effort 15-minute schedule. Android battery controls and Doze may delay it.
- **Local notifications:** optional local Android notifications for newly discovered headlines with keyword matches. This is not Firebase/remote push and requires the user's notification permission on Android 13+.

## Economic calendar

The Calendar screen opens the official Forex Factory calendar and Notices pages:
- https://www.forexfactory.com/calendar
- https://www.forexfactory.com/notices

The app does not scrape, download, cache, copy, or republish Forex Factory calendar schedules. Native event rows, forecast/previous/actual values, impact levels, and in-app event reminders are not enabled. They require written permission covering public mobile-app display and redistribution from Fair Economy, Inc., or a different provider whose licence explicitly permits these uses.

## Market reaction analysis

A pure calculation module can calculate observed post-event returns from real OHLC minute bars supplied to it. It is **not connected to a live price/history provider or the app UI** in 0.3.0. The application does not currently calculate or display measured market impact. No synthetic price bars or fabricated impact figures are used.

## Data sources

GDELT DOC 2.0 is queried directly from each device. The app attributes the GDELT Project in Settings and links headlines to original publisher pages. GDELT coverage is not exhaustive, and its search results may be duplicated, incomplete, late, or irrelevant.

- GDELT DOC API overview: https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/
- GDELT project and terms: https://www.gdeltproject.org/about.html

For broad public distribution, a shared backend/cache with monitoring, abuse controls, and request-rate management is recommended. The current client-side cache/throttle is per device and is not a substitute for server-side rate controls.

## Build and tests

Open **Actions** and select **Build Trading News APK**. CI runs:
- unit tests for news parsing and URL validation,
- tests for instrument relevance mapping,
- tests for observed price-reaction calculations,
- Android lint,
- debug APK compilation.

The resulting APK is a **debug build** signed with a temporary runner debug certificate. It is suitable for installation and testing, not a production-signed Google Play release. Production distribution requires a protected persistent signing key, complete privacy policy/support details, store declarations, device QA, dependency assessment, and external mobile security review.

## Security review and privacy

See the security audit and privacy-policy template in the docs directory. The static review and CI tests are not a penetration test, dynamic analysis, device QA, legal opinion, or security certification. Complete the privacy template before public distribution.
