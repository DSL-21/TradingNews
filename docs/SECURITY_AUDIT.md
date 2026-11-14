# Trading News 0.4.0 — Static Security and Reliability Review

**Review basis:** Android source, manifest, Gradle configuration, GDELT client/parser, local watchlist, WorkManager scheduling, notification path, Finance Calendar snapshot parser/repository/screen, GitHub Actions updater, release workflow and helper scripts. This is static source review plus automated unit tests, Android lint and CI compilation. It is not a penetration test, dynamic analysis, physical-device QA, legal opinion or security certification.

## Controls present

| Area | Current control |
|---|---|
| Android permissions | INTERNET and optional POST_NOTIFICATIONS; no location, contacts, camera or microphone permission |
| Notifications | Runtime permission checked on Android 13+; app-level notification state checked; SecurityException handled if permission is revoked between check and notify |
| Transport | GDELT and public calendar snapshot are HTTPS; Android cleartext traffic is disabled |
| External article links | HTTPS-only; parser rejects embedded credentials, non-default ports, local host/IP literals and invalid hosts |
| Calendar/source links | HTTPS only; Finance Calendar attribution documentation host is allow-listed; invalid publisher URLs are suppressed while the event itself can still be shown |
| Network timeouts | 10-second connect and 15-second read timeout for GDELT; bounded calendar snapshot request |
| Response limits | 2 MB GDELT response and 1 MB calendar snapshot |
| Redirects | Automatic HTTP redirects disabled |
| Parser bounds | GDELT parser examines at most 250 records and retains 50; calendar parser accepts at most 200 snapshot events |
| Publisher labels | GDELT publisher name derives from the validated article URL host, not the provider's free-form domain field |
| Deduplication | Duplicate GDELT article URLs and duplicate calendar event IDs are removed |
| News cache | Compact parsed fields only; invalid, future-dated or older-than-24-hour cache discarded |
| Calendar cache | Last valid attributed snapshot cached locally; stale state is explicit; malformed snapshots discarded |
| Refresh control | News 10-minute cache and 60-second per-device manual refresh throttle; calendar repository 1-hour cache |
| Feed states | News LIVE, CACHED, OFFLINE and CONNECTING are distinct; calendar shows snapshot timestamp/stale status/errors |
| Watchlist | User-selected symbols stored in app-private SharedPreferences; no watchlist is sent to GDELT |
| Background work | Unique WorkManager task with network constraint and 15-minute minimum interval |
| Notifications | Opt-in local Android notifications; Android 13+ runtime permission checked; alerts disabled with background refresh |
| Calendar API secret | Finance Calendar API key stored only in GitHub Actions secret FC_API_KEY and sent through X-API-Key; never embedded in APK or snapshot |
| Calendar updater | Three daily requests, each capped at 20 events/1 MB; response attribution and publisher source-rights fields retained; updater does not replace the previous snapshot if a request fails |
| Release signing | Persistent PKCS12 private key supplied through protected GitHub Actions secrets; keystore is not committed; release workflow checks the configured public repository and current calendar snapshot |
| CI supply chain | Third-party actions pinned to immutable commit SHAs |
| Data collection | No account, analytics SDK, advertising SDK, location collection or provider API secret embedded in the APK |

## Findings addressed in the 0.4.0 work

1. **Missing calendar screen:** Implemented a native event list with All/High/Medium/Low filters, snapshot status, provider attribution, original publisher links and local-time conversion for confirmed UTC times.
2. **Unconfirmed event times:** Date-only, range, estimated and unknown times remain explicitly qualified; null UTC time is never converted to midnight.
3. **Provider/source attribution:** Parser requires root Finance Calendar attribution terms/docs and retains per-event source name, URL, licence and rights metadata.
4. **Free-plan limits:** Snapshot updater uses three requests/day (about 90/month), up to 20 records per response, with the provider's truncation and paid-plan counters displayed.
5. **No invented calendar values:** Free-plan forecast/prior/actual fields remain absent; the app does not fill them with examples or estimates.
6. **API key isolation:** The calendar key is a GitHub Actions secret and is not shipped to Android clients.
7. **Automated publishing:** A scheduled GitHub workflow creates and commits the new JSON snapshot; a separate test workflow runs Python unit tests on the updater. Calendar snapshots do not trigger unnecessary Android APK rebuilds.
8. **Notification permission race:** Permission checks and SecurityException handling cover permission revocation between check and delivery.
9. **Refresh/alert consistency:** Disabling background refresh disables local background alerts.
10. **User-controlled relevance:** A local instrument watchlist is wired into parsing, cached-feed reprocessing and Settings.
11. **No fabricated data:** No demo headlines, synthetic economic events or synthetic price bars are displayed.
12. **Relevance wording:** Keyword matches are relevance hints, not market impact scores, predictions or trading signals.

