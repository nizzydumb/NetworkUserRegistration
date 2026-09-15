$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxVersion = "26.0.2"
$javaFxDirectory = Join-Path $projectRoot "lib\javafx-sdk-$javaFxVersion"
$jdkDirectory = Join-Path $projectRoot "lib\jdk-26.0.2"
$bouncyCastleDirectory = Join-Path $projectRoot "lib\bouncycastle-1.84"
$jnaDirectory = Join-Path $projectRoot "lib\jna-5.19.1"
$requiredFiles = @(
    "lib\javafx.base.jar",
    "lib\javafx.graphics.jar",
    "lib\javafx.controls.jar",
    "lib\javafx.fxml.jar",
    "lib\javafx.media.jar",
    "lib\javafx.swing.jar",
    "lib\javafx.web.jar",
    "lib\jdk.jsobject.jar",
    "src.zip",
    "bin\glass.dll",
    "bin\javafx_font.dll",
    "bin\prism_d3d.dll",
    "bin\jfxmedia.dll",
    "bin\jfxwebkit.dll"
)

$missingFiles = @($requiredFiles | Where-Object {
    -not (Test-Path (Join-Path $javaFxDirectory $_))
})
if ($missingFiles.Count -gt 0) {
    throw "The repository's offline JavaFX SDK is incomplete. Missing: $($missingFiles -join ', ')"
}

$requiredJdkFiles = @("bin\java.exe", "bin\javac.exe", "lib\modules", "release")
$missingJdkFiles = @($requiredJdkFiles | Where-Object {
    -not (Test-Path (Join-Path $jdkDirectory $_))
})
if ($missingJdkFiles.Count -gt 0) {
    throw "The repository's offline OpenJDK is incomplete. Missing: $($missingJdkFiles -join ', ')"
}

$requiredBouncyCastleFiles = @(
    "bcprov-jdk18on-1.84.jar",
    "bcutil-jdk18on-1.84.jar",
    "bcpkix-jdk18on-1.84.jar"
)
$missingBouncyCastleFiles = @($requiredBouncyCastleFiles | Where-Object {
    -not (Test-Path (Join-Path $bouncyCastleDirectory $_))
})
if ($missingBouncyCastleFiles.Count -gt 0) {
    throw "The repository's offline Bouncy Castle libraries are incomplete. Missing: $($missingBouncyCastleFiles -join ', ')"
}

$requiredJnaFiles = @("jna-5.19.1.jar", "jna-platform-5.19.1.jar")
$missingJnaFiles = @($requiredJnaFiles | Where-Object { -not (Test-Path (Join-Path $jnaDirectory $_)) })
if ($missingJnaFiles.Count -gt 0) {
    throw "The repository's offline JNA libraries are incomplete. Missing: $($missingJnaFiles -join ', ')"
}
