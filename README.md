# Network User Registration

## Requirements

- Windows x64
- JDK 26 available on `PATH`
- PowerShell

JavaFX 26.0.2 is downloaded from Gluon automatically on the first build and cached under `lib`. Downloaded JavaFX
files, IntelliJ settings, build output, logs, and proprietary Quantis binaries are intentionally excluded from Git.

## Build and run

```powershell
.\build.ps1
.\run.ps1
```

The first build requires internet access. Later builds use the local JavaFX cache.

## Quantis

The direct Java JNI declarations are included in the source tree. Install the official Quantis driver and place
`Quantis.dll` plus its native dependencies under `lib\quantis`. See `lib\quantis\README.md` for configuration and
device detection examples. Vendor binaries are not stored in Git.

## NIST SP 800-22

The retained NIST STS 2.1.2 reference source and Java port are described in
`docs\NIST_SP_800_22_JAVA_PORT.md`.
