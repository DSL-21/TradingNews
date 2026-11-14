# Direct Android APK release setup

Trading News is distributed as an open-source Android APK through GitHub Releases. **Google Play, a Google Play Console account and Google app-signing services are not required.** Android still requires an APK to be signed. Production releases use a persistent PKCS12 signing identity and the release workflow verifies its certificate fingerprint.

## Why a new signing key is safe here

The previous `v0.2.0` download was a debug APK. Debug builds use the separate package ID `com.tradingnews.app.debug`; production releases use `com.tradingnews.app`. There is no existing production APK release signed with the missing key. The first production release can therefore start with a new signing identity.

Do not lose the new signing identity after the first production release. Android will reject future updates if they are signed with a different certificate.

## 1. Recommended: create the signing identity in GitHub Codespaces

This is the recommended setup when you do not want the private keystore saved on your personal computer. The helper creates the PKCS12 keystore and password under `/dev/shm` (a RAM-backed filesystem) inside the running Codespace, then sends the five values to **GitHub Actions repository secrets**. It does not save the keystore to the repository or your PC.

### Create a narrowly scoped GitHub token

Create a fine-grained personal access token at <https://github.com/settings/personal-access-tokens/new> with:

- Resource owner: `DSL-21`
- Repository access: only `DSL-21/TradingNews`
- Repository permission: `Secrets` = **Read and write**
- Required metadata permission: **Read-only**
- Account permissions: none
- A short expiration, such as 7 days

Do not select Agent secrets, Codespaces secrets, or Dependabot secrets. The release workflow reads **Actions secrets**, not Codespaces secrets.

Generate the token and keep it private. Never paste it into chat, a source file, a terminal command argument, or a commit.

### Pass the token to the Codespaces terminal without displaying it

Open the Codespace for this repository. In its terminal, run:

```bash
read -r -s -p "Paste GitHub token (hidden): " GH_TOKEN
printf '\\n'
export GH_TOKEN
```

Paste the token at the prompt and press Enter. The characters will not appear. Check that it is loaded without printing it:

```bash
if [ -n "$GH_TOKEN" ]; then echo "Token is loaded"; else echo "Token is empty"; fi
```

You can verify the token and repository secret permission without exposing its value:

```bash
gh api user --jq '"Authenticated as: @\\(.login)"' &&
gh api repos/DSL-21/TradingNews/actions/secrets --jq '"Actions Secrets access OK. Existing secrets: \\(.total_count)"'
```

The authenticated account must be `DSL-21`. If a command returns an authorization error, stop and correct the token permission before continuing.

### Pull and run the Codespaces-only helper

The helper is committed at `scripts/setup-release-secrets-codespaces.sh`. Update the Codespaces working tree, then run it:

```bash
git pull --ff-only origin main
bash scripts/setup-release-secrets-codespaces.sh
```

The helper checks that it is running in Codespaces, validates that `/dev/shm` is actually RAM-backed, checks the GitHub identity and repository visibility, creates or reuses the same temporary keystore during retries, and configures these **GitHub Actions** repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `ANDROID_CERT_SHA256`

The alias is `tradingnews-release`. The helper does not print secret values. It verifies that all five secret names exist after upload, then removes the temporary keystore and password files from Codespaces RAM and unsets the token from the current shell.

**Important during setup:** do not stop, rebuild, or delete the Codespace until the script reports success. If an upload fails partway through, the helper retains the matching keystore and password in that running Codespace's RAM so that you can rerun the same script and finish with the same signing identity. Do not manually generate a second key or delete the temporary directory during a partial setup.

### Critical backup limitation

GitHub does not reveal Actions secret values after they are saved. In this cloud-only setup, once the helper removes the RAM copy, the GitHub Actions keystore secret is the only retained private-key copy unless you separately make an encrypted backup in a secure vault. If the keystore secret is deleted or becomes unavailable and there is no backup, the same signing identity cannot be recovered; future updates signed with a new key will not update existing installations. This is a deliberate trade-off for not keeping the private key on your PC.

The seven-day token expiration does not expire or disable the Actions secrets. Once the setup succeeds, you can revoke the token earlier from GitHub token settings; the release workflow does not need that personal token for future releases.

## 2. Alternative: generate the key on Windows

Use this option only if you prefer to keep an encrypted local backup of the keystore. Install GitHub CLI and Android Studio (which includes a Java runtime with `keytool`). Open PowerShell in a local clone of this repository and authenticate with an account that has repository Admin permission:

```powershell
gh auth login --hostname github.com --git-protocol https --web
```

Run the helper from the repository root:

```powershell
.\\scripts\\setup-release-secrets.ps1
```

It creates an RSA-3072 PKCS12 signing keystore and random password under `$HOME\\TradingNewsSigning\\`, exports the public certificate fingerprint, and configures the same five GitHub Actions secrets listed above. It refuses to store signing material inside the repository and will not overwrite a partial or existing key pair.

If using this option, securely back up both `TradingNews-release.p12` and `TradingNews-keystore-password.txt` in at least two encrypted locations. Never commit either file, upload it to a public issue, or send it through chat.

## 3. Repository and obsolete calendar credentials

The repository is public. The release workflow checks that visibility before publishing.

Earlier project versions optionally used a separate Finance Calendar API key. The current app no longer calls that service or reads a calendar snapshot. The current app needs no Finance Calendar or Forex Factory API key. Remove an obsolete `FC_API_KEY` Actions secret if one still exists.

## 4. Publish the first production APK

The current app version is `0.7.1` (Android versionCode 8). Once the five signing secrets are configured:

1. Open the repository's **Actions** tab.
2. Select **Publish Trading News GitHub Release** and click **Run workflow** on the `main` branch.
3. Enter `RELEASE` exactly in the confirmation field.
4. Enter a short release summary and run the workflow.

The workflow repeats unit tests and Android lint, builds the signed release APK, checks the package name and app version, verifies the configured signing-certificate fingerprint, calculates the APK SHA-256 checksum and publishes the APK as a versioned GitHub Release. It refuses to overwrite an existing tag.

Normal source pushes run the debug build/test workflow and upload a temporary CI artifact. They do not publish production releases automatically.

## Updating the app later

Before every future production release, increment both `versionCode` and `versionName` in `app/build.gradle.kts`. Keep using the same signing identity. Never regenerate it for an ordinary app update.

This is direct APK distribution, not a Google Play listing. Users install the APK themselves and may need to allow installation from their chosen browser or file manager.