## Actual functionality vs prepared modules

- GDELT live headline discovery is wired to the app.
- Topic filters and the locally stored instrument watchlist are wired to headline UI.
- Finance Calendar Free Developer schedule data is retrieved by GitHub Actions and published as an attributed JSON snapshot. The native calendar screen consumes the snapshot, filters impacts, retains source-level credit/licence metadata, and displays confirmed times in device-local time.
- Free Developer schedule data does not include forecast/prior/actual numeric figures and is limited to the provider's stated quota/record cap.
- Forex Factory remains an official external link only. The app does not scrape its calendar.
- The OHLC market-reaction calculator is a tested standalone utility. It is not connected to a licensed live/history price provider and is not displayed as a measured-impact feature.
- Notifications are local WorkManager notifications, not instant remote push.

## Setup and deployment dependencies

1. A Finance Calendar Free Developer API key must be obtained by the repository owner and stored as GitHub Actions secret `FC_API_KEY`. The API updater does not run with real events until that secret exists and the first successful schedule fetch completes.
2. The repository must be Public for the installed app to fetch the raw JSON snapshot anonymously. A private repository cannot serve the configured raw URL to arbitrary Android users without a backend or an embedded credential; embedding a GitHub token is not permitted. The signed-release workflow checks public visibility and blocks the release if the repository is Private.
3. Four Android signing secrets must be configured before the signed-release job can publish: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD.
4. The private signing key and password must be securely backed up by the owner. Do not commit them.

## Remaining risks and release requirements

1. **No central news backend:** Each Android device queries GDELT directly. For a large audience, add a shared news cache, central request limits, health monitoring and abuse controls.
2. **Provider/source rights:** Finance Calendar access does not override the licences of individual source publishers. Keep record-level source links/credits/notices and re-check publisher terms when extending caching, republishing or redistribution.
3. **Schedule completeness:** The free schedule is limited to 20 records/response with no pagination. The provider reports truncation and extra paid-plan events; the app displays these limitations rather than implying complete coverage.
4. **Price-history feed:** No licensed price-history provider is connected. Real post-event price reaction is not displayed.
5. **No dynamic QA:** No Android device/emulator matrix, accessibility pass, network-transition testing, MobSF/dynamic analysis or external penetration test has been performed.
6. **Signing-key custody:** Losing the persistent private key prevents updates signed with the same app identity. Never commit the keystore or password.
7. **Repository visibility:** The current repository is private until its owner deliberately changes it to Public. Calendar snapshots will not be publicly retrievable by an installed app until then.
8. **Privacy policy:** The included policy remains a template. Replace placeholders with real operator identity, contact, legal basis, retention details, provider disclosures and store declarations before public distribution.

## Automated tests

The test suite covers:
- valid HTTPS GDELT parsing, unsafe URL rejection, timestamp/cache handling, sorting, deduplication, bounded results and topic categories;
- user watchlist parsing and deterministic instrument relevance;
- observed post-event OHLC calculations and rejection of invalid/insufficient bars;
- Finance Calendar event parsing, source-link/rights retention, attribution validation, sorting, truncation, stale snapshots, and suppression of unsafe publisher links;
- Python updater snapshot aggregation, attribution consistency, duplicate event merging, and source-license preservation.

A passing CI run establishes that tested source paths compile and tests pass. It does not prove every runtime path, device, Android version, network state, provider response or attack scenario is safe.
