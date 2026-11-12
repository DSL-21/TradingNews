# Trading News — Android 0.2.0

Trading News is an English-language, dark-mode Android market-headline reader. The repository contains the actual Android source tree directly; there is no source ZIP to unpack during CI.

## Current functionality

- Live GDELT DOC 2.0 headline discovery over the previous 24 hours.
- Publisher domain and the time GDELT first saw the article (`seendate`); this is not necessarily the publisher's publication time.
- Direct links to original publisher articles.
- Topic filters for Markets, Geopolitics, Energy, Crypto, Companies, and Other.
- Explicit LIVE, CACHED, OFFLINE, and CONNECTING feed states.
- Local cache, 10-minute cache lifetime, and 60-second manual-refresh throttle.
- No demo headlines, fabricated economic events, or heuristic market-impact score.

## Forex Factory calendar

The Calendar tab opens the official Forex Factory calendar in the browser. The app does not scrape, copy, cache, or redistribute Forex Factory calendar schedules.

Forex Factory's current notices prohibit copying, republication, or redistribution of its calendar schedules and FEED database without prior written consent. A native event feed and in-app reminders require written permission from Fair Economy, Inc.

- Official calendar: https://www.forexfactory.com/calendar
- Notices: https://www.forexfactory.com/notices

This is a deliberate legal boundary for a public-facing app, not a placeholder for an unapproved scraper.

## News source and attribution

GDELT DOC 2.0 is used for headline discovery. The app attributes GDELT and links to its project site from Settings. News cards link to original publishers; article bodies are not republished. Topic labels are keyword navigation aids, not validated impact ratings, trading signals, price-direction predictions, or recommendations.

- GDELT DOC API overview: https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/
- GDELT project and terms: https://www.gdeltproject.org/about.html

GDELT has service limits. The app throttles requests locally, but public-scale distribution should move aggregation behind a monitored shared backend with caching and abuse controls.

## Build APK from GitHub

All source files are committed directly to the repository root.

1. Open the **Actions** tab.
2. Select **Build Trading News APK**.
3. Open the run and wait for unit tests, Android lint, and APK build to finish.
4. Download `TradingNews-v0.2.0-debug-apk`.
5. Extract `app-debug.apk` and install it on Android.

The artifact is a debug APK for testing, not a production-signed Google Play release.

## Security review and remaining work

See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md).

The static review and unit tests are not a penetration test, dynamic analysis, dependency vulnerability scan, or device QA. Before public distribution, complete the privacy policy, support contact details, release signing, emulator/device testing, dependency scanning, and an external mobile security review.
