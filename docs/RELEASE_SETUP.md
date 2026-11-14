# One-time Android release signing and GitHub Actions setup

The app can always build a debug APK through the Build Trading News APK workflow. The separate Publish Trading News GitHub Release workflow creates a persistent-signature release APK only when all four repository secrets below are configured.

## Keep the release key private

A PKCS12 release keystore has been generated for this project. Download the keystore and its password from the project owner, keep at least two secure backups, and never upload either file to this repository.

Keystore alias: tradingnews-release  
Certificate SHA-256 fingerprint: 34:CE:7E:FB:49:06:58:EB:40:7F:A2:D3:33:59:72:A6:DC:A7:13:BF:26:B2:DC:58:E2:B8:DC:39:1B:6A:A5:39

## Configure the four repository secrets once

The easiest method is to use the supplied PowerShell helper. It uses GitHub CLI to send the secrets directly to GitHub Actions without printing their values.

1. Install GitHub CLI and sign in from PowerShell:

   ```powershell
   gh auth login --hostname github.com --git-protocol https --web
   ```

   Ensure the authenticated account has Admin access to this repository.

2. Download these two private files from the assistant response and keep them outside the repository:
   - `TradingNews-release.p12`
   - `TradingNews-keystore-password.txt`

3. Download `scripts/setup-release-secrets.ps1` from this repository, then run it from PowerShell, for example:

   ```powershell
   .\setup-release-secrets.ps1 `
     -KeystorePath "$HOME\Downloads\TradingNews-release.p12" `
     -PasswordFile "$HOME\Downloads\TradingNews-keystore-password.txt"
   ```

The script sets the four required names:
- `ANDROID_KEYSTORE_BASE64` — base64 of the PKCS12 keystore.
- `ANDROID_KEYSTORE_PASSWORD` — password from the separate text file.
- `ANDROID_KEY_ALIAS` — `tradingnews-release`.
- `ANDROID_KEY_PASSWORD` — the same password for this key.

The values are sent to GitHub with `gh secret set`; the script does not print them. It is safe to rerun if a secret needs to be refreshed. Do not commit the keystore or password file to the repository.

## What happens after setup

- Every qualifying push to main triggers the test/debug build.
- After a successful build, the release workflow checks the four secrets.
- When all secrets exist, it reruns unit tests and lint, builds the signed release APK, verifies its signature/package/version, and creates a standard GitHub Release named vMAJOR.MINOR.PATCH with the APK attached.
- If the secrets are not configured, the debug build still succeeds and the signed-release job is skipped with a notice. No source ZIP needs to be uploaded manually.
- Every future app update must increment both versionCode and versionName. A published release tag is never overwritten.

To run the pipeline after adding secrets, open Actions → Build Trading News APK → Run workflow on main. A successful run then triggers the signed release workflow automatically.

## Public portfolio visibility

The repository is currently private. To make source code and release assets visible to employers, the owner must deliberately switch it to Public under Settings → General → Danger Zone → Change repository visibility. Review the repository first and ensure no private keys or credentials have ever been committed.

## Key-loss warning

The private key is the identity used to sign this Android package. Losing it means you cannot sign future versions as an update to installs signed with this key. Keep secure backups and never generate a replacement for this release line unless you intend to publish a different application signing identity.
