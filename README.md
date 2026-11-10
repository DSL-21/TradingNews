# Trading News

Trading News Android MVP (v0.1.0), dark theme, Kotlin + Jetpack Compose.

## Build the APK with GitHub Actions

1. Upload `TradingNews-v0.1.0-GitHub.zip` from the ChatGPT-provided project bundle to the repository root.
2. Open the **Actions** tab.
3. Select **Build Trading News APK**. A push containing the ZIP automatically starts the workflow; alternatively use **Run workflow**.
4. When the run succeeds, open it and download the `TradingNews-debug-apk` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android device.

The workflow extracts the source project, installs JDK 17 / Android SDK 36, builds `:app:assembleDebug`, and publishes the APK as a 14-day artifact.

## Important data-provider notes

- The app includes a GDELT-based breaking-news backend connector.
- The economic-calendar adapter is intentionally disabled until the operator has the appropriate data-display / redistribution rights.
- Forex Factory is not scraped or consumed through an undocumented export in this public-facing starter. Secure written permission before using its data in a publicly distributed app.
- Never embed API keys in the APK or commit them to this repository.

## Status

The GitHub Actions workflow is committed. The source ZIP still needs to be uploaded to the repository root before the first build can complete. The first CI run is the Android compilation check; fix any build errors reported by Actions before distributing the APK.
