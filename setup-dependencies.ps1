$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxVersion = "26.0.2"
$javaFxDirectory = Join-Path $projectRoot "lib\javafx-sdk-$javaFxVersion"
$jdkDirectory = Join-Path $projectRoot "lib\jdk-26.0.2"
$bouncyCastleDirectory = Join-Path $projectRoot "lib\bouncycastle-1.84"
$jnaDirectory = Join-Path $projectRoot "lib\jna-5.19.1"

function Assert-MaterializedFile([string]$relativePath, [long]$minimumSize) {
    $fullPath = Join-Path $projectRoot $relativePath
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw "Required dependency file is missing: $relativePath"
    }

    $file = Get-Item -LiteralPath $fullPath
    if ($file.Length -lt $minimumSize) {
        $firstLine = if ($file.Length -lt 1024) { Get-Content -LiteralPath $fullPath -TotalCount 1 } else { "" }
        if ($firstLine -eq "version https://git-lfs.github.com/spec/v1") {
            throw "Git LFS content was not downloaded for $relativePath. Run 'git lfs install' and 'git lfs pull' in the repository root."
        }
        throw "Dependency file is incomplete: $relativePath ($($file.Length) bytes)."
    }
}
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
    throw "The bundled JavaFX SDK is incomplete. Missing: $($missingFiles -join ', ')"
}

$requiredJdkFiles = @(
    "bin\java.exe",
    "bin\javac.exe",
    "bin\jar.exe",
    "bin\jpackage.exe",
    "lib\modules",
    "release"
)
$missingJdkFiles = @($requiredJdkFiles | Where-Object {
    -not (Test-Path (Join-Path $jdkDirectory $_))
})
if ($missingJdkFiles.Count -gt 0) {
    throw "The bundled OpenJDK is incomplete. Missing: $($missingJdkFiles -join ', ')"
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
    throw "The bundled Bouncy Castle libraries are incomplete. Missing: $($missingBouncyCastleFiles -join ', ')"
}

$requiredJnaFiles = @("jna-5.19.1.jar", "jna-platform-5.19.1.jar")
$missingJnaFiles = @($requiredJnaFiles | Where-Object { -not (Test-Path (Join-Path $jnaDirectory $_)) })
if ($missingJnaFiles.Count -gt 0) {
    throw "The bundled JNA libraries are incomplete. Missing: $($missingJnaFiles -join ', ')"
}

# These large files use Git LFS. Existence alone is insufficient because an
# incomplete clone contains small text pointer files at the same paths.
Assert-MaterializedFile "lib\jdk-26.0.2\lib\modules" 100000000
Assert-MaterializedFile "lib\jdk-26.0.2\lib\src.zip" 10000000
Assert-MaterializedFile "lib\javafx-sdk-26.0.2\bin\jfxwebkit.dll" 50000000

$javaExecutable = Join-Path $jdkDirectory "bin\java.exe"
$javaCompiler = Join-Path $jdkDirectory "bin\javac.exe"
function Assert-Executable([string]$path, [string]$description) {
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $path
    $startInfo.Arguments = "-version"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    [void]$process.StandardOutput.ReadToEnd()
    [void]$process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "$description could not start. Restore its Git LFS files, then try again."
    }
    $process.Dispose()
}

Assert-Executable $javaExecutable "The bundled JDK"
Assert-Executable $javaCompiler "The bundled Java compiler"

Write-Host "Dependency check passed: OpenJDK, JavaFX, Bouncy Castle, JNA, and Git LFS files are ready."
