# Network User Registration

Windows JavaFX application for network/user registration, Quantis USB entropy,
NIST SP 800-22 diagnostics, and guarded physical-drive access.

## Immediately after cloning

Use Git with Git LFS. Do not use GitHub's **Download ZIP** for this project,
because several required JDK and JavaFX files are stored as Git LFS objects.

For a new clone:

```powershell
git lfs install
git clone <repository-url>
cd NetworkUserRegistration
git lfs pull
.\setup-dependencies.ps1
```

If the repository was already cloned, run these commands in its root:

```powershell
git lfs install
git lfs pull
.\setup-dependencies.ps1
```

Nothing should be unzipped or moved after a correct Git LFS clone. Open the
directory that directly contains `src`, `lib`, `README.md`, and
`NetworkUserRegistration.iml`.

`setup-dependencies.ps1` is a validator: it does not download or install
anything. A successful run ends with:

```text
Dependency check passed: OpenJDK, JavaFX, Bouncy Castle, JNA, and Git LFS files are ready.
```

If PowerShell refuses to run scripts, use this only in the current terminal:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

## IntelliJ IDEA setup

Use IntelliJ IDEA with Java 26 support.

1. Open the repository root with **File | Open**. Do not open `src`, `lib`, or
   the `.iml` file as a separate project.
2. Open **File | Project Structure | Platform Settings | SDKs**.
3. Remove or correct any broken SDK entry named `openjdk-26.0.2` that points to
   a path on another computer.
4. Press **+ | Add JDK** and choose:

   ```text
   <project>\lib\jdk-26.0.2
   ```

   Select the JDK directory itself, not `bin`, `java.exe`, `javac.exe`,
   `lib`, or `lib\modules`.
5. Keep or rename the SDK to exactly `openjdk-26.0.2`, which matches
   `.idea/misc.xml`.
6. Under **Project Settings | Project**, select `openjdk-26.0.2` as the Project
   SDK and use language level **26** or **SDK default**.
7. Under **Project Settings | Modules | Dependencies**, set Module SDK to
   **Project SDK**.
8. Under **Settings | Build, Execution, Deployment | Compiler | Java
   Compiler**, confirm target bytecode **26**.
9. Use **File | Reload All from Disk**, then **Build | Rebuild Project**.

The root `NetworkUserRegistration.iml` already declares JavaFX 26.0.2, Bouncy
Castle 1.84, and JNA 5.19.1 with project-relative paths. Do not manually add
their JARs unless that module file was not imported.

### If IntelliJ refuses the bundled JDK

The bundled directory is a complete and working Windows x64 OpenJDK 26.0.2.
The following command must succeed:

```powershell
.\lib\jdk-26.0.2\bin\java.exe -version
```

Check these causes in order:

1. **Git LFS objects are missing.** Run `git lfs pull`, then
   `.\setup-dependencies.ps1`. The file `lib\jdk-26.0.2\lib\modules` should be
   roughly 146 MB, not a small text file beginning with
   `version https://git-lfs.github.com/spec/v1`.
2. **The wrong directory was selected.** Add `lib\jdk-26.0.2`, not its `bin`
   directory or an executable.
3. **A stale global IntelliJ SDK exists.** Delete the broken
   `openjdk-26.0.2` entry under Platform Settings | SDKs, add the project copy,
   then explicitly reselect it for both Project and Module.
4. **The project was opened at the wrong level.** Reopen the directory
   containing `NetworkUserRegistration.iml`.
5. **The IntelliJ build lacks Java 26 support.** Update IntelliJ IDEA, then add
   the JDK again. IntelliJ's own runtime does not need to be changed.
6. **Security software blocked extracted executables.** Check Windows Security
   history and file properties. Do not download individual replacement DLLs or
   JDK files from third-party sites.

If `java.exe -version` works and `setup-dependencies.ps1` passes, the JDK files
are valid; the remaining problem is IntelliJ SDK registration or project/module
selection.

## Application entry point

The single application entry point is:

```text
app.RegistrationApplication
```

Run the committed IntelliJ configuration named **RegistrationApplication**.
Do not create a configuration by right-clicking a random class: the committed
configuration supplies the JavaFX module path and native-library options.

Its required settings are:

```text
Main class: app.RegistrationApplication
Use classpath of module: NetworkUserRegistration
JRE: Project SDK (openjdk-26.0.2)
Working directory: $PROJECT_DIR$
```

VM options:

```text
--module-path "$PROJECT_DIR$/lib/javafx-sdk-26.0.2/lib"
--add-modules javafx.controls
--enable-native-access=javafx.graphics,ALL-UNNAMED
-Djava.library.path="$PROJECT_DIR$/lib/quantis"
```

The old forwarding `app.Main` class and duplicate run configuration were
removed because they created a second apparent entry point without adding any
functionality.

## PowerShell commands

For ordinary command-line execution, run only:

```powershell
.\run.ps1
```

It validates dependencies, compiles the Java sources, and starts
`app.RegistrationApplication`. You do not need to run `build.ps1` first.

| Goal | Command | Calls other scripts automatically? |
|---|---|---|
| Validate a fresh clone | `.\setup-dependencies.ps1` | No; performs checks only |
| Compile Java and copy resources | `.\build.ps1` | Yes; dependency check |
| Compile and launch the application | `.\run.ps1` | Yes; `build.ps1` |
| Rebuild raw-drive C DLL | `.\build-native.ps1` | No |
| Rebuild Quantis JNI DLL | `.\build-quantis.ps1` | No |
| Build the complete distributable image | `.\build-release.ps1` | Yes; both native builds and Java build |

There is no required sequence involving every script. Use the one matching the
desired outcome. Full descriptions, inputs, outputs, and ordering are in
[docs/POWERSHELL_SCRIPTS.md](docs/POWERSHELL_SCRIPTS.md).

## Windows application image

Build the application image with:

```powershell
.\build-release.ps1
```

Start:

```text
dist\NetworkUserRegistration\NetworkUserRegistration.exe
```

Distribute the entire `dist\NetworkUserRegistration` directory, not only its
JAR or EXE. See [Windows packaging](docs/WINDOWS_PACKAGING.md).

The outer `NetworkUserRegistration.exe` is the user-facing administrator
launcher and should be started from Explorer or a normal interactive PowerShell
window. `NetworkUserRegistrationApp.exe` is the internal jpackage launcher;
keep it in the same directory and do not distribute it separately.

## Feature documentation

- [Quantis USB setup](docs/QUANTIS_USB_SETUP.md)
- [NIST SP 800-22 Java port](docs/NIST_SP_800_22_JAVA_PORT.md)
- [Raw physical-drive writes](docs/RAW_DRIVE_WRITES.md)
- [C and JNA development](docs/C_JNA_DEVELOPMENT.md)
- [Bouncy Castle](docs/BOUNCY_CASTLE.md)
- [Icons](docs/ICONS.md)
