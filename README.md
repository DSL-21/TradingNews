# Trading News — Android 0.2.0

Trading News is an English-language, dark-mode Android market-headline reader. Source files are committed directly to this repository.

## Current functionality

- Live GDELT DOC 2.0 headline discovery over the previous 24 hours.
- Publisher hostname and the time GDELT first saw the article (`seendate`); this is not necessarily the publisher's publication time.
- Direct HTTPS links to original publisher articles.
- Topic filters for Markets, Geopolitics, Energy, Crypto, Companies, and Other.
- Explicit LIVE, CACHED, OFFLINE, and CONNECTING feed states.
- Local compact cache, 10-minute cache lifetime, 24-hour maximum cache age, and 60-second manual-refresh throttle.
- Bounded response size and parser work; no demo headlines, fabricated economic events, or heuristic market-impact score.

## Latest standard GitHub Release

**[Trading News 0.2.0 — open Releases and download APK](https://github.com/DSL-21/TradingNews/releases/tag/v0.2.0)**

This is a standard GitHub Release, not a pre-release. The attached `app-debug.apk` is a debug variant signed with a temporary runner debug certificate. It is installable for testing, but it is not a production-signed Google Play package. A persistent private signing key must be configured as protected GitHub Actions secrets before public-store distribution.

## Forex Factory calendar

The Calendar tab opens the official website:
- https://www.forexfactory.com/calendar
- https://www.forexfactory.com/notices

The app does not scrape, download, cache, copy, or republish Forex Factory calendar schedules. Native event data, forecasts, previous/actual values, impact levels, and in-app reminders must not be added until the operator has written permission covering public mobile-app display and redistribution from Fair Economy, Inc.

## News source and attribution

GDELT DOC 2.0 is used for headline discovery. The app links to GDELT project information from Settings and links each headline to its original publisher. Article bodies are not republished. Topic labels are keyword-based navigation aids only, not validated impact ratings, trading signals, price-direction predictions, or recommendations.

- GDELT DOC API overview: https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/
- GDELT project and terms: https://www.gdeltproject.org/about.html

GDELT has service limits. Each device currently queries the provider directly. A shared monitored backend, central cache, and abuse/rate controls are recommended before scaling to a larger public audience.

## Build and test

Open **Actions** and choose **Build Trading News APK** to run the continuous-integration workflow. It executes parser unit tests, Android lint, and an APK build. The **Release Trading News** workflow runs tests, lint, both Android variants, APK signature/package verification, then publishes a standard GitHub Release.

GitHub Actions uses pinned action commit SHAs. Dependabot checks Gradle and GitHub Actions dependencies weekly.

## Security review and remaining work

See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md).

The static review and automated tests are not a penetration test, dynamic analysis, professional security certification, or device QA. Before public app-store distribution, complete the privacy policy, support contact details, persistent release signing, emulator/device testing, dependency vulnerability assessment, and an external mobile security review.
