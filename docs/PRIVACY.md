# Trading News — Privacy Notice

**Project maintainer:** `DSL-21` (GitHub repository maintainer)  
**Last reviewed:** 14 November 2026  
**Contact:** Open an issue in the [Trading News repository](https://github.com/DSL-21/TradingNews/issues).

This notice describes the current Android app. Trading News is an open-source news reader, not a brokerage service, trading terminal or financial account provider. It does not require an account.

## Data stored on your device

Trading News stores the following items in Android app-private storage:

- A rolling cache of up to 50 news headline records (headline, publisher hostname, original article URL, GDELT seen time).
- Your instrument watchlist.
- Your automatic-refresh and local-notification preferences.
- A timestamp used to limit repeated news-provider requests.

The app replaces the previous headline cache after a successful refresh. Cache entries older than 24 hours are discarded when the cache is next read. It does not save full article text, article images, historical price data, broker credentials, trading account information or a local news archive.

The Settings screen provides a cache-clear action and a separate full local-data reset. A full reset disables and cancels scheduled refresh, disables local alerts, clears the headline cache and app preferences, and removes Trading News notifications. Android's notification permission is controlled separately by Android. Uninstalling the app removes its app-private data; it does not erase your browser history or data held by external providers.

The storage figure in Settings is an estimate of app-owned saved values. It is not an exact measurement of Android's file-system allocation and excludes the APK, Android/WorkManager system records, SharedPreferences file overhead and your browser's cache.

## Network requests and third parties

When the news feed is opened or refreshed, the app makes an HTTPS request to the public GDELT DOC 2.0 API:

- https://api.gdeltproject.org/api/v2/doc/doc

The app sends a general market-news search query and does not send your selected instrument watchlist to GDELT. As with ordinary HTTPS requests, the provider and network infrastructure may process technical connection data such as your IP address, request time and user-agent information. See the provider's own information at https://www.gdeltproject.org/.

Opening a headline launches the original publisher's HTTPS URL in your browser. The Calendar tab opens the official Forex Factory website in your browser. Those providers and your browser operate under their own privacy policies. Trading News does not download, parse, cache, copy, rehost or republish Forex Factory calendar exports or FEED records.

The app does not include an advertising SDK, analytics SDK, account system, remote app backend or Firebase push service. It does not request location, contacts, camera or microphone permissions. Its Android manifest requests Internet access and the optional notification permission.

## Background refresh and notifications

Background refresh is disabled by default. When enabled in Settings, Android WorkManager may request the news feed periodically (no more frequently than the platform's 15-minute periodic-work minimum). Android battery, network and background-execution policies can delay or suppress it.

Background headline alerts are also disabled by default. They are generated locally by the app's WorkManager task when enabled and when Android notification permission is granted where required. They are not remote push notifications. Notification content can be visible on the lock screen according to your Android notification settings.

## Choices and contact

You can change the watchlist, disable background refresh, disable local alerts, clear the headline cache or reset all app-owned local data in Settings. Android system settings separately control notification permission.

For questions about this notice or the app's privacy behaviour, contact the project maintainer through the repository issue tracker linked above. Do not post passwords, signing keys, personal documents or other sensitive information in public issues.

## Scope

This notice describes the current open-source Android implementation. GDELT, original publishers, Forex Factory, Android, network providers and the browser may process data independently under their own terms. This project is not a law firm and this notice is not a jurisdiction-specific legal opinion. Anyone redistributing the app under a business or commercial service should review the disclosure and applicable legal obligations for their own operation.
