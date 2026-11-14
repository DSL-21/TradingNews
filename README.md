# Trading News — Android 0.4.0

Trading News is an English-language, dark-mode Android application for market-related headlines and economic-event schedules. Source, tests, scheduled data updates, APK builds and releases are managed directly in GitHub. No source ZIP upload is needed for ongoing development.

## App features

- **Live news:** GDELT DOC 2.0 headline discovery for the previous 24 hours, with direct links to original publishers.
- **Feed status:** visible LIVE, CACHED, OFFLINE and CONNECTING states.
- **Topic filters:** Markets, Geopolitics, Energy, Crypto, Companies and Other.
- **Instrument watchlist:** select supported forex pairs, metals, indices, oil and crypto symbols in Settings. The selection is stored locally and controls relevance matches.
- **Economic calendar:** native upcoming-event list powered by the Finance Calendar Free Developer schedule API, cached as an attributed JSON snapshot in this repository.
- **Calendar filters:** All, High, Medium and Low impact; event time shown in device-local time only when a confirmed UTC instant is supplied; date-only/uncertain times stay qualified.
- **Source credits:** original publisher links and source-level licence/rights metadata are preserved and shown where supplied.
- **Quota visibility:** the calendar shows actual daily/monthly Free Developer API usage from the provider response.
- **Background refresh:** optional Android WorkManager refresh on a best-effort 15-minute schedule. Android battery controls may delay it.
- **Local notifications:** optional local Android notifications for new headlines matched to the selected watchlist. These are not remote push messages.

## Economic calendar coverage

The Free Developer schedule plan is limited to 100 successful responses per month, at most 10 per day, 20 event records per response, and a rolling UTC date window from today through six days ahead. Trading News uses three country-group requests each UTC day (about 90 per 30-day month), within the published quota.

The free schedule response does not include numeric forecasts, prior values or actual release figures. The app does not fabricate or infer them. The provider's truncation and paid-plan counters are displayed so the feed is not presented as exhaustive.

The repository owner must obtain a Finance Calendar API key and configure it once as GitHub Actions secret `FC_API_KEY`. The key is sent through the `X-API-Key` header by the scheduled workflow; it is never embedded in the APK or snapshot.

- API documentation: https://www.financecalendar.com/api/
- API terms: https://www.financecalendar.com/api/terms/
- Provider: https://www.financecalendar.com/

The provider's terms require FinanceCalendar attribution and preservation of original publisher links, credits, licences and notices. The event rows retain the source fields returned by the API. Upstream publisher source terms still apply.

Forex Factory remains available as an official external calendar link only. Trading News does not scrape or redistribute its FEED calendar.

## What the app deliberately does not claim

- Keyword relevance is not a validated market-impact score, sentiment signal, prediction or trading recommendation.
- The OHLC reaction calculator has tests, but no licensed price-history provider is connected; the app does not currently display measured post-event price reactions.
- Local Android notifications are best-effort WorkManager alerts, not instant Firebase/remote push.
- GDELT coverage is not exhaustive and headlines may be late, duplicated, incomplete or irrelevant.

## Automated GitHub workflow

1. A push to main affecting Android app/Gradle source triggers **Build Trading News APK**: unit tests, Android lint and a debug APK artifact.
2. A successful build triggers **Publish Trading News GitHub Release**. A signed public Release is created only after the repository is Public, all four Android signing secrets and `FC_API_KEY` are configured, and a recent attributed calendar snapshot is present. The workflow rebuilds/verifies the signed release APK and attaches it to a standard GitHub Release.
3. A daily scheduled **Update licensed economic calendar** workflow calls the Finance Calendar API using the `FC_API_KEY` repository secret and commits an updated `data/economic-calendar.json` snapshot.
4. **Test calendar publishing pipeline** validates updater Python tests and snapshot JSON without rebuilding the Android APK for every daily data update.

One-time setup is documented in [docs/RELEASE_SETUP.md](docs/RELEASE_SETUP.md). The repository must be Public for Android installs to read the raw snapshot anonymously. It is currently private; change visibility only after reviewing the source tree and commit history to ensure no secrets have been committed.

## Build and security

CI runs GDELT/parser, instrument relevance, OHLC calculator and calendar parser tests, Python calendar updater tests, Android lint and APK compilation. See [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md) and [docs/PRIVACY_POLICY_TEMPLATE.md](docs/PRIVACY_POLICY_TEMPLATE.md).

The privacy policy still needs real operator details. CI/static review is not physical-device QA, penetration testing, legal advice or security certification. Before broad public distribution, test on real Android devices and complete the privacy policy/store declarations.
