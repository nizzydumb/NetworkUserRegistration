# PowerShell script guide

Run scripts from the repository root, the directory containing `README.md`.
They use project-relative paths and the bundled tools; they do not depend on a
system JDK or compiler.

If PowerShell blocks local scripts, allow them only for the current terminal:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

This setting disappears when that PowerShell window closes.

## Quantis driver installation

`install-quantis-driver.ps1` installs the retained vendor-signed USB driver.
It must be run from an Administrator PowerShell:

```powershell
.\install-quantis-driver.ps1
```

It does not install a Java library or rebuild the application. The driver
directory is marked binary-preserved in `.gitattributes` because the catalog
signs exact file bytes. If Windows rejects the repository copy but accepts the
original vendor directory, delete the old checkout, run `git lfs pull`, and
clone/checkout again with the current attributes before retrying.

## Normal commands

### `setup-dependencies.ps1`

Checks that the bundled JDK, JavaFX, Bouncy Castle, JNA, and required Git LFS
objects are complete and executable.

```powershell
.\setup-dependencies.ps1
```

It does not download, install, extract, or move anything. Run it directly when
diagnosing a fresh clone. `build.ps1` also calls it automatically.

### `build.ps1`

Validates dependencies, compiles every Java source file, and copies resources
from `src` into `out\production\NetworkUserRegistration`.

```powershell
.\build.ps1
```

It does not launch the program or rebuild native DLLs.

### `run.ps1`

Calls `build.ps1` first, then launches `app.RegistrationApplication` with the
correct JavaFX module path, classpath, native-access option, and Quantis DLL
directory.

```powershell
.\run.ps1
```

For ordinary command-line development, this is the only command required.
Do not run `build.ps1` immediately before it unless a separate compile check is
desired, because `run.ps1` builds again.

### `build-release.ps1`

Performs the complete release pipeline:

1. rebuilds the raw-drive DLL;
2. rebuilds the Quantis x64 JNI DLL;
3. compiles the Java application;
4. creates the application JAR and private Java runtime;
5. creates the Windows application image and elevated launcher;
6. includes the Quantis driver and its documentation.

```powershell
.\build-release.ps1
```

Start the result with:

```text
dist\NetworkUserRegistration\NetworkUserRegistration.exe
```

Distribute the complete `dist\NetworkUserRegistration` directory. This script
already calls all prerequisite build scripts, so do not run them first.

## Specialized native rebuilds

These are needed only after changing native source code.

| Script | Use it when | Output |
|---|---|---|
| `build-native.ps1` | `native/rawdrive/raw_drive.c` changed | `lib/native/rawdrive/rawdrive.dll` |
| `build-quantis.ps1` | retained Quantis C/C++ or JNI source changed | `lib/quantis/Quantis.dll` |

Both DLLs are already stored in the repository for direct IntelliJ use.

## Which command should I use?

| Goal | Command |
|---|---|
| Check a newly cloned dependency tree | `.\setup-dependencies.ps1` |
| Compile Java only | `.\build.ps1` |
| Compile and run from PowerShell | `.\run.ps1` |
| Rebuild only raw-drive native code | `.\build-native.ps1` |
| Rebuild only Quantis native code | `.\build-quantis.ps1` |
| Produce the complete distributable application | `.\build-release.ps1` |

IntelliJ users do not need any script for normal compilation and execution.
Select the committed `RegistrationApplication` run configuration and press
Run.
