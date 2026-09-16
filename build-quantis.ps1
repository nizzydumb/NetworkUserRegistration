$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceDir = Join-Path $projectRoot "third_party\quantis-20.2.3\native"
$buildDir = Join-Path $projectRoot "out\native\quantis"
$destinationDir = Join-Path $projectRoot "lib\quantis"
$toolchainDir = Join-Path $projectRoot "lib\w64devkit-2.10.0\w64devkit\bin"
$jdkInclude = Join-Path $projectRoot "lib\jdk-26.0.2\include"
$jdkWindowsInclude = Join-Path $jdkInclude "win32"
$gcc = Join-Path $toolchainDir "gcc.exe"
$gxx = Join-Path $toolchainDir "g++.exe"

foreach ($requiredPath in @($sourceDir, $gcc, $gxx, $jdkInclude, $jdkWindowsInclude)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required Quantis build dependency is missing: $requiredPath"
    }
}

New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
New-Item -ItemType Directory -Path $destinationDir -Force | Out-Null

$commonArguments = @(
    "-O2",
    "-DUNICODE",
    "-D_UNICODE",
    "-DDISABLE_QUANTIS_PCI",
    "-DDISABLE_QUANTIS_PCIE",
    "-iquote", $sourceDir
)

$cSources = @("DllMain.c", "Conversion.c", "Quantis_C.c")
$cppSources = @("QuantisUsb_Windows.cpp", "Quantis_Java.cpp")
$objects = [System.Collections.Generic.List[string]]::new()

foreach ($sourceName in $cSources) {
    $sourcePath = Join-Path $sourceDir $sourceName
    $objectPath = Join-Path $buildDir (($sourceName -replace '\.c$', '') + ".o")
    & $gcc @commonArguments -c $sourcePath -o $objectPath
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile $sourceName"
    }
    $objects.Add($objectPath)
}

foreach ($sourceName in $cppSources) {
    $sourcePath = Join-Path $sourceDir $sourceName
    $objectPath = Join-Path $buildDir (($sourceName -replace '\.cpp$', '') + ".o")
    $arguments = [System.Collections.Generic.List[string]]::new()
    $arguments.AddRange([string[]]$commonArguments)
    if ($sourceName -eq "Quantis_Java.cpp") {
        $arguments.AddRange([string[]]@("-I", $jdkInclude, "-I", $jdkWindowsInclude))
    }
    & $gxx @arguments -c $sourcePath -o $objectPath
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to compile $sourceName"
    }
    $objects.Add($objectPath)
}

$destination = Join-Path $destinationDir "Quantis.dll"
& $gxx -shared @objects -lsetupapi -static-libgcc -static-libstdc++ -o $destination
if ($LASTEXITCODE -ne 0) {
    throw "Failed to link Quantis.dll"
}

Write-Host "Built Windows x64 Quantis JNI library: $destination"
