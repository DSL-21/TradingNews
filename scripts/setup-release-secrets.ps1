[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$KeystorePath,

    [Parameter(Mandatory = $true)]
    [string]$PasswordFile,

    [string]$CalendarApiKeyFile,

    [string]$Repository = "DSL-21/TradingNews",

    [string]$KeyAlias = "tradingnews-release"
)

$ErrorActionPreference = "Stop"

function Assert-LastExitCode([string]$Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "GitHub CLI failed while $Action (exit code $LASTEXITCODE)."
    }
}

try {
    $gh = Get-Command gh -ErrorAction Stop
    if (-not (Test-Path -LiteralPath $KeystorePath -PathType Leaf)) {
        throw "Keystore file not found: $KeystorePath"
    }
    if (-not (Test-Path -LiteralPath $PasswordFile -PathType Leaf)) {
        throw "Password file not found: $PasswordFile"
    }
    if ($CalendarApiKeyFile -and -not (Test-Path -LiteralPath $CalendarApiKeyFile -PathType Leaf)) {
        throw "Calendar API key file not found: $CalendarApiKeyFile"
    }

    & $gh.Source auth status --hostname github.com 2>&1 | Out-Null
    Assert-LastExitCode "checking GitHub CLI authentication"

    $keystoreBytes = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $KeystorePath).Path)
    if ($keystoreBytes.Length -lt 100) {
        throw "Keystore file appears unexpectedly small."
    }

    $password = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $PasswordFile).Path).Trim()
    if ([string]::IsNullOrWhiteSpace($password)) {
        throw "Password file is empty."
    }

    $keystoreBase64 = [Convert]::ToBase64String($keystoreBytes)

    Write-Host "Setting ANDROID_KEYSTORE_BASE64..."
    $keystoreBase64 | & $gh.Source secret set ANDROID_KEYSTORE_BASE64 --repo $Repository
    Assert-LastExitCode "setting ANDROID_KEYSTORE_BASE64"

    Write-Host "Setting ANDROID_KEYSTORE_PASSWORD..."
    $password | & $gh.Source secret set ANDROID_KEYSTORE_PASSWORD --repo $Repository
    Assert-LastExitCode "setting ANDROID_KEYSTORE_PASSWORD"

    Write-Host "Setting ANDROID_KEY_ALIAS..."
    $KeyAlias | & $gh.Source secret set ANDROID_KEY_ALIAS --repo $Repository
    Assert-LastExitCode "setting ANDROID_KEY_ALIAS"

    Write-Host "Setting ANDROID_KEY_PASSWORD..."
    $password | & $gh.Source secret set ANDROID_KEY_PASSWORD --repo $Repository
    Assert-LastExitCode "setting ANDROID_KEY_PASSWORD"

    if ($CalendarApiKeyFile) {
        $calendarKey = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $CalendarApiKeyFile).Path).Trim()
        if ([string]::IsNullOrWhiteSpace($calendarKey)) {
            throw "Calendar API key file is empty."
        }
        Write-Host "Setting FC_API_KEY..."
        $calendarKey | & $gh.Source secret set FC_API_KEY --repo $Repository
        Assert-LastExitCode "setting FC_API_KEY"
        $calendarKey = $null
    }

    Write-Host ""
    Write-Host "Android release signing secrets were submitted to $Repository."
    if ($CalendarApiKeyFile) {
        Write-Host "The Finance Calendar API key was also submitted as FC_API_KEY."
    } else {
        Write-Host "FC_API_KEY was not changed. Supply -CalendarApiKeyFile after obtaining your provider key to enable calendar updates."
    }
    Write-Host "No signing password, API key, or keystore content was printed by this script."
    Write-Host "Run the 'Build Trading News APK' workflow on main to trigger the signed GitHub Release."
}
catch {
    Write-Error $_
    exit 1
}
