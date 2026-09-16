# Building the Windows application package

The final distributable is not a standalone JAR. It is a Windows application image containing the application
JAR, a private Java runtime, JavaFX, Java libraries, native DLLs, and an administrator-aware launcher. The target
computer does not need Java, JavaFX, JNA, Bouncy Castle, or a C compiler installed.

## Build the release image

From the repository root, run:

```powershell
.\build-release.ps1
```

The script performs these steps:

1. Rebuilds `rawdrive.dll` from C with the bundled w64devkit compiler.
2. Rebuilds all Java classes and resources with the bundled OpenJDK.
3. Creates `NetworkUserRegistration.jar` with the correct `Main-Class` and dependency class path.
4. Copies JNA, JNA Platform, Bouncy Castle, JavaFX native DLLs, and `rawdrive.dll` into the package input.
5. Copies locally installed Quantis DLLs from `lib/quantis` when they are present.
6. Runs the bundled JDK's `jpackage` to create a private Java runtime and Windows application image.
7. Compiles `NetworkUserRegistration.exe` and embeds a Windows `requireAdministrator` manifest.

The result is:

```text
dist/
└── NetworkUserRegistration/
    ├── NetworkUserRegistration.exe       elevated user-facing launcher
    ├── NetworkUserRegistrationApp.exe    jpackage Java launcher
    ├── app/                               JARs and native DLLs
    └── runtime/                           private Java/JavaFX runtime
```

Start the packaged application with:

```powershell
.\dist\NetworkUserRegistration\NetworkUserRegistration.exe
```

Windows displays a User Account Control prompt before starting the Java application. The internal
`NetworkUserRegistrationApp.exe` should normally not be launched directly because it does not request elevation.

## Why the JAR is not enough

IntelliJ's **Build Artifacts | JAR** output contains Java bytecode and resources, but a JAR does not control Windows
UAC and does not contain a Windows runtime by itself. This application additionally requires:

- a compatible Java runtime;
- JavaFX modules and their Windows DLLs;
- JNA and JNA Platform;
- Bouncy Castle provider, PKIX, and utility JARs;
- `rawdrive.dll`;
- Quantis DLLs and their native dependencies when Quantis is used;
- JVM options enabling JavaFX modules and native access.

The release image supplies these pieces and fixes their locations relative to the installed application.

## Administrator launcher

The launcher sources are under `packaging/windows`:

- `elevated-launcher.c` locates and starts `NetworkUserRegistrationApp.exe` beside itself.
- `elevated-launcher.manifest` declares `requestedExecutionLevel="requireAdministrator"`.
- `elevated-launcher.rc` embeds that manifest as Windows resource type 24.

`build-release.ps1` compiles the resource with `windres.exe` and links the launcher with `gcc.exe`. Because the UAC
requirement is embedded in a real Windows executable, it works when launched from Explorer, PowerShell, a desktop
shortcut, or the Start menu. A JAR manifest cannot provide this Windows behavior.

The application still performs its own native elevation check. UAC approval grants permission to request raw-disk
access; it does not guarantee that Windows or a storage driver will accept every operation.

## Packaged native-library paths

During normal IntelliJ development, `RawDriveNative` loads:

```text
<project>/lib/native/rawdrive/rawdrive.dll
```

The packaged configuration sets:

```text
-Drawdrive.library.path=$APPDIR\rawdrive.dll
-Djava.library.path=$APPDIR
```

`$APPDIR` is expanded by the jpackage launcher to the package's `app` directory. This allows JNA, JavaFX, Quantis,
and `rawdrive.dll` to load without depending on the user's `PATH` or current working directory.

## Quantis packaging

The licensed Quantis binaries are not supplied by this repository. Before building a release that uses Quantis,
place the vendor-provided DLLs and every vendor-required native dependency under:

```text
lib\quantis\
```

The release script copies every DLL from that directory into the application directory. If the directory contains
only the placeholder documentation, the rest of the application packages normally, but Quantis initialization will
remain unavailable.

## Distribution

Distribute the entire `dist/NetworkUserRegistration` directory. Do not copy only either EXE or only the JAR. A ZIP
archive is suitable:

```powershell
Compress-Archive -Path .\dist\NetworkUserRegistration `
    -DestinationPath .\dist\NetworkUserRegistration-windows-x64.zip -Force
```

After extraction, users run `NetworkUserRegistration.exe`. Keep the internal directory structure unchanged.

## Release verification

Before distributing a build:

1. Run `build-release.ps1` and confirm it exits successfully.
2. Confirm both EXE files, the `app` directory, and the `runtime` directory exist.
3. Start `NetworkUserRegistration.exe` and confirm Windows displays the UAC prompt.
4. Confirm authentication and the main window work without a system Java installation.
5. Confirm the drive selector initially has no selection and updates mount state after a drive is chosen.
6. Run only the temporary-image native test during routine verification. Do not perform a real raw-disk write as a
   packaging smoke test.
7. Test the complete copied/ZIP-extracted directory on a clean Windows x64 machine before release.

## Customization

The application name and image directory are configured in `build-release.ps1`. If the internal jpackage launcher
name changes, update `elevated-launcher.c` to match it. Rebuild after changing the UAC manifest or launcher source.

An installer can be added later, but the application image intentionally avoids an additional installer-toolchain
dependency. It is already self-contained and portable as a directory or ZIP archive.

## Common problems

### UAC does not appear

Make sure the user launched `NetworkUserRegistration.exe`, not `NetworkUserRegistrationApp.exe` or the JAR.

### Application starts from the project but not the package

Inspect `dist/NetworkUserRegistration/app/NetworkUserRegistrationApp.cfg`. It must contain the JNA/Bouncy Castle
classpath entries, native-access option, `rawdrive.library.path`, and `java.library.path` generated by the script.

### `rawdrive.dll` cannot be loaded

Confirm `dist/NetworkUserRegistration/app/rawdrive.dll` exists and the package was not rearranged after building.

### Quantis cannot be loaded

Recheck the vendor DLL set under `lib/quantis`, rebuild the package, and verify those DLLs appear in the packaged
`app` directory. Quantis may require more than the primary `Quantis.dll`.

### Antivirus or SmartScreen warning

The launcher and application are locally compiled and unsigned. Production distribution should use an
organization-owned Windows code-signing certificate to sign both EXE launchers and native DLLs. Code signing is
separate from administrator elevation.
