$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkBin = Join-Path $projectRoot "lib\jdk-26.0.2\bin"
$jpackage = Join-Path $jdkBin "jpackage.exe"
$image = Join-Path $projectRoot "dist\NetworkUserRegistration"
$installerDir = Join-Path $projectRoot "dist\installer"

if (-not (Test-Path -LiteralPath $jpackage)) {
    throw "Bundled jpackage was not found. Run .\setup-dependencies.ps1 and restore Git LFS files first."
}

$candle = Get-Command candle.exe -ErrorAction SilentlyContinue
$light = Get-Command light.exe -ErrorAction SilentlyContinue
if (-not $candle -or -not $light) {
    throw "WiX Toolset (candle.exe and light.exe) is required. Install WiX 3.x and add its bin directory to PATH, then retry."
}

if (-not (Test-Path -LiteralPath $image)) {
    Write-Host "Application image not found; building it first..."
    & (Join-Path $projectRoot "build-release.ps1")
    if ($LASTEXITCODE -ne 0) { throw "Application image build failed with exit code $LASTEXITCODE" }
}

if (Test-Path -LiteralPath $installerDir) {
    Remove-Item -LiteralPath $installerDir -Recurse -Force
}
New-Item -ItemType Directory -Path $installerDir -Force | Out-Null

& $jpackage `
    --type exe `
    --app-image $image `
    --dest $installerDir `
    --name NetworkUserRegistration `
    --app-version 1.0.0 `
    --vendor "NetworkUserRegistration" `
    --win-menu `
    --win-menu-group "NetworkUserRegistration" `
    --win-shortcut `
    --win-per-user-install
if ($LASTEXITCODE -ne 0) { throw "Windows installer creation failed with exit code $LASTEXITCODE" }

Write-Host "Installer created in $installerDir"
