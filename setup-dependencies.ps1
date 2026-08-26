$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxVersion = "26.0.2"
$javaFxDirectory = Join-Path $projectRoot "lib\javafx-sdk-$javaFxVersion"

if (Test-Path (Join-Path $javaFxDirectory "lib\javafx.controls.jar")) {
    return
}

$archive = Join-Path ([IO.Path]::GetTempPath()) "openjfx-${javaFxVersion}_windows-x64_bin-sdk.zip"
$downloadUrl = "https://download2.gluonhq.com/openjfx/$javaFxVersion/openjfx-${javaFxVersion}_windows-x64_bin-sdk.zip"

Write-Host "Downloading JavaFX SDK $javaFxVersion for Windows x64..."
Invoke-WebRequest -Uri $downloadUrl -OutFile $archive

$extractionDirectory = Join-Path ([IO.Path]::GetTempPath()) `
    ("network-user-registration-javafx-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $extractionDirectory | Out-Null
Expand-Archive -LiteralPath $archive -DestinationPath $extractionDirectory -Force
New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot "lib") | Out-Null
Move-Item -LiteralPath (Join-Path $extractionDirectory "javafx-sdk-$javaFxVersion") `
    -Destination $javaFxDirectory

Write-Host "JavaFX installed at $javaFxDirectory"
