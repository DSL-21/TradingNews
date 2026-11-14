# Trading News — Data Sources

## GDELT DOC 2.0

**Purpose:** Discover recent news headlines related to financial markets, macroeconomics, energy, crypto, companies, and geopolitical events.

**Endpoint:** https://api.gdeltproject.org/api/v2/doc/doc

The Android client requests article-list mode for the previous 24 hours and asks for at most 50 records. The app displays the headline, publisher hostname, time GDELT first saw the article (seendate), and a validated HTTPS link to the original publisher.

The app does not copy or display full third-party article bodies. Category labels and instrument matches are keyword-based navigation/relevance aids only. They are not market-impact ratings, sentiment, price-direction forecasts, or trading recommendations.

Attribution and project information:
- https://www.gdeltproject.org/
- https://www.gdeltproject.org/about.html
- https://blog.gdeltproject.org/gdelt-doc-2-0-api-debuts/

The API is queried directly by each device. The client applies a 10-minute local cache, a 24-hour maximum cache age, and a 60-second per-device manual-refresh throttle. The user's instrument watchlist is stored locally and is not sent as personal data to GDELT. For a larger public audience, move aggregation behind a shared monitored backend with central rate limits and abuse controls.

## Finance Calendar — native economic-event schedule

**Purpose:** Populate the native Calendar screen with upcoming scheduled events.

**API endpoint:** https://www.financecalendar.com/wp-json/fc/v1/calendar  
**API documentation:** https://www.financecalendar.com/api/  
**API terms:** https://www.financecalendar.com/api/terms/

Trading News uses the provider's Free Developer plan. The provider's documented allowance is 100 successful JSON responses per calendar month, up to 10 successful requests per day, a maximum of 20 event records per response, and a rolling UTC window from today through six days ahead. Pagination and result figures are not included in the free schedule response.

The GitHub Actions updater uses three requests each UTC day for these country groups:
- US, GB, EU
- DE, JP, CH
- CA, AU, NZ

That is approximately 90 successful requests per 30-day month, within the published monthly and daily limits. The updater sends the API key using the `X-API-Key` request header from the GitHub Actions secret `FC_API_KEY`. The secret is never embedded in the Android APK or written into the snapshot.

The updater merges events by the provider's stable event ID and preserves the complete event records, including:
- publisher `source_name` and `source_url`,
- event `url`,
- `source_license` and `source_rights` metadata,
- schedule precision and date/time fields,
- missing values and nulls,
- the provider's root attribution and terms notice.

The native screen displays available publisher credits and source links. It shows time only when the provider supplies a confirmed instant, and explicitly labels events with unconfirmed times rather than inventing midnight. The app displays the snapshot's retrieval time and stale/truncated/paid-plan coverage notices.

**Free-plan limitation:** Forecast, prior and actual numeric figures are not part of the Free Developer schedule response. The UI does not fabricate them. A provider flag saying figures/results may be available does not mean those values are included in this free response.

The provider's current API terms describe the Free Developer plan as suitable for customer-facing use within the quota, provided that visible FinanceCalendar attribution and each original publisher's source links, credits, licences, notices and reuse terms are preserved. The operator must continue to follow record-level source rights; provider access does not override an upstream publisher's licence. Re-check the provider terms before materially changing the product, caching period, audience or distribution model.

**GitHub hosting:** The snapshot is committed to `data/economic-calendar.json` and read by the Android app from the public raw GitHub URL:
https://raw.githubusercontent.com/DSL-21/TradingNews/main/data/economic-calendar.json

This URL is publicly readable only after the repository has been deliberately changed to Public. A private repository returns no anonymous file access to an installed app. Do not put a GitHub token in the APK as a workaround.

## Forex Factory

Forex Factory remains available as a source link:
- https://www.forexfactory.com/calendar
- https://www.forexfactory.com/notices

Trading News does not scrape, download, cache, copy, or republish Forex Factory's calendar schedule or FEED database. The native schedule is from Finance Calendar under its own Free Developer terms, not from Forex Factory.

## Market price history

The OHLC reaction calculator accepts price bars supplied to it and is covered by unit tests. No licensed market-price/history provider is connected in this release. The app therefore does not show measured price reactions or synthetic prices.

## Data interpretation

- GDELT `seendate` is when GDELT first saw an article. It is not guaranteed to equal the publisher's publication time.
- GDELT search coverage is not exhaustive and can include duplicate, inaccurate, low-quality, or late-indexed sources.
- A headline/instrument keyword match does not establish causality or likely price direction.
- Calendar event times may be estimated, date-only, range-based or unconfirmed. The app must retain the provider's precision label and never silently substitute midnight.
- Users should verify important stories and source-level calendar figures on the original publisher's website.
