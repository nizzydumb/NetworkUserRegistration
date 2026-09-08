# Network User Registration

## Requirements

- Windows x64
- IntelliJ IDEA with Java 26 support
- Git LFS when cloning from GitHub
- PowerShell only if using the optional command-line scripts

The repository contains both the complete Windows x64 OpenJDK 26.0.2 distribution and the complete extracted
Windows x64 JavaFX 26.0.2 SDK. No network connection or system Java installation is required to compile and run
the application. User-specific IntelliJ settings, build output, logs, redundant download archives, and proprietary
Quantis binaries are excluded from Git.

## Open and run directly in IntelliJ IDEA

1. Clone with Git LFS enabled so the large JDK and JavaFX files are materialized, then open the repository root in
   IntelliJ IDEA. If the repository was delivered as a complete archive instead, simply extract and open it.
2. Open **File | Project Structure | Platform Settings | SDKs**.
3. Select **Add SDK | JDK** and choose `lib\jdk-26.0.2` inside this project. This is a one-time IntelliJ registration;
   IntelliJ deliberately stores installed SDK paths outside the Git repository.
4. Name the SDK `openjdk-26.0.2-project`. The committed project configuration already references that exact name.
5. Under **Project Settings | Project**, confirm that Project SDK is `openjdk-26.0.2-project` and Language level is
   `26`.
6. Under **Project Settings | Modules | Dependencies**, confirm that Module SDK is `Project SDK` and that the three
   JavaFX JARs from `lib\javafx-sdk-26.0.2\lib` are present.
7. Select the committed **RegistrationApplication** run configuration and click Run. It supplies the JavaFX module
   path, native-access option, and Quantis native-library path automatically.

If IntelliJ reports `class file has wrong version`, check **Project Structure**, the module SDK, Java compiler target,
and run-configuration JRE. Class-file version 56 is Java 12, 58 is Java 14, 68 is Java 24, and 70 is Java 26. All
project compilation and execution settings must use the bundled OpenJDK 26.0.2 SDK rather than an older system JDK.

## Build and run

```powershell
.\build.ps1
.\run.ps1
```

Both commands also work offline and use the project-local JDK rather than `PATH`.

## Quantis

The direct Java JNI declarations are included in the source tree. Install the official Quantis driver and place
`Quantis.dll` plus its native dependencies under `lib\quantis`. See `lib\quantis\README.md` for configuration and
device detection examples. Vendor binaries are not stored in Git.

## NIST SP 800-22

The retained NIST STS 2.1.2 reference source and Java port are described in
`docs\NIST_SP_800_22_JAVA_PORT.md`.
