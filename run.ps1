$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxLib = Join-Path $projectRoot "lib\javafx-sdk-26.0.2\lib"
$javaRuntime = Join-Path $projectRoot "lib\jdk-26.0.2\bin\java.exe"
$outputDir = Join-Path $projectRoot "out\production\NetworkUserRegistration"
$quantisLib = Join-Path $projectRoot "lib\quantis"

& (Join-Path $projectRoot "build.ps1")
& $javaRuntime `
    --module-path $javaFxLib `
    --add-modules javafx.controls `
    --enable-native-access=javafx.graphics,ALL-UNNAMED `
    "-Djava.library.path=$quantisLib" `
    -cp $outputDir `
    app.RegistrationApplication
