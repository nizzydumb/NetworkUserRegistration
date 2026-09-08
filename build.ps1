$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxLib = Join-Path $projectRoot "lib\javafx-sdk-26.0.2\lib"
$javaCompiler = Join-Path $projectRoot "lib\jdk-26.0.2\bin\javac.exe"
$bouncyCastleLib = Join-Path $projectRoot "lib\bouncycastle-1.84"
$outputDir = Join-Path $projectRoot "out\production\NetworkUserRegistration"
$sourceRoot = Join-Path $projectRoot "src"
& (Join-Path $projectRoot "setup-dependencies.ps1")

if (-not (Test-Path $javaFxLib)) {
    throw "JavaFX dependency setup did not create $javaFxLib"
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$sourceFiles = Get-ChildItem -Path $sourceRoot -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName }
$resourceFiles = Get-ChildItem -Path $sourceRoot -Recurse -File |
    Where-Object { $_.Extension -ne ".java" }
$bouncyCastleJars = @(Get-ChildItem -Path $bouncyCastleLib -Filter "*.jar" -File |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
    ForEach-Object { $_.FullName })
$compilerArgs = @(
    "--module-path", $javaFxLib,
    "--add-modules", "javafx.controls",
    "-cp", ($bouncyCastleJars -join [IO.Path]::PathSeparator),
    "-d", $outputDir
)
$compilerArgs += $sourceFiles

& $javaCompiler @compilerArgs

foreach ($resource in $resourceFiles) {
    $relativePath = $resource.FullName.Substring($sourceRoot.Length + 1)
    $targetPath = Join-Path $outputDir $relativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $targetPath) | Out-Null
    Copy-Item -Path $resource.FullName -Destination $targetPath -Force
}
