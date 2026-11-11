# Trading News 0.2.0 — Static Security Review

**Scope:** Android source project in the v0.2.0 source archive, including its manifest, Gradle configuration, GDELT client/parser, cache behaviour, and GitHub Actions build. This is a static source review and test plan, not a penetration test, device QA, legal opinion, or security certification.

## Problems found in v0.1.0 and addressed in v0.2.0

| Finding | Risk | v0.2.0 treatment |
|---|---|---|
| Empty backend or failed API calls silently displayed invented demo market events and headlines | High — false market information | Removed all demo data. Network failures show OFFLINE or clearly labelled cached data. |
| Headline impact was inferred from a few keywords | High — misleading implied trading signal | Removed impact scores. Only broad topic categories remain. |
| GDELT seendate was displayed as article publication time | Medium — timestamp misinterpretation | Labelled as the time GDELT first saw the article. |
| Publisher links accepted HTTP and credential-bearing URLs | Medium — unsafe external navigation | Only HTTPS URLs with a valid host and no embedded credentials are accepted; UI validates again before opening. |
| HTTP response size was not limited | Medium — memory-exhaustion risk | Added 2 MB content-length and character-count limits. |
| Repeated manual refreshes were not throttled | Medium — excessive provider requests | 10-minute local cache and 60-second manual refresh throttle. |
| Stale data was not distinguished from fresh data | Medium — user may mistake old data for live | Explicit LIVE, CACHED, OFFLINE, CONNECTING states and last-retrieved timestamp. |
| Forex Factory calendar was assumed usable in a public app without a redistribution licence | High — contractual/database-rights issue | App opens the official calendar; it does not copy, scrape, cache, or republish event schedules. Native integration requires written permission. |

## Remaining risks

1. The Android client queries GDELT directly. At public scale this creates per-device provider load. Add a shared backend/cache, monitoring, rate limits, abuse protection, and health alerts before large-scale distribution.
2. GDELT is a news-discovery feed. Headlines can be incomplete, duplicated, misleading, or irrelevant. Topic classification is keyword-based and does not estimate validated market impact or price direction.
3. GDELT attribution does not provide rights to republish third-party article bodies or images. The app only displays headlines/source metadata and links to original publishers.
4. Native Forex Factory calendar data and in-app event alerts remain disabled until written permission covers schedules, forecast/previous/actual values, caching, display, and redistribution.
5. Current GitHub Actions output is a debug APK. It is not a production-signed Play Store release.
6. No Android emulator/device QA, dependency vulnerability scan, dynamic analysis, or professional penetration test has been performed. CI tests and compilation do not replace those activities.

## Checks included in v0.2.0

Unit tests cover valid HTTPS article parsing, rejection of HTTP/javascript/credential URLs, invalid timestamps, ordering/deduplication, and topic classification. GitHub Actions runs unit tests, Android lint, and debug APK build.

## Before public store release

- Deploy shared news aggregation with HTTPS, rate controls, monitoring, structured logs, and alerting.
- Complete a real privacy policy and support contact details.
- Obtain written data-display/redistribution permission for Forex Factory or select a calendar provider whose licence permits public app display.
- Add protected release signing (GitHub Actions secrets; never commit keystores or passwords).
- Run emulator/device QA, dependency vulnerability scanning, and an external mobile security review.
- Complete Google Play Data safety and relevant app declarations.
