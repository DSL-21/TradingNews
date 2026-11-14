# One-time Android release setup

Trading News uses GitHub Actions for Android builds and signed GitHub Releases. No source ZIP or APK needs to be uploaded manually for normal code updates.

## 1. Private Android release signing key

A persistent PKCS12 release keystore has been generated for the repository.

- File: `TradingNews-release.p12`
- Separate password file: `TradingNews-keystore-password.txt`
- Key alias: `tradingnews-release`
- Certificate SHA-256: `AF:65:0B:1F:30:4C:6C:D6:9F:42:BA:15:5E:B5:44:DF:EB:AF:A6:3F:6D:A6:1C:91:4B:78:C1:11:B0:AC:51:EA`

Download both private files from the assistant response and store them securely outside the repository. Keep at least two encrypted backups. Never upload either file to GitHub, add them to the Android app or paste the password into chat/issues.

## 2. Configure GitHub Actions secrets once

Install GitHub CLI and authenticate with an account that has Admin access to this repository:

```powershell
gh auth login --hostname github.com --git-protocol https --web
```

Download `scripts/setup-release-secrets.ps1` from this repository. From PowerShell, run:

```powershell
.\setup-release-secrets.ps1 `
  -KeystorePath "$HOME\Downloads\TradingNews-release.p12" `
  -PasswordFile "$HOME\Downloads\TradingNews-keystore-password.txt"
```

The script sends the values through `gh secret set`; it does not print them. It configures these four repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

No news/calendar provider API key is required for the current app configuration.

## 3. Make the GitHub portfolio public

The repository is currently Private. To use GitHub as a public portfolio and let employers inspect source/releases, the owner must deliberately switch it to Public under GitHub Settings → General → Danger Zone → Change repository visibility.

Before changing visibility, review current and historical commits, Actions logs, issues and releases to ensure no secrets or private material were ever committed. GitHub Actions secrets remain protected, but anything committed into Git history is not secret merely because the repository was private.

The signed-release workflow checks that the repository is Public before publishing a public GitHub Release.

## 4. First signed release

1. Configure the four signing secrets using the helper above.
2. Change repository visibility to Public after reviewing its history.
3. Open GitHub Actions → **Build Trading News APK** → Run workflow on main.
4. The workflow runs unit tests and Android lint, builds a debug artifact, then triggers **Publish Trading News GitHub Release**.
5. The release workflow rebuilds the production release APK, verifies the persistent signing certificate, package name and version, and creates a standard GitHub Release with the APK attached.

Future changes are committed directly to GitHub. The build workflow runs automatically, and after the four secrets are configured the release workflow creates the new versioned GitHub Release. No ZIP/APK upload is needed.

## Key-loss warning

The signing key is the application update identity. If it is lost, future updates cannot be signed as the same application identity using this key. Keep secure encrypted backups. Never commit the keystore or its password.
