$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$gcc = Join-Path $projectRoot "lib\w64devkit-2.10.0\w64devkit\bin\gcc.exe"
$source = Join-Path $projectRoot "native\rawdrive\raw_drive.c"
$outputDir = Join-Path $projectRoot "lib\native\rawdrive"
$output = Join-Path $outputDir "rawdrive.dll"

if (-not (Test-Path $gcc)) {
    throw "Bundled GCC was not found: $gcc"
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
& $gcc -std=c11 -O2 -Wall -Wextra -Werror -shared $source -o $output -ladvapi32
if ($LASTEXITCODE -ne 0) {
    throw "Native raw-drive library compilation failed with exit code $LASTEXITCODE"
}

Write-Host "Built $output"
