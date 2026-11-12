# Trading News 0.2.0 — Static Security Review

**Review basis:** The Android source files currently committed directly to the `main` branch. This is a static source review plus the repository's automated parser tests, Android lint, and Gradle build. It is not a penetration test, emulator/device QA, dynamic analysis, legal opinion, or security certification.

## Controls present in the current source

| Area | Current control |
|---|---|
| Android permissions | Internet permission only; no location, contacts, camera, microphone, or notification permission |
| Transport | HTTPS GDELT endpoint; Android cleartext traffic disabled |
| External article links | URI parsing, HTTPS-only, no embedded credentials, local host/IP literals and non-default ports rejected |
| Network timeouts | 10-second connect timeout and 15-second read timeout |
| Response limits | 2 MB content-length and character-count limits |
| Redirects | Automatic HTTP redirects disabled |
| Feed structure | JSON structure is checked before a response is treated as a successful feed |
| Parser bounds | At most 250 input records are examined and 50 output articles are retained |
| Article data | Titles and source labels are length-limited; publisher label is derived from the validated URL host |
| De-duplication | Duplicate article URLs are discarded |
| Cache | Compact parsed fields only; cache is discarded if malformed, future-dated, or older than 24 hours |
| Refresh control | 10-minute cache lifetime and 60-second per-device manual refresh throttle |
| Feed state | LIVE, CACHED, OFFLINE, and CONNECTING are distinct states |
| Data handling | No account, API key, analytics SDK, or advertising SDK in this release |
| CI | Parser tests, Android lint, APK build; GitHub Actions references are pinned to immutable commit SHAs |
| Dependency updates | Dependabot configured for Gradle and GitHub Actions |

## Issues addressed during this review

1. **Invented fallback data:** Removed. No sample headlines or fabricated economic events are shown when the provider is unavailable.
2. **Unvalidated impact labels:** Removed. Keyword topic labels are for navigation only and are not market-impact ratings or trading signals.
3. **Unbounded cache age:** Cache older than 24 hours is discarded; timestamps in the future are treated as invalid.
4. **Raw response stored on device:** Cache now stores only the bounded, parsed article fields rather than the full provider response.
5. **Provider-supplied domain label:** The display source is derived from the validated HTTPS article URL host, not the free-form `domain` field.
6. **Unbounded parser work:** The parser caps records examined and results retained.
7. **Malformed preferences:** Invalid or type-corrupted local cache entries are cleared instead of being allowed to crash feed startup.
8. **Workflow supply chain:** GitHub Actions are pinned to reviewed immutable commit SHAs in CI and release workflows.

## Remaining risks and limitations

1. **No shared backend:** Each Android device queries GDELT directly. For a public audience, deploy a shared backend/cache with rate limiting, monitoring, abuse controls, and service-health alerts before scaling.
2. **Provider quality:** GDELT is a news-discovery source. Coverage is not exhaustive; headlines may be duplicated, misleading, incomplete, or late. Keyword categories do not determine factual accuracy, sentiment, causality, or expected price direction.
3. **External article content:** The app displays headline metadata and links to the publisher. It does not republish article bodies or images. GDELT attribution does not automatically grant rights to third-party publisher content.
4. **Forex Factory:** The Calendar tab opens the official website only. Do not scrape, cache, copy, or redistribute event schedules without written permission covering public app display and redistribution.
5. **No push or economic-event reminders:** They are not implemented in this version.
6. **No dynamic mobile testing:** No Android emulator/device matrix, MobSF/dynamic test, external penetration test, or professional security assessment has been performed.
7. **Release signing:** The attached APK is a debug build, not a production-signed release. A persistent private signing key must be provisioned as protected GitHub Actions secrets before building distributable production releases. Never commit a keystore or its password.
8. **Privacy and compliance:** The privacy policy in this repository is a template. The operator must complete it with correct legal identity, contact details, jurisdiction-specific disclosures, and store declarations before public distribution.

## Automated checks in CI

The test suite covers valid HTTPS article parsing, domain-label integrity, rejection of unsafe URLs, invalid timestamps, parsing of ISO cache timestamps, sorting and de-duplication, result bounds, and topic categorization. GitHub Actions runs `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`.

These checks establish that the project compiles and the tested cases pass; they do not prove that every runtime path, network state, Android version, or threat scenario is safe.
