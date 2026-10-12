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
$applicationIcon = Join-Path $projectRoot "packaging\windows\app-icon.ico"

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

function Remove-ReleaseImage([string]$path, [string]$expectedParent) {
    $resolvedParent = [IO.Path]::GetFullPath($expectedParent).TrimEnd('\')
    $resolvedPath = [IO.Path]::GetFullPath($path)
    if (-not $resolvedPath.StartsWith($resolvedParent + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove directory outside $resolvedParent`: $resolvedPath"
    }

    # A previous app-image run can still have Java/launcher processes holding
    # runtime DLLs open. Stop only this package's known process names before
    # replacing the image; unrelated applications are not affected.
    @('NetworkUserRegistrationApp', 'NetworkUserRegistrationJava', 'NetworkUserRegistration') |
        ForEach-Object {
            Get-Process -Name $_ -ErrorAction SilentlyContinue |
                ForEach-Object {
                    Write-Host "Stopping previous $($_.ProcessName) process (PID $($_.Id))..."
                    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
                }
        }

    $remaining = @(Get-Process -Name NetworkUserRegistrationApp,NetworkUserRegistrationJava,NetworkUserRegistration -ErrorAction SilentlyContinue)
    if ($remaining.Count -gt 0) {
        $pids = ($remaining | ForEach-Object { $_.Id }) -join ', '
        throw "A previous NetworkUserRegistration process is still running (PID $pids). Close the application, or run this script elevated, then retry."
    }

    $lastError = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Remove-Item -LiteralPath $resolvedPath -Recurse -Force -ErrorAction Stop
            return
        } catch {
            $lastError = $_
            if ($attempt -lt 3) { Start-Sleep -Seconds 1 }
        }
    }
    throw "Unable to remove the previous release image. Close NetworkUserRegistration and retry. Details: $lastError"
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
    Remove-ReleaseImage $resolvedImage $resolvedDist
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
    --icon $applicationIcon `
    --module-path (Join-Path $javaFxRoot "lib") `
    --add-modules javafx.controls,java.se `
    --java-options '--enable-native-access=javafx.graphics,ALL-UNNAMED' `
    --java-options '-Drawdrive.library.path=$APPDIR\rawdrive.dll' `
    --java-options '-Djava.library.path=$APPDIR'
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

Move-Item -LiteralPath (Join-Path $dist "NetworkUserRegistrationApp") -Destination $image
# Keep the Java launcher private and reserve the image's primary launcher name
# for the administrator-aware wrapper below. This also makes jpackage-created
# installer shortcuts target the UAC-aware executable.
Rename-Item -LiteralPath (Join-Path $image "NetworkUserRegistrationApp.exe") `
    -NewName "NetworkUserRegistrationJava.exe"
Rename-Item -LiteralPath (Join-Path $image "app\NetworkUserRegistrationApp.cfg") `
    -NewName "NetworkUserRegistrationJava.cfg"
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
