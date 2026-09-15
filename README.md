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

## Configure and run directly in IntelliJ IDEA

The root `NetworkUserRegistration.iml` is explicitly included in Git. It defines the source root and portable,
project-relative JavaFX, Bouncy Castle, and JNA libraries. IntelliJ should load those libraries automatically. The JDK is
still selected manually because IntelliJ stores SDK registrations outside the project.

### 1. Open the project

Clone with Git LFS enabled so the large dependency files are materialized, then open the repository root. If the
project was delivered as a complete archive, extract it and open the directory containing `src`, `lib`, and
`NetworkUserRegistration.iml`.

If IntelliJ does not recognize `src`, right-click it and select **Mark Directory As | Sources Root**.

### 2. Install the project-local JDK

1. Open **File | Project Structure | Platform Settings | SDKs**.
2. Press **+**, select **Add JDK**, and choose `<project>\lib\jdk-26.0.2`.
3. Select the JDK directory itself—not `bin`, `java.exe`, `javac.exe`, or `lib\modules`.
4. Give it any clear name, for example `Project OpenJDK 26.0.2`.
5. Under **Project Settings | Project**, select it as **Project SDK** and set Language level to `26` or `SDK default`.
6. Under **Project Settings | Modules | Dependencies**, set Module SDK to `Project SDK`.

A JDK is an IntelliJ SDK, not a JAR dependency. IntelliJ stores SDK registrations outside the project, so this one
selection cannot be safely encoded in `.iml`.

### 3. Verify the imported JavaFX library

Open **Project Structure | Modules | Dependencies** and confirm that `JavaFX 26.0.2` appears. The module definition
includes `javafx.base.jar`, `javafx.graphics.jar`, `javafx.controls.jar`, and the SDK source archive. If IntelliJ did
not load it, reload the project from disk before adding the same files manually.

Do not add `javafx-swt.jar` unless SWT is installed separately. Add other JavaFX modules only when future source
code imports them.

### 4. Verify the imported Bouncy Castle library

Confirm that `Bouncy Castle 1.84` appears under **Project Structure | Modules | Dependencies**. Its three runtime
JARs, source archives, and Javadocs are already declared in `.iml`. Source and Javadoc archives are attached for
navigation but are not placed on the runtime classpath.

### 5. Configure compilation and execution

Under **Settings | Build, Execution, Deployment | Compiler | Java Compiler**, set target bytecode to `26`. Then use
**Build | Rebuild Project**.

The committed `RegistrationApplication` run configuration should appear automatically. If it does not, create an
**Application** configuration with:

```text
Main class: app.RegistrationApplication
Use classpath of module: NetworkUserRegistration
JRE: Project SDK
Working directory: $PROJECT_DIR$
```

Use these VM options:

```text
--module-path "$PROJECT_DIR$/lib/javafx-sdk-26.0.2/lib"
--add-modules javafx.controls
--enable-native-access=javafx.graphics,ALL-UNNAMED
-Djava.library.path="$PROJECT_DIR$/lib/quantis"
```

The IntelliJ JavaFX library makes JavaFX classes available during compilation. The `--module-path` and
`--add-modules` options make JavaFX available to the JVM during execution. Both configurations are required.

If IntelliJ reports `class file has wrong version`, check **Project Structure**, the module SDK, Java compiler target,
and run-configuration JRE. Class-file version 56 is Java 12, 58 is Java 14, 68 is Java 24, and 70 is Java 26. All
project compilation and execution settings must use the bundled OpenJDK 26.0.2 SDK rather than an older system JDK.

### JNA and native C library

Confirm that `JNA 5.19.1` appears under **Project Structure | Modules | Dependencies**. The compiled x64 DLL is at
`lib/native/rawdrive/rawdrive.dll`; keep the run configuration working directory set to `$PROJECT_DIR$`. To use the
physical-drive screen, launch IntelliJ as Administrator. Rebuild native C changes with `build-native.ps1`; the
w64devkit compiler is included in the repository.

## Build and run

```powershell
.\build.ps1
.\run.ps1
```

Both commands use the project-local JDK rather than `PATH`.

## Quantis

The direct Java JNI declarations are included in the source tree. Install the official Quantis driver and place
`Quantis.dll` plus its native dependencies under `lib\quantis`. See `lib\quantis\README.md` for configuration and
device detection examples. Vendor binaries are not stored in Git.

## NIST SP 800-22

The retained NIST STS 2.1.2 reference source and Java port are described in
`docs\NIST_SP_800_22_JAVA_PORT.md`.

## Icons

The application/window PNG assets, inline SVG button icons, import examples, accessibility rules, and licensing
checklist are documented in `docs\ICONS.md`.

## Bouncy Castle

Bouncy Castle Java 1.84 is bundled with provider, utility, and PKIX/CMS artifacts plus matching
sources, Javadocs, POM metadata, and SHA-256 records. Both IntelliJ and PowerShell builds include it automatically.
Registration and usage examples are in `docs\BOUNCY_CASTLE.md`.

## Raw physical-drive writes

A guarded Windows C library with direct JNA calls, the read-only JavaFX drive dropdown, and separate inventory,
disk-image, and opt-in physical-write test classes are documented in `docs\RAW_DRIVE_WRITES.md`.
The complete C compilation and JNA binding workflow is documented in
`docs\C_JNA_DEVELOPMENT.md`.
