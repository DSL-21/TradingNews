# Direct Android APK release setup

Trading News is distributed as an open-source Android APK through GitHub Releases. **Google Play, a Google Play Console account and Google app-signing services are not required.** Android still requires an APK to be signed. The project uses its own locally generated PKCS12 key and verifies that every release uses the same certificate.

## Why a new signing key is safe here

The previous `v0.2.0` download was a debug APK. Debug builds use the separate package ID `com.tradingnews.app.debug`; production releases use `com.tradingnews.app`. There is no existing production APK release signed with the missing key. The first production release can therefore start with a new persistent signing identity.

Do not lose the new key after the first production release. Android will reject future updates if they are signed with a different certificate.

## 1. Install prerequisites and authenticate

On Windows, install GitHub CLI and Android Studio (which includes a Java runtime with `keytool`). Open PowerShell in your local clone of this repository and authenticate with an account that has repository Admin permission:

```powershell
gh auth login --hostname github.com --git-protocol https --web
```

## 2. Generate your new key and configure GitHub Secrets

Run the helper from the repository root:

```powershell
.\scripts\setup-release-secrets.ps1
```

The script:

1. Creates a new RSA-3072 PKCS12 signing keystore and a cryptographically random password if the two signing files do not already exist.
2. Stores the private files outside the repository in `$HOME\TradingNewsSigning\`.
3. Exports the public certificate and computes its SHA-256 fingerprint.
4. Uses GitHub CLI to configure these five repository Actions secrets without printing the key or password:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
   - `ANDROID_CERT_SHA256`

The default alias is `tradingnews-release`. The helper refuses to store signing material inside the repository and will not overwrite a partial or existing key pair. If the files already exist, it verifies and reuses them. If the script reports that only one file exists, stop and recover the matching file; do not generate a replacement over it.

## 3. Back up the signing identity

After the script succeeds, securely back up both files:

- `$HOME\TradingNewsSigning\TradingNews-release.p12`
- `$HOME\TradingNewsSigning\TradingNews-keystore-password.txt`

Keep at least two encrypted backups in separate secure locations. Never commit them, upload them to GitHub, or send them through chat or public issues. The certificate fingerprint is not a password, but the private keystore and its password are critical secrets.

## 4. Make the source repository public

The repository is currently Private. When you have reviewed the complete Git history, workflow logs, releases and repository contents for secrets or private material, change visibility in:

**GitHub → Settings → General → Danger Zone → Change repository visibility → Public**

The source code and MIT licence are public; the signing key remains in your local encrypted backups and GitHub Actions secrets. The release workflow refuses to publish while the repository is private.

## 5. Publish the first production APK

The current app version is `0.7.0` (Android versionCode 7). Once the signing secrets are configured and the repository is Public:

1. Open the repository's **Actions** tab.
2. Select **Publish Trading News GitHub Release** and click **Run workflow** on the `main` branch.
3. Enter `RELEASE` exactly in the confirmation field.
4. Enter a short release summary and run the workflow.

The workflow repeats unit tests and Android lint, builds the signed release APK, checks the package name and app version, verifies the configured signing-certificate fingerprint, calculates the APK SHA-256 checksum and publishes the APK as a versioned GitHub Release. It refuses to overwrite an existing tag.

Normal source pushes run the debug build/test workflow and upload a temporary CI artifact. They do not publish production releases automatically.

## Updating the app later

Before every future production release, increment both `versionCode` and `versionName` in `app/build.gradle.kts`. Keep using the same keystore and password file. Never regenerate the signing identity for an ordinary app update.

This is direct APK distribution, not a Google Play listing. Users install the APK themselves and may need to allow installation from their chosen browser or file manager.
