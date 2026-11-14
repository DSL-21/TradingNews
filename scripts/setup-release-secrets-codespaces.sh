#!/usr/bin/env bash
# Create the Trading News Android release signing identity inside GitHub Codespaces
# and store it only as GitHub Actions repository secrets.
#
# Run with: bash scripts/setup-release-secrets-codespaces.sh
# Never enable shell tracing (set -x) while handling signing material.

set -Eeuo pipefail
umask 077

readonly REPOSITORY="DSL-21/TradingNews"
readonly KEY_ALIAS="tradingnews-release"
readonly RAM_ROOT="/dev/shm"
readonly WORKDIR="${RAM_ROOT}/tradingnews-release-setup"
readonly KEYSTORE_PATH="${WORKDIR}/TradingNews-release.p12"
readonly PASSWORD_FILE="${WORKDIR}/keystore-password"
readonly CERT_PATH="${WORKDIR}/certificate.der"

SUCCESS=0
STORE_PASSWORD=""
KEYSTORE_BASE64=""
CERT_SHA256=""

cleanup() {
  local status=$?
  unset STORE_PASSWORD KEYSTORE_BASE64 CERT_SHA256 TRADINGNEWS_SIGNING_PASSWORD
  if [[ "$SUCCESS" -eq 1 && "$status" -eq 0 ]]; then
    if [[ -d "$WORKDIR" ]]; then
      rm -rf -- "$WORKDIR"
      echo
      echo "Signing secrets were saved in GitHub Actions."
      echo "Temporary keystore and password files were removed from Codespaces RAM."
    fi
    unset GH_TOKEN
  elif [[ -f "$KEYSTORE_PATH" && -f "$PASSWORD_FILE" ]]; then
    echo
    echo "SETUP DID NOT FINISH."
    echo "The matching keystore and password were retained in Codespaces RAM:"
    echo "  $WORKDIR"
    echo "Do not stop, rebuild, or delete this Codespace yet."
    echo "Rerun this same script in this Codespace to retry with the SAME signing identity."
  fi
  return "$status"
}
trap cleanup EXIT

fail() {
  echo "::error::$*" >&2
  exit 1
}

# Guard against accidentally running the cloud-only setup on a local machine.
[[ "${CODESPACES:-}" == "true" ]] || fail "This helper is intended to run only inside GitHub Codespaces."
[[ -n "${GH_TOKEN:-}" ]] || fail "GH_TOKEN is not set. Authenticate with the fine-grained token described in docs/RELEASE_SETUP.md."

for command_name in gh keytool base64 sha256sum openssl mountpoint stat; do
  command -v "$command_name" >/dev/null 2>&1 || fail "Required command is missing: $command_name"
done

mountpoint -q "$RAM_ROOT" || fail "$RAM_ROOT is not a mounted RAM filesystem."
[[ "$(stat -f -c '%T' "$RAM_ROOT")" == "tmpfs" ]] || fail "$RAM_ROOT is not tmpfs; refusing to write signing material there."

AUTH_USER="$(gh api user --jq '.login')" || fail "GitHub authentication failed."
[[ "$AUTH_USER" == "DSL-21" ]] || fail "Authenticated GitHub account is @$AUTH_USER, expected @DSL-21."

VISIBILITY="$(gh api "repos/$REPOSITORY" --jq '.visibility')" || fail "Cannot read repository metadata."
[[ "$VISIBILITY" == "public" ]] || fail "Repository must be public before publishing a public portfolio release."

# Read only secret names. GitHub never returns secret values.
EXISTING_SECRETS="$(gh secret list --app actions --repo "$REPOSITORY" --json name --jq '.[].name')" || fail "Cannot list GitHub Actions repository secrets."

EXPECTED_SECRETS=(
  "ANDROID_KEYSTORE_BASE64"
  "ANDROID_KEYSTORE_PASSWORD"
  "ANDROID_KEY_ALIAS"
  "ANDROID_KEY_PASSWORD"
  "ANDROID_CERT_SHA256"
)

existing_count=0
for name in "${EXPECTED_SECRETS[@]}"; do
  if grep -Fxq "$name" <<< "$EXISTING_SECRETS"; then
    ((existing_count += 1))
  fi
done

if [[ "$existing_count" -eq 5 && ! -e "$WORKDIR" ]]; then
  echo "All five Trading News signing secrets already exist."
  echo "No new signing key was created and nothing was changed."
  SUCCESS=1
  exit 0
fi

if [[ "$existing_count" -gt 0 && ! -e "$WORKDIR" ]]; then
  fail "Some signing secrets already exist but the matching RAM keystore is unavailable. Do not generate a replacement key. Inspect Settings > Secrets and variables > Actions first."
fi

mkdir -p -- "$WORKDIR"
chmod 700 "$WORKDIR"

has_keystore=0
has_password=0
[[ -s "$KEYSTORE_PATH" ]] && has_keystore=1
[[ -s "$PASSWORD_FILE" ]] && has_password=1

