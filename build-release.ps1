$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkBin = Join-Path $projectRoot "lib\jdk-26.0.2\bin"
$javaFxRoot = Join-Path $projectRoot "lib\javafx-sdk-26.0.2"
$gccBin = Join-Path $projectRoot "lib\w64devkit-2.10.0\w64devkit\bin"
$classes = Join-Path $projectRoot "out\production\NetworkUserRegistration"
$staging = Join-Path $projectRoot "out\package-input"
$dist = Join-Path $projectRoot "dist"
$image = Join-Path $dist "NetworkUserRegistration"
$manifest = Join-Path $staging "MANIFEST.MF"

function Reset-ProjectDirectory([string]$path, [string]$expectedParent) {
    $resolvedParent = [IO.Path]::GetFullPath($expectedParent).TrimEnd('\')
    $resolvedPath = [IO.Path]::GetFullPath($path)
    if (-not $resolvedPath.StartsWith($resolvedParent + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to replace directory outside $resolvedParent`: $resolvedPath"
    }
    if (Test-Path -LiteralPath $resolvedPath) {
        Remove-Item -LiteralPath $resolvedPath -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $resolvedPath | Out-Null
}

& (Join-Path $projectRoot "build-native.ps1")
& (Join-Path $projectRoot "build-quantis.ps1")
& (Join-Path $projectRoot "build.ps1")
Reset-ProjectDirectory $staging (Join-Path $projectRoot "out")
New-Item -ItemType Directory -Force -Path $dist | Out-Null
if (Test-Path -LiteralPath $image) {
    $resolvedImage = [IO.Path]::GetFullPath($image)
    $resolvedDist = [IO.Path]::GetFullPath($dist).TrimEnd('\')
    if (-not $resolvedImage.StartsWith($resolvedDist + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to replace application image outside $resolvedDist"
    }
    Remove-Item -LiteralPath $resolvedImage -Recurse -Force
}

$runtimeJars = @(
    Get-ChildItem (Join-Path $projectRoot "lib\bouncycastle-1.84") -Filter "*.jar" -File |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" }
    Get-ChildItem (Join-Path $projectRoot "lib\jna-5.19.1") -Filter "*.jar" -File |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" }
)
$classPathNames = $runtimeJars | ForEach-Object { $_.Name }
@(
    "Manifest-Version: 1.0"
    "Main-Class: app.RegistrationApplication"
    "Class-Path: $($classPathNames -join ' ')"
    ""
) | Set-Content -LiteralPath $manifest -Encoding ascii

$applicationJar = Join-Path $staging "NetworkUserRegistration.jar"
& (Join-Path $jdkBin "jar.exe") cfm $applicationJar $manifest -C $classes .
if ($LASTEXITCODE -ne 0) { throw "JAR creation failed with exit code $LASTEXITCODE" }
$runtimeJars | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $staging -Force }
Copy-Item -LiteralPath (Join-Path $projectRoot "lib\native\rawdrive\rawdrive.dll") -Destination $staging -Force
Get-ChildItem (Join-Path $javaFxRoot "bin") -Filter "*.dll" -File |
    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $staging -Force }
$quantisDirectory = Join-Path $projectRoot "lib\quantis"
if (Test-Path -LiteralPath $quantisDirectory) {
    Get-ChildItem $quantisDirectory -Filter "*.dll" -File |
        ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $staging -Force }
}

& (Join-Path $jdkBin "jpackage.exe") `
    --type app-image `
    --name NetworkUserRegistrationApp `
    --dest $dist `
    --input $staging `
    --main-jar NetworkUserRegistration.jar `
    --main-class app.RegistrationApplication `
    --module-path (Join-Path $javaFxRoot "lib") `
    --add-modules javafx.controls `
    --java-options '--enable-native-access=javafx.graphics,ALL-UNNAMED' `
    --java-options '-Drawdrive.library.path=$APPDIR\rawdrive.dll' `
    --java-options '-Djava.library.path=$APPDIR'
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

Move-Item -LiteralPath (Join-Path $dist "NetworkUserRegistrationApp") -Destination $image
$quantisRoot = Join-Path $projectRoot "third_party\quantis-20.2.3"
$quantisDriverDestination = Join-Path $image "drivers\QuantisUsb"
New-Item -ItemType Directory -Force -Path $quantisDriverDestination | Out-Null
Copy-Item -Path (Join-Path $quantisRoot "driver\QuantisUsb\*") `
    -Destination $quantisDriverDestination -Recurse -Force
Copy-Item -LiteralPath (Join-Path $quantisRoot "LICENSE.txt") `
    -Destination (Join-Path $image "Quantis-LICENSE.txt") -Force
Copy-Item -LiteralPath (Join-Path $projectRoot "docs\QUANTIS_USB_SETUP.md") `
    -Destination (Join-Path $image "QUANTIS_USB_SETUP.md") -Force
$launcherDirectory = Join-Path $projectRoot "packaging\windows"
$resource = Join-Path $staging "elevated-launcher-resource.o"
& (Join-Path $gccBin "windres.exe") -O coff `
    -i (Join-Path $launcherDirectory "elevated-launcher.rc") `
    -o $resource "--include-dir=$launcherDirectory"
if ($LASTEXITCODE -ne 0) { throw "Launcher resource compilation failed with exit code $LASTEXITCODE" }
& (Join-Path $gccBin "gcc.exe") -std=c11 -O2 -Wall -Wextra -Werror -municode -mwindows `
    (Join-Path $launcherDirectory "elevated-launcher.c") $resource `
    -o (Join-Path $image "NetworkUserRegistration.exe") -lshell32
if ($LASTEXITCODE -ne 0) { throw "Elevated launcher compilation failed with exit code $LASTEXITCODE" }

Write-Host "Release image created at $image"
Write-Host "Start it with dist\NetworkUserRegistration\NetworkUserRegistration.exe"
