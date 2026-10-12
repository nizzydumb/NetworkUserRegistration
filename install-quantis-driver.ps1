#Requires -RunAsAdministrator
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$driverDirectory = Join-Path $projectRoot "third_party\quantis-20.2.3\driver\QuantisUsb"
$infPath = Join-Path $driverDirectory "QuantisUsb.inf"
$catalogPath = Join-Path $driverDirectory "QuantisUsb.cat"

if (-not (Test-Path -LiteralPath $infPath -PathType Leaf) -or
    -not (Test-Path -LiteralPath $catalogPath -PathType Leaf)) {
    throw "The retained Quantis driver package is incomplete: $driverDirectory"
}

$catalogSignature = Get-AuthenticodeSignature -LiteralPath $catalogPath
if ($catalogSignature.Status -notin @("Valid", "Unknown")) {
    throw "QuantisUsb.cat failed signature validation: $($catalogSignature.Status)"
}

$pnputil = Join-Path $env:WINDIR "System32\pnputil.exe"
$arguments = @("/add-driver", $infPath, "/install")
& $pnputil @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Windows rejected the Quantis driver package (pnputil exit code $LASTEXITCODE)."
}

Write-Host "Quantis driver installation completed. Reconnect the USB device if Windows does not refresh it automatically."
