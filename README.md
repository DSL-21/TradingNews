# Trading News — Android 0.4.0

Trading News is an English-language, dark-mode Android application for discovering current market-related headlines, filtering them by topic and instrument, and opening the official Forex Factory economic calendar.

## App features

- **Live news:** GDELT DOC 2.0 headline discovery for the previous 24 hours, with direct links to original publishers.
- **Feed status:** visible LIVE, CACHED, OFFLINE, and CONNECTING states.
- **Topic filters:** Markets, Geopolitics, Energy, Crypto, Companies, and Other.
- **Instrument watchlist:** select supported forex pairs, metals, indices, oil, and crypto symbols in Settings. The selection is stored locally and controls which relevance matches are shown.
- **Background refresh:** optional Android WorkManager refresh on a best-effort 15-minute schedule. Android battery controls may delay it.
- **Local notifications:** optional local Android notifications for new headlines that match the selected watchlist. These are not remote push messages.
- **Economic calendar:** opens the original Forex Factory calendar in the browser. The app does not scrape, copy, cache, or redistribute its calendar data.

## What the app deliberately does not claim

- Keyword relevance is not a validated market-impact score, sentiment signal, prediction, or trading recommendation.
- A standalone OHLC reaction calculator is covered by unit tests, but no licensed price-history provider is connected; the app does not currently display measured post-event price reactions.
- Native Forex Factory event rows and reminders are not included without written redistribution permission.
- GDELT coverage is not exhaustive, and its indexed headlines may be late, duplicated, incomplete, or irrelevant.

## Data sources

- GDELT DOC 2.0: https://api.gdeltproject.org/api/v2/doc/doc
- GDELT project and attribution: https://www.gdeltproject.org/about.html
- Official Forex Factory calendar: https://www.forexfactory.com/calendar
- Forex Factory Notices: https://www.forexfactory.com/notices

The client uses HTTPS, bounded responses, URL validation, duplicate removal, a compact local cache, a 10-minute cache lifetime, a 24-hour maximum cache age, and a 60-second per-device manual-refresh throttle. Before scaling to a large public audience, a shared backend/cache with central rate controls and monitoring is recommended.

## Automated build and GitHub Release

1. Every push to main that changes Android app source or Gradle build files triggers **Build Trading News APK**.
2. The workflow runs unit tests and Android lint, then produces a debug APK artifact for testing.
3. After that successful workflow, **Publish Trading News GitHub Release** checks whether the persistent release-signing secrets are configured.
4. Once signing is configured, the release workflow reruns unit tests and lint, builds a signed release APK, verifies its certificate/package/version, and publishes a standard GitHub Release with the APK attached.
5. Each app release must increment both versionCode and versionName. Existing releases are never overwritten.

See [docs/RELEASE_SETUP.md](docs/RELEASE_SETUP.md) for the one-time signing-key and GitHub Actions setup. No ZIP source archive has to be manually uploaded for future changes.

## Build checks

CI runs:
- news parser/URL-safety tests,
- instrument-watchlist parsing tests,
- instrument relevance tests,
- observed OHLC reaction calculator tests,
- Android lint,
- debug APK compilation,
- release lint and signed release build when signing secrets are configured.

## Security, privacy, and portfolio publication

See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md) and [docs/PRIVACY_POLICY_TEMPLATE.md](docs/PRIVACY_POLICY_TEMPLATE.md). The privacy document remains a template and must be completed before public distribution.

The repository is currently private. To use it as a public portfolio project, change repository visibility to Public in GitHub Settings after reviewing the source and documentation. Do not commit the release keystore, its password, API credentials, or other secrets.

Static review and CI are not a penetration test, dynamic analysis, physical-device QA, legal opinion, or security certification.
