# Network User Registration

## Requirements

- Windows x64
- JDK 26 available on `PATH`
- PowerShell

The repository contains a reduced Windows x64 JavaFX 26.0.2 runtime with the `base`, `graphics`, and `controls`
modules required by this application. No network connection is required to build or run it. Full JavaFX SDK
downloads, IntelliJ settings, build output, logs, and proprietary Quantis binaries are excluded from Git.

## Build and run

```powershell
.\build.ps1
.\run.ps1
```

Both commands work offline after cloning or unpacking the repository, provided JDK 26 is installed locally.

## Quantis

The direct Java JNI declarations are included in the source tree. Install the official Quantis driver and place
`Quantis.dll` plus its native dependencies under `lib\quantis`. See `lib\quantis\README.md` for configuration and
device detection examples. Vendor binaries are not stored in Git.

## NIST SP 800-22

The retained NIST STS 2.1.2 reference source and Java port are described in
`docs\NIST_SP_800_22_JAVA_PORT.md`.
