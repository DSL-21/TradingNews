[CmdletBinding()]
param(
    [string]$KeystorePath = (Join-Path $HOME "TradingNewsSigning\TradingNews-release.p12"),
    [string]$PasswordFile = (Join-Path $HOME "TradingNewsSigning\TradingNews-keystore-password.txt"),
    [string]$Repository = "DSL-21/TradingNews",
    [string]$KeyAlias = "tradingnews-release"
)

$ErrorActionPreference = "Stop"
$env:TRADINGNEWS_SIGNING_PASSWORD = $null

function Assert-LastExitCode([string]$Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed while $Action (exit code $LASTEXITCODE)."
    }
}

function Set-RepositorySecret([string]$Name, [string]$Value, $GhCommand) {
    Write-Host "Configuring GitHub secret $Name..."
    $Value | & $GhCommand.Source secret set $Name --repo $Repository
    Assert-LastExitCode "setting $Name"
}

try {
    $gh = Get-Command gh -ErrorAction Stop
    $keytoolCommand = Get-Command keytool -ErrorAction SilentlyContinue
    $keytoolPath = $null

    if ($keytoolCommand) {
        $keytoolPath = $keytoolCommand.Source
    } else {
        $candidates = @()
        if ($env:JAVA_HOME) {
            $candidates += (Join-Path $env:JAVA_HOME "bin\keytool.exe")
        }
        if ($env:ProgramFiles) {
            $candidates += (Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe")
        }
        $keytoolPath = $candidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    }

    if (-not $keytoolPath) {
        throw "keytool was not found. Install Android Studio or a JDK 17+, then run this script again."
    }

    $keyDirectory = Split-Path -Parent ([System.IO.Path]::GetFullPath($KeystorePath))
    $passwordDirectory = Split-Path -Parent ([System.IO.Path]::GetFullPath($PasswordFile))
    $repoRoot = $null
    $gitCommand = Get-Command git -ErrorAction SilentlyContinue
    if ($gitCommand) {
        $repoRootOutput = & $gitCommand.Source rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $repoRootOutput) {
            $repoRoot = [System.IO.Path]::GetFullPath(($repoRootOutput | Select-Object -First 1))
        }
    }
    if ($repoRoot) {
        $repoPrefix = $repoRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        foreach ($privatePath in @(
            [System.IO.Path]::GetFullPath($KeystorePath),
            [System.IO.Path]::GetFullPath($PasswordFile)
        )) {
            if ($privatePath.Equals($repoRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
                $privatePath.StartsWith($repoPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                throw "Signing material must be stored outside the repository. Use the default TradingNewsSigning folder under your home directory."
            }
        }
    }

    foreach ($directory in @($keyDirectory, $passwordDirectory) | Select-Object -Unique) {
        if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
            New-Item -ItemType Directory -Path $directory -Force | Out-Null
        }
    }

    $hasKeystore = Test-Path -LiteralPath $KeystorePath -PathType Leaf
    $hasPassword = Test-Path -LiteralPath $PasswordFile -PathType Leaf
    if ($hasKeystore -xor $hasPassword) {
        throw "Only one signing file exists. Do not overwrite or replace it automatically; restore the matching pair or choose a new empty path deliberately."
    }

    & $gh.Source auth status --hostname github.com 2>&1 | Out-Null
    Assert-LastExitCode "checking GitHub CLI authentication"

    if (-not $hasKeystore) {
        $randomBytes = New-Object byte[] 48
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($randomBytes) } finally { $rng.Dispose() }
        $password = [Convert]::ToBase64String($randomBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
        [Array]::Clear($randomBytes, 0, $randomBytes.Length)
        [System.IO.File]::WriteAllText($PasswordFile, $password, [System.Text.Encoding]::ASCII)

        $env:TRADINGNEWS_SIGNING_PASSWORD = $password
        Write-Host "Generating a new persistent Android signing identity outside the repository..."
        $generateArgs = @(
            "-genkeypair", "-alias", $KeyAlias, "-keyalg", "RSA", "-keysize", "3072",
            "-validity", "10000", "-keystore", $KeystorePath, "-storetype", "PKCS12",
            "-storepass:env", "TRADINGNEWS_SIGNING_PASSWORD",
            "-keypass:env", "TRADINGNEWS_SIGNING_PASSWORD",
            "-dname", "CN=Trading News Release, O=DSL-21, C=CZ", "-noprompt"
        )
        & $keytoolPath @generateArgs
        if ($LASTEXITCODE -ne 0) {
            Remove-Item -LiteralPath $KeystorePath -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath $PasswordFile -Force -ErrorAction SilentlyContinue
            Assert-LastExitCode "generating the Android release keystore"
        }
    } else {
        $password = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $PasswordFile).Path).Trim()
        if ([string]::IsNullOrWhiteSpace($password)) {
            throw "The signing password file is empty."
        }
        $env:TRADINGNEWS_SIGNING_PASSWORD = $password
    }

    $verifyArgs = @("-list", "-keystore", $KeystorePath, "-storetype", "PKCS12", "-alias", $KeyAlias, "-storepass:env", "TRADINGNEWS_SIGNING_PASSWORD")
    & $keytoolPath @verifyArgs | Out-Null
    Assert-LastExitCode "verifying the PKCS12 keystore and alias"

    $certificatePath = Join-Path $env:TEMP ("trading-news-cert-" + [Guid]::NewGuid().ToString("N") + ".der")
    try {
        $exportArgs = @("-exportcert", "-alias", $KeyAlias, "-keystore", $KeystorePath, "-storetype", "PKCS12", "-storepass:env", "TRADINGNEWS_SIGNING_PASSWORD", "-file", $certificatePath)
        & $keytoolPath @exportArgs | Out-Null
        Assert-LastExitCode "exporting the public signing certificate"

        $certificateBytes = [System.IO.File]::ReadAllBytes($certificatePath)
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        try {
            $fingerprint = [BitConverter]::ToString($sha256.ComputeHash($certificateBytes)).Replace('-', ':')
        } finally {
            $sha256.Dispose()
            [Array]::Clear($certificateBytes, 0, $certificateBytes.Length)
        }
    } finally {
        if (Test-Path -LiteralPath $certificatePath) {
            Remove-Item -LiteralPath $certificatePath -Force
        }
    }

    $keystoreBytes = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $KeystorePath).Path)
    if ($keystoreBytes.Length -lt 100) {
        throw "Keystore file appears unexpectedly small."
    }
    $keystoreBase64 = [Convert]::ToBase64String($keystoreBytes)
    [Array]::Clear($keystoreBytes, 0, $keystoreBytes.Length)

    Set-RepositorySecret "ANDROID_KEYSTORE_BASE64" $keystoreBase64 $gh
    Set-RepositorySecret "ANDROID_KEYSTORE_PASSWORD" $password $gh
    Set-RepositorySecret "ANDROID_KEY_ALIAS" $KeyAlias $gh
    Set-RepositorySecret "ANDROID_KEY_PASSWORD" $password $gh
    Set-RepositorySecret "ANDROID_CERT_SHA256" $fingerprint $gh

    Write-Host ""
    Write-Host "All five Android signing secrets were configured for $Repository."
    Write-Host "Certificate SHA-256: $fingerprint"
    Write-Host "Keystore: $KeystorePath"
    Write-Host "Password backup: $PasswordFile"
    Write-Host "The signing password and keystore contents were not printed."
    Write-Host ""
    Write-Host "IMPORTANT: Back up both files in at least two secure, encrypted locations."
    Write-Host "Never commit either file, upload it to a public issue, or send it in chat."
    Write-Host "Next: make the repository Public after your history review, then run the manual Publish Trading News GitHub Release workflow."
}
catch {
    Write-Error $_
    exit 1
}
finally {
    $env:TRADINGNEWS_SIGNING_PASSWORD = $null
}
