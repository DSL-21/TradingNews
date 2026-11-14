# Trading News — Android 0.6.0

Trading News is a simple English-language Android app for following Forex and broader market news. It combines a live headline feed, fast on-device search, topic filters, a user-configurable instrument watchlist, background refresh, and direct access to the official Forex Factory calendar.

The source, automated tests, Android builds and release workflow are managed directly in GitHub. No source ZIP upload is needed for future code changes.

## What the app does

- **Live market headlines:** GDELT DOC 2.0 searches the previous 24 hours for Forex, currency, central-bank, inflation, rates, bonds, equity, energy, gold, geopolitical and crypto coverage.
- **Search:** search currently loaded headlines by title, publisher/source hostname, symbol or relevance reason.
- **Topic filters:** All, Markets, Geopolitics, Energy, Crypto, Companies and Other.
- **Instrument filters:** filter headlines by the user's selected Forex pairs, metals, indices, oil and crypto symbols.
- **Custom watchlist:** choose the instruments that matter in Settings; the selection is stored locally on the device.
- **Original publisher links:** headlines open the source article rather than copying full article text into the app.
- **Feed status:** LIVE, CACHED, OFFLINE and CONNECTING states.
- **Privacy & Storage controls:** approximate local-data size, cache-only clearing, and a confirmed full local-data reset that disables scheduled refresh and local alerts.
- **Background refresh:** optional Android WorkManager refresh, best-effort every 15 minutes. Android battery controls can delay it.
- **Local notifications:** optional Android notifications for new headlines matching the watchlist. These are not remote push messages.
- **Forex Factory calendar:** opens the official calendar and its own weekly export links (JSON, ICS, CSV and XML). The calendar/export data stays hosted by Forex Factory.

## Privacy and local storage

The app retains at most 50 compact headline records and replaces the previous cache on refresh. Cache entries older than 24 hours are discarded when next read. The Settings screen estimates the size of cached headline metadata and saved preference values, supports clearing the news cache without changing the watchlist, and offers a confirmed full local-data reset. The estimate excludes the installed APK, Android/WorkManager system records and the external browser cache. Background refresh is off by default and must be enabled in Settings.

## How Forex Factory is handled

Forex Factory provides a weekly export directly from its own calendar page:
- Official calendar: https://www.forexfactory.com/calendar
The calendar page provides current weekly JSON, ICS, CSV and XML export links. It adds a changing `version` query parameter to the files, so the app opens the original calendar page instead of hard-coding a potentially stale export URL.

The export files are technically accessible over the internet. However, Forex Factory's notices explicitly prohibit copying, republication or redistribution of its calendar schedules and FEED database without prior written consent. Trading News therefore opens the original calendar/export URLs directly. It does not scrape, download, parse, cache or republish those events. This preserves access to the publisher's own current filters, time-zone options, impact levels and event details without creating an unofficial copy.

Official notices: https://www.forexfactory.com/notices

## News source and search behaviour

GDELT DOC 2.0 provides broad news discovery across monitored online sources. The app loads up to 50 recent headlines from the previous 24 hours. Search and topic/instrument filters are applied to the current loaded feed on the phone; they do not yet submit a separate custom query to GDELT.

Instrument matches are deterministic keyword relevance hints. They are not validated market-impact scores, sentiment analysis, causality estimates, price-direction predictions or trading recommendations.

## What is not included

- No fabricated headlines, demo events or synthetic prices.
- No copying of full third-party article bodies.
- No native Forex Factory event database without written permission.
- No licensed price-history provider or measured post-event price-reaction feature.
- No instant remote push service; notifications are local WorkManager alerts.

## Automated GitHub workflow

1. A push to main affecting Android app/Gradle source triggers **Build Trading News APK**: unit tests, Android lint and an installable debug APK artifact.
2. A successful build triggers **Publish Trading News GitHub Release**.
3. The release workflow requires the repository to be Public and the four Android signing secrets: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD.
4. Once configured, it rebuilds and verifies the signed release APK and creates a standard versioned GitHub Release.
5. Each app release increments both versionCode and versionName. Existing tags/releases are never overwritten.

See [docs/RELEASE_SETUP.md](docs/RELEASE_SETUP.md) for one-time signing setup. The repository is currently private; change its visibility only after checking commit history and confirming that no private material was committed.

## Build and security

CI runs the GDELT parser/URL-safety tests, instrument relevance tests, OHLC calculator tests, Android lint and APK compilation. See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md) and [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md).

The included privacy policy remains a template and needs real operator details before public distribution. Do not publish it as a final legal policy until every placeholder is completed and the disclosures are checked against the actual operator and target jurisdictions. CI/static review is not physical-device QA, penetration testing, legal advice or security certification.
