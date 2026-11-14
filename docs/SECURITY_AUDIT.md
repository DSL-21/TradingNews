# Trading News 0.3.0 — Static Security and Reliability Review

**Review basis:** Android source committed to the repository, manifest, Gradle configuration, GDELT client/parser, WorkManager scheduling, notification path, calendar entry screen, and current CI workflow. This is a static source review plus automated unit tests, Android lint, and a build attempt. It is not a penetration test, dynamic analysis, device QA, legal opinion, or security certification.

## Controls present

| Area | Current control |
|---|---|
| Android permissions | INTERNET and optional POST_NOTIFICATIONS; no location, contacts, camera, or microphone permission |
| Notifications | Runtime permission checked on Android 13+; app-level notification state checked; SecurityException handled if permission is revoked between check and notify |
| Transport | GDELT endpoint uses HTTPS; Android cleartext traffic is disabled |
| External article links | HTTPS-only; parser rejects embedded credentials, non-default ports, local host/IP literals and invalid hosts |
| Network timeouts | 10-second connect timeout and 15-second read timeout |
| Response limits | 2 MB GDELT response limit |
| Redirects | Automatic HTTP redirects disabled |
| Parser bounds | At most 250 input records are examined and 50 valid articles are retained |
| Article labels | Publisher name shown from the validated URL host rather than trusting the provider's free-form domain label |
| Deduplication | Duplicate article URLs are discarded |
| Cache | Compact parsed article metadata only; invalid, future-dated, or older-than-24-hour cache is discarded |
| Refresh control | 10-minute local cache and 60-second per-device manual refresh throttle |
| Feed state | LIVE, CACHED, OFFLINE, and CONNECTING are distinct |
| Background refresh | Unique WorkManager task with network constraint and 15-minute minimum interval |
| Local notifications | Opt-in local Android notifications; disabled when background refresh is disabled |
| Data handling | No account, embedded API secret, advertising SDK, or analytics SDK |
| Calendar | Official Forex Factory page is opened externally; the app does not scrape or republish its schedule |

## Findings addressed in the 0.3.0 work

1. **Missing calendar composable:** MainActivity referenced a composable that did not exist. Added a standalone Calendar screen that opens the official provider-hosted calendar and Notices pages, with an explicit data-rights explanation.
2. **Notification permission race:** Lint identified a missing-permission path at notification delivery. Added explicit permission checks and SecurityException handling.
3. **Refresh/alert inconsistency:** Disabling automatic refresh now also disables background headline alerts; the alert toggle is disabled while refresh is off.
4. **Version ambiguity:** App version is incremented to 0.3.0.
5. **Missing tests:** Added unit tests for instrument relevance matching and observed OHLC post-event calculations, alongside existing parser tests.
6. **Fabricated fallback data:** No demo headlines or fabricated economic events are shown.
7. **Unvalidated impact claims:** Keyword-based category and instrument matching are relevance hints only, not a validated impact model or a trading signal.

## Actual functionality vs prepared modules

- GDELT headline discovery is wired into the Android app.
- Instrument relevance is wired into headline parsing and cards. It is deterministic keyword matching, not an AI model, sentiment model, or prediction.
- The observed price-reaction calculator is a standalone utility tested with supplied OHLC bars. There is no connected market-price provider, so it is not currently run on live economic events and is not displayed in the UI.
- The economic-calendar backend adapter is not configured by default. The Calendar screen opens the official Forex Factory website. Native event redistribution requires written permission or a differently licensed provider.
- Background notifications are local WorkManager notifications. They are not instant remote push and may be delayed by Android battery management.

## Remaining risks and work before broad public release

1. **No shared backend:** Each device queries GDELT directly. Deploy a shared backend/cache with monitoring, request limits, abuse controls, operational alerts, and a privacy review before scaling.
2. **Provider quality:** GDELT is a discovery feed, not a guarantee of completeness or accuracy. Headlines can be duplicated, incomplete, misleading, irrelevant, or late-indexed.
3. **Publisher rights:** The app links to the publisher. GDELT attribution does not itself grant rights to republish third-party article text or images.
4. **Forex Factory:** Current Notices prohibit copying, republication, and redistribution of calendar schedules and FEED data without prior written consent. Native integration must remain disabled until the operator has written permission for the exact mobile display and redistribution use.
5. **No native calendar alerts:** No economic-event alerts are sent because no licensed in-app event source is configured.
6. **No connected price feed:** The app does not calculate/display actual price reaction. Add a properly licensed market price/history source and event timestamps before presenting this as a product feature.
7. **No dynamic testing:** No Android device/emulator test matrix, MobSF/dynamic assessment, or external penetration test has been performed.
8. **Release signing:** CI debug APKs are signed with temporary runner debug certificates. They are installable test artifacts, not production-signed Play Store builds. Store signing must use protected secrets and a persistent private keystore; never commit a keystore or password.
9. **Privacy policy:** The included privacy policy is a template. Replace every placeholder with the operator's real identity, contact, processing purposes, retention details, provider disclosure, jurisdiction-specific legal basis, and store declarations.

## Automated tests

The test suite includes:
- valid HTTPS parsing, rejection of unsafe URLs, invalid timestamps, cache timestamps, sorting, deduplication, bounded results, and topic categorization;
- deterministic instrument keyword matching, watchlist restriction, unrelated headlines, and stable ordering;
- observed post-event price calculations from supplied OHLC data and rejection of insufficient/invalid price bars.

CI runs unit tests, Android lint, and debug APK compilation. A passing CI run establishes only that the tested source compiles and the tested cases pass; it does not prove every Android version, runtime state, data provider, or attack scenario is safe.
