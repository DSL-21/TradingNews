# One-time Android release and calendar setup

This repository uses GitHub Actions for ongoing builds, signed releases and daily calendar publishing. No source ZIP or APK needs to be uploaded manually for normal updates.

## 1. Private Android release signing key

A new persistent PKCS12 release keystore was created for the repository.

- File: `TradingNews-release.p12`
- Separate password file: `TradingNews-keystore-password.txt`
- Key alias: `tradingnews-release`
- Certificate SHA-256: `AF:65:0B:1F:30:4C:6C:D6:9F:42:BA:15:5E:B5:44:DF:EB:AF:A6:3F:6D:A6:1C:91:4B:78:C1:11:B0:AC:51:EA`

Download both private files from the assistant response and store them securely outside the repository. Keep at least two encrypted backups. Never upload either file to GitHub, add them to the Android app, or paste the password into chat/issues.

## 2. Get a Finance Calendar Free Developer API key

1. Open https://www.financecalendar.com/api/ and create/verify your developer account.
2. Request/activate the **Free Developer** key according to the provider's current review process.
3. Keep the key private. It must never be included in the Android source, APK, JSON snapshot, README, or Git commits.
4. Save the exact key alone in a local UTF-8 text file outside the repository, for example `$HOME\Downloads\FinanceCalendar-api-key.txt`.

Free Developer limits documented by the provider: 100 successful JSON responses/month, maximum 10/day, 20 event records/response, rolling UTC window today through +6 days, no pagination and no forecast/prior/actual values. This project uses three country-group calls/day (about 90/month). Check the provider's terms and quota before changing the schedule or query count.

Provider docs: https://www.financecalendar.com/api/  
Provider terms: https://www.financecalendar.com/api/terms/

## 3. Configure GitHub Actions secrets once

Install GitHub CLI and authenticate with an account that has Admin access to this repository:

```powershell
gh auth login --hostname github.com --git-protocol https --web
```

Download `scripts/setup-release-secrets.ps1` from this repository. From PowerShell, run:

```powershell
.\setup-release-secrets.ps1 `
  -KeystorePath "$HOME\Downloads\TradingNews-release.p12" `
  -PasswordFile "$HOME\Downloads\TradingNews-keystore-password.txt" `
  -CalendarApiKeyFile "$HOME\Downloads\FinanceCalendar-api-key.txt"
```

The script sends secret values through `gh secret set`; it does not print them. It sets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `FC_API_KEY`

You can omit `-CalendarApiKeyFile` if you want to set up signed releases before obtaining the calendar key. The calendar remains empty until `FC_API_KEY` exists and the scheduled/dispatch updater succeeds.

## 4. First signed release and first calendar snapshot

1. Open GitHub Actions → **Build Trading News APK** → Run workflow on main.
2. The workflow tests and builds the app. After a successful build, **Publish Trading News GitHub Release** checks the signing secrets, rebuilds the signed release APK and creates the standard `vMAJOR.MINOR.PATCH` GitHub Release.
3. Open Actions → **Update licensed economic calendar** → Run workflow. The first successful run publishes the real schedule snapshot.
4. Later snapshots update automatically each day at 00:20 UTC. No manual uploads or daily intervention are needed.

If the release signing secrets are missing, CI still builds a debug APK artifact but skips the signed GitHub Release. A production-style installable APK is produced only after a persistent signing key is configured.

## 5. Make the portfolio/app data publicly accessible

The repository is currently Private. The installed Android app reads the calendar snapshot from:

https://raw.githubusercontent.com/DSL-21/TradingNews/main/data/economic-calendar.json

Anonymous Android clients cannot fetch files from a private repository. To use GitHub as the public portfolio/distribution platform, the repository owner must deliberately switch it to Public in GitHub Settings → General → Danger Zone → Change repository visibility.

Before changing visibility, review all current and historical commits, actions logs, issues and releases to ensure no secrets or private material were ever committed. GitHub Actions secrets remain protected, but anything committed into Git history is not secret merely because the repository was private.

## Key-loss warning

The signing key is the application update identity. If it is lost, future updates cannot be signed as the same application identity using this key. Keep secure backups. Never commit the keystore or its password.
