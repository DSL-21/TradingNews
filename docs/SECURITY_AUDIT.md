# Trading News 0.4.0 — Static Security and Reliability Review

**Review basis:** Android source, manifest, Gradle configuration, GDELT client/parser, local watchlist, WorkManager scheduling, notification path, calendar entry screen, GitHub Actions build and signed-release workflows. This is static source review plus automated unit tests, Android lint, and CI compilation. It is not a penetration test, dynamic analysis, physical-device QA, legal opinion, or security certification.

## Controls present

| Area | Current control |
|---|---|
| Android permissions | INTERNET and optional POST_NOTIFICATIONS; no location, contacts, camera, or microphone permission |
| Notifications | Runtime permission checked on Android 13+; app-level notification state checked; SecurityException handled if permission is revoked between check and notify |
| Transport | GDELT uses HTTPS; Android cleartext traffic is disabled |
| External article links | HTTPS-only; parser rejects embedded credentials, non-default ports, local host/IP literals and invalid hosts |
| Network timeouts | 10-second connect and 15-second read timeout |
| Response limits | 2 MB response and bounded parser work |
| Redirects | Automatic HTTP redirects disabled |
| Parser bounds | At most 250 input records examined and 50 valid articles retained |
| Publisher labels | Derived from the validated article URL host, not the provider's free-form domain field |
| Deduplication | Duplicate article URLs discarded |
| Cache | Compact parsed fields only; invalid, future-dated, or older-than-24-hour cache discarded |
| Refresh control | 10-minute local cache and 60-second per-device manual refresh throttle |
| Feed states | LIVE, CACHED, OFFLINE, and CONNECTING are distinct |
| Watchlist | User-selected symbols stored in app-private SharedPreferences; no watchlist is sent to GDELT |
| Background work | Unique WorkManager task, network constraint, 15-minute minimum interval |
| Notifications | Opt-in local Android notifications; Android 13+ runtime permission checked; SecurityException handled; alerts disabled with background refresh |
| Calendar | Opens the official Forex Factory site; no scraping or calendar-feed redistribution |
| Release signing | Persistent PKCS12 private key expected from protected GitHub Actions secrets; keystore is not stored in the repository |
| CI supply chain | Third-party actions pinned to immutable commit SHAs |
| Data collection | No account, analytics SDK, advertising SDK, location collection, or API secret embedded in the APK |

## Findings addressed in the 0.4.0 work

1. **Missing calendar screen:** Implemented a provider-hosted calendar entry screen with explicit source/rights notice.
2. **Notification permission race:** Added permission checks and SecurityException handling for permission revocation between check and delivery.
3. **Refresh/alert inconsistency:** Disabling background refresh disables local background alerts.
4. **Dead calendar backend adapter:** Removed the unconfigured native calendar backend/models instead of shipping unused classes that implied a real provider integration.
5. **User-controlled relevance:** Added a local instrument watchlist and wired it into parsing, cached-feed reprocessing, and the Settings UI.
6. **Cancellation handling:** Background worker now propagates coroutine cancellation instead of converting it into a retry result.
7. **Release automation:** Added a two-stage workflow: test/debug build followed by a signed standard GitHub Release once repository signing secrets exist.
8. **No fabricated data:** No sample headlines, synthetic events, or synthetic price bars are displayed.
9. **Relevance wording:** Instrument matches are labelled as keyword matches; they are not described as market impact or trading signals.

## Actual functionality vs prepared modules

- GDELT live headline discovery is wired to the app.
- Topic filters and the user-selected instrument watchlist are wired to the headline UI.
- Background refresh and local Android headline notifications are implemented, subject to Android scheduling and permission controls.
- The Calendar screen opens the official Forex Factory website. Native calendar data is not included without written permission or an alternative provider licence.
- The OHLC market-reaction calculator is a tested standalone utility. It is not connected to a live/history price provider and is not displayed as an app feature.
- Notifications are local WorkManager notifications, not instant remote push.

## Remaining risks and release requirements

1. **No central backend:** Every device queries GDELT directly. A shared cache, central rate limiting, health monitoring, and abuse controls are recommended before scaling.
2. **Provider quality:** GDELT coverage is not exhaustive; data may be duplicated, late, inaccurate, incomplete, or irrelevant.
3. **Third-party content rights:** The app displays headlines/metadata and links to publishers, not article bodies or images.
4. **Forex Factory data rights:** Native event schedules, forecasts, actuals, impact values, or reminders must not be copied or redistributed without written permission covering public app use.
5. **No price-history provider:** Real post-event price reaction is not currently shown. A licensed data source and timestamp-quality review are needed before enabling it.
6. **No dynamic QA:** No Android device/emulator matrix, accessibility pass, network-transition testing, MobSF/dynamic analysis, or external penetration test has been performed.
7. **Signing-key custody:** The persistent PKCS12 keystore and its password must be backed up securely by the owner. Losing the private key prevents future updates signed as the same app identity. Never commit it.
8. **Repository privacy:** This repo is private at the time of the audit. It must be deliberately made public in GitHub Settings for public portfolio visibility.
9. **Privacy policy:** The included policy is a template. Replace placeholders with real operator identity, contact, legal basis, retention details, third-party disclosures, and accurate declarations before public distribution.

## Automated tests

The test suite covers:
- valid HTTPS article parsing, unsafe URL rejection, timestamp handling, cache timestamp parsing, sorting, deduplication, bounded results, and topic categorization;
- applying the selected instrument watchlist to parsed articles;
- deterministic instrument relevance, selected-watchlist behavior, unrelated headlines, and ordering;
- observed post-event OHLC returns from supplied bars and rejection of invalid/insufficient data.

A passing CI run establishes that tested source paths compile and tests pass. It does not prove every runtime path, device, Android version, network state, provider response, or attack scenario is safe.
