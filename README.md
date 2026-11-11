# Trading News — Android 0.2.0

Dark-mode Android app for live market headlines. The demo feed has been removed.

## Current functionality

- Live GDELT DOC 2.0 headline discovery (previous 24 hours).
- Publisher domain, GDELT-seen timestamp, and a safe link to the original article.
- Topic filters for Markets, Geopolitics, Energy, Crypto, Companies, and Other.
- Explicit LIVE, CACHED, OFFLINE, and CONNECTING states.
- Local 10-minute cache, 60-second manual-refresh throttle, bounded network timeouts and response parsing.
- No sample headlines, fabricated economic events, or heuristic market-impact scores.

## Forex Factory calendar

The Calendar tab opens the official Forex Factory calendar in the browser. It does not scrape, copy, cache, or redistribute event schedules.

Forex Factory's current notices prohibit copying, republication, or redistribution of calendar schedules and the FEED database without prior written consent. Native in-app calendar rows and event reminders therefore require written permission from Fair Economy, Inc.

Official calendar: https://www.forexfactory.com/calendar  
Notices: https://www.forexfactory.com/notices

## Data and attribution

GDELT DOC 2.0 is used for headline discovery. The app attributes and links to the GDELT Project and opens original publisher articles. The app does not republish article bodies. Topic labels are keyword-based navigation aids, not market-impact ratings, trading signals, price-direction predictions, or recommendations.

GDELT documentation: https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/  
GDELT terms and attribution: https://www.gdeltproject.org/about.html

## Build the APK with GitHub Actions

1. Upload `TradingNews-v0.2.0-source.zip` to the root of this repository.
2. Commit the upload to `main`.
3. The **Build Trading News APK** workflow starts automatically.
4. The workflow runs parser unit tests and Android lint, then builds the debug APK.
5. Open the completed run in **Actions** and download `TradingNews-v0.2.0-debug-apk`.
6. Extract `app-debug.apk` and install it on Android.

The artifact is a debug APK for testing, not a production-signed Google Play release. Release signing, device QA, privacy-policy completion, and app-store review remain necessary before public store distribution.

## Static security review

See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md). The review is static source analysis plus unit tests; it is not a penetration test, emulator/device QA, or professional security certification.

Known limitations before broad public deployment:
- Each device currently queries GDELT directly. A shared backend/cache, monitoring, and abuse/rate controls are recommended at public scale.
- Forex Factory native calendar data requires written redistribution permission.
- Keyword topic categories are not a validated market-impact model.
- No automatic economic-event alerts are enabled because the in-app event feed is not licensed.
