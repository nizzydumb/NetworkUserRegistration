$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxVersion = "26.0.2"
$javaFxDirectory = Join-Path $projectRoot "lib\javafx-runtime-$javaFxVersion"
$requiredFiles = @(
    "lib\javafx.base.jar",
    "lib\javafx.graphics.jar",
    "lib\javafx.controls.jar",
    "bin\glass.dll",
    "bin\javafx_font.dll",
    "bin\prism_d3d.dll"
)

$missingFiles = @($requiredFiles | Where-Object {
    -not (Test-Path (Join-Path $javaFxDirectory $_))
})
if ($missingFiles.Count -gt 0) {
    throw "The repository's offline JavaFX bundle is incomplete. Missing: $($missingFiles -join ', ')"
}
