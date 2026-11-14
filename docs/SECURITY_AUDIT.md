# Trading News 0.5.0 — Static Security and Reliability Review

**Review basis:** Android source, manifest, Gradle configuration, GDELT client/parser, local headline search, topic/instrument filters, local watchlist, WorkManager scheduling, notification path, official Forex Factory link/export screen, GitHub Actions build and release workflows. This is static source review plus automated unit tests, Android lint and CI compilation. It is not a penetration test, dynamic analysis, physical-device QA, legal opinion or security certification.

## Controls present

| Area | Current control |
|---|---|
| Android permissions | INTERNET and optional POST_NOTIFICATIONS; no location, contacts, camera or microphone permission |
| Notifications | Runtime permission checked on Android 13+; SecurityException handled if permission is revoked between check and delivery |
| Transport | GDELT and official calendar/export links use HTTPS; Android cleartext traffic is disabled |
| External article links | HTTPS-only; parser rejects embedded credentials, non-default ports, local host/IP literals and invalid hosts |
| Calendar/export links | Calendar screen allow-lists official Forex Factory and nfs.faireconomy.media HTTPS hosts; links open at the original provider |
| Network timeouts | 10-second connect and 15-second read timeout for GDELT |
| Response limits | 2 MB GDELT response; bounded parser work |
| Redirects | Automatic HTTP redirects disabled for GDELT |
| Parser bounds | GDELT parser examines at most 250 input records and retains at most 50 valid articles |
| Publisher labels | GDELT publisher name derives from the validated article URL host |
| Deduplication | Duplicate GDELT article URLs discarded |
| News cache | Compact parsed fields only; invalid, future-dated or older-than-24-hour cache discarded |
| Search | On-device query matching over loaded headline titles, publisher hosts, symbol matches and relevance reasons |
| Refresh control | 10-minute local cache and 60-second per-device manual refresh throttle |
| Feed states | LIVE, CACHED, OFFLINE and CONNECTING are distinct |
| Watchlist | User-selected symbols stored in app-private SharedPreferences; not sent as personal data to GDELT |
| Background work | Unique WorkManager task with network constraint and 15-minute minimum interval |
| Notifications | Opt-in local Android notifications; Android 13+ runtime permission checked; alerts disabled with background refresh |
| Forex Factory data | Official calendar and export links are opened directly; no export fetch, parser, local copy, cache or rehosting exists in the app |
| Release signing | Persistent PKCS12 private key supplied through protected GitHub Actions secrets; keystore is not committed |
| CI supply chain | Third-party Actions pinned to immutable commit SHAs |
| Data collection | No account, analytics SDK, advertising SDK or location collection |

## 0.5.0 product changes

1. **Simplified economic calendar:** Removed the third-party Finance Calendar API, native snapshot parser, JSON cache, scheduled updater and related CI workflow.
2. **Official Forex Factory:** Calendar screen opens the official Forex Factory calendar and source-hosted weekly JSON/ICS/CSV exports. It does not copy, scrape, download, parse or republish FEED data.
3. **Headline search:** Added on-device search by headline title, publisher hostname, matched symbol and relevance reason.
4. **Instrument filters:** Added instrument chips to the Headlines screen, using the user's locally selected watchlist.
5. **Topic filters:** Retained Markets, Geopolitics, Energy, Crypto, Companies and Other categories.
6. **Versioning:** Incremented Android versionCode and versionName to 5 / 0.5.0.
7. **Release automation:** Simplified signed-release prerequisites to the four Android signing secrets and public repository visibility.

## Actual functionality vs prepared modules

- GDELT live headline discovery is wired to the app.
- Client-side headline search and category/instrument filters are wired to the Headlines UI.
- The local instrument watchlist is stored on the device and controls relevance matches and available instrument filters.
- Forex Factory calendar and weekly export links open at the original provider; export content is not imported into the app.
- The OHLC market-reaction calculator is a tested standalone utility. It is not connected to a licensed price-history provider and is not displayed as a measured-impact feature.
- Background headline refresh and local Android notifications are best-effort WorkManager features, not instant remote push.

## Known limitations and remaining work

1. **Limited loaded search set:** The search field filters only the latest loaded feed of up to 50 headlines. It does not issue a new query to search the entire GDELT index.
2. **No central news backend:** Each Android device queries GDELT directly. For a large audience, consider a shared cache, central request controls and monitoring.
3. **Provider quality:** GDELT coverage is not exhaustive; data may be duplicated, late, inaccurate, incomplete or irrelevant.
4. **Calendar integration rights:** The app links to Forex Factory's original calendar/export. Native copying or redistribution requires prior written permission from Fair Economy, Inc.
5. **No price-history provider:** Real post-event price reaction is not currently shown.
6. **No dynamic QA:** No Android device/emulator matrix, accessibility pass, network-transition testing, MobSF/dynamic analysis or external penetration test has been performed.
7. **Signing-key custody:** Losing the persistent private key prevents updates signed with the same app identity. Never commit the keystore or password.
8. **Repository visibility:** The repository remains Private until its owner deliberately changes it to Public. Public GitHub Releases and the portfolio are not visible to employers while it remains private.
9. **Privacy policy:** The included policy remains a template. Replace placeholders with real operator identity, contact, legal basis, retention details, provider disclosures and store declarations before public distribution.

## Automated tests

The test suite covers:
- valid GDELT article parsing, unsafe URL rejection, timestamp/cache handling, sorting, deduplication, bounded results and topic categories;
- user watchlist parsing and deterministic instrument relevance;
- observed post-event OHLC calculations and rejection of invalid/insufficient bars.

A passing CI run establishes that tested source paths compile and tests pass. It does not prove every runtime path, device, Android version, network state, provider response or attack scenario is safe.