if [[ "$has_keystore" -ne "$has_password" ]]; then
  fail "Only one of the temporary keystore/password files exists. Do not delete or regenerate it automatically; inspect $WORKDIR."
fi

if [[ "$has_keystore" -eq 0 ]]; then
  echo "Generating a new RSA-3072 PKCS12 Android release signing identity in RAM..."
  openssl rand -hex 48 | tr -d '\r\n' > "$PASSWORD_FILE"
  chmod 600 "$PASSWORD_FILE"
  STORE_PASSWORD="$(cat "$PASSWORD_FILE")"
  export TRADINGNEWS_SIGNING_PASSWORD="$STORE_PASSWORD"

  if ! keytool -genkeypair \
      -alias "$KEY_ALIAS" \
      -keyalg RSA \
      -keysize 3072 \
      -validity 10000 \
      -keystore "$KEYSTORE_PATH" \
      -storetype PKCS12 \
      -storepass:env TRADINGNEWS_SIGNING_PASSWORD \
      -keypass:env TRADINGNEWS_SIGNING_PASSWORD \
      -dname "CN=Trading News Release, O=DSL-21, C=CZ" \
      -noprompt; then
    rm -f -- "$KEYSTORE_PATH" "$PASSWORD_FILE"
    fail "keytool could not create the PKCS12 keystore."
  fi
  chmod 600 "$KEYSTORE_PATH"
else
  echo "Found an unfinished setup in RAM; reusing its existing signing identity."
  STORE_PASSWORD="$(cat "$PASSWORD_FILE")"
  [[ -n "$STORE_PASSWORD" ]] || fail "The temporary keystore password file is empty."
  export TRADINGNEWS_SIGNING_PASSWORD="$STORE_PASSWORD"
fi

echo "Verifying the PKCS12 keystore and alias..."
keytool -list \
  -keystore "$KEYSTORE_PATH" \
  -storetype PKCS12 \
  -alias "$KEY_ALIAS" \
  -storepass:env TRADINGNEWS_SIGNING_PASSWORD >/dev/null

echo "Exporting the public certificate fingerprint..."
keytool -exportcert \
  -alias "$KEY_ALIAS" \
  -keystore "$KEYSTORE_PATH" \
  -storetype PKCS12 \
  -storepass:env TRADINGNEWS_SIGNING_PASSWORD \
  -file "$CERT_PATH" >/dev/null
chmod 600 "$CERT_PATH"
CERT_SHA256="$(sha256sum "$CERT_PATH" | awk '{print toupper($1)}' | sed 's/../&:/g; s/:$//')"
[[ "$CERT_SHA256" =~ ^([0-9A-F]{2}:){31}[0-9A-F]{2}$ ]] || fail "Could not calculate a valid SHA-256 certificate fingerprint."

echo "Encoding the keystore in memory for GitHub Actions..."
KEYSTORE_BASE64="$(base64 < "$KEYSTORE_PATH" | tr -d '\r\n')"
[[ -n "$KEYSTORE_BASE64" ]] || fail "Keystore encoding failed."

set_action_secret() {
  local name="$1"
  local value="$2"
  printf '%s' "$value" | gh secret set "$name" --app actions --repo "$REPOSITORY" >/dev/null
  echo "Configured Actions secret: $name"
}

# Upload the keystore last so an incomplete attempt cannot appear fully configured.
# If a network error interrupts the sequence, the same key remains in RAM for retry.
echo "Uploading signing values to GitHub Actions Secrets (values are never printed)..."
set_action_secret "ANDROID_KEYSTORE_PASSWORD" "$STORE_PASSWORD"
set_action_secret "ANDROID_KEY_ALIAS" "$KEY_ALIAS"
set_action_secret "ANDROID_KEY_PASSWORD" "$STORE_PASSWORD"
set_action_secret "ANDROID_CERT_SHA256" "$CERT_SHA256"
set_action_secret "ANDROID_KEYSTORE_BASE64" "$KEYSTORE_BASE64"

echo "Verifying that all five secret NAMES now exist..."
FINAL_SECRETS="$(gh secret list --app actions --repo "$REPOSITORY" --json name --jq '.[].name')" || fail "Could not verify the Actions secrets."
for name in "${EXPECTED_SECRETS[@]}"; do
  grep -Fxq "$name" <<< "$FINAL_SECRETS" || fail "GitHub did not confirm the expected secret name: $name"
done

echo
echo "SUCCESS: all five Android signing secrets are configured for $REPOSITORY."
echo "Key alias: $KEY_ALIAS"
echo "Certificate SHA-256: $CERT_SHA256"
echo "Secret values were not printed."
echo
echo "IMPORTANT: GitHub cannot reveal secret values again. The Actions keystore secret is now the only retained private-key copy unless you separately create an encrypted backup."
SUCCESS=1
exit 0
