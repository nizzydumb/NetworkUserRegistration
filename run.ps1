$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxLib = Join-Path $projectRoot "lib\javafx-sdk-26.0.2\lib"
$javaRuntime = Join-Path $projectRoot "lib\jdk-26.0.2\bin\java.exe"
$outputDir = Join-Path $projectRoot "out\production\NetworkUserRegistration"
$quantisLib = Join-Path $projectRoot "lib\quantis"
$bouncyCastleLib = Join-Path $projectRoot "lib\bouncycastle-1.84"

& (Join-Path $projectRoot "build.ps1")
$bouncyCastleJars = @(Get-ChildItem -Path $bouncyCastleLib -Filter "*.jar" -File |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
    ForEach-Object { $_.FullName })
$runtimeClasspath = (@($outputDir) + $bouncyCastleJars) -join [IO.Path]::PathSeparator
& $javaRuntime `
    --module-path $javaFxLib `
    --add-modules javafx.controls `
    --enable-native-access=javafx.graphics,ALL-UNNAMED `
    "-Djava.library.path=$quantisLib" `
    -cp $runtimeClasspath `
    app.RegistrationApplication
