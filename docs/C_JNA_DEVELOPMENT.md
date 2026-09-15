# Building Windows C code and binding it with JNA

This guide explains how this repository builds `rawdrive.dll` and how Java calls
its exported C functions through Java Native Access (JNA). It also describes how to extend the native API safely.

## What is already bundled

The repository contains everything required for a self-contained Windows x64 build:

| Component | Project location | Purpose |
| --- | --- | --- |
| OpenJDK 26.0.2 | `lib/jdk-26.0.2` | Java compiler and runtime |
| JNA 5.19.1 | `lib/jna-5.19.1/jna-5.19.1.jar` | Loads the DLL and marshals function arguments |
| JNA Platform 5.19.1 | `lib/jna-5.19.1/jna-platform-5.19.1.jar` | Windows constants and readable error messages |
| w64devkit 2.10.0 | `lib/w64devkit-2.10.0/w64devkit` | Portable GCC/MinGW Windows C toolchain |
| Native source | `native/rawdrive` | C header and implementation |
| Runtime DLL | `lib/native/rawdrive/rawdrive.dll` | x64 native library loaded by Java |

JNA source JARs are included for IntelliJ navigation. The compiler distribution retains its included license files.
No Maven, Gradle, Visual Studio, Windows SDK installation, system JDK, or network connection is needed.

## Prerequisites

1. Copy or clone the complete repository. If cloning, install Git LFS first so every large bundled
   dependency is materialized rather than left as an LFS pointer.
2. Use Windows x64. The bundled JVM, JNA native component, compiler, and `rawdrive.dll` are x64.
3. In IntelliJ, register `lib/jdk-26.0.2` as the Project SDK.
4. Keep the application working directory set to `$PROJECT_DIR$`.

Administrator elevation is not required to compile the DLL or run the disk-image test. It is required for direct
Windows physical-drive access. JNA itself does not grant or bypass permissions.

## Build the DLL

From a PowerShell terminal opened at the repository root:

```powershell
.\build-native.ps1
```

The script resolves all paths relative to itself and runs the equivalent of:

```text
lib\w64devkit-2.10.0\w64devkit\bin\gcc.exe
  -std=c11 -O2 -Wall -Wextra -Werror -shared
  native\rawdrive\raw_drive.c
  -o lib\native\rawdrive\rawdrive.dll
  -ladvapi32
```

Important flags:

- `-std=c11` selects the C language version.
- `-O2` enables production optimization.
- `-Wall -Wextra -Werror` enables strict warnings and fails the build on a warning.
- `-shared` produces a DLL instead of an executable.
- `-ladvapi32` links the Windows API library used for process-token/elevation checks.

The build overwrites only `lib/native/rawdrive/rawdrive.dll`. Restart a running Java process after rebuilding;
Windows processes keep loaded DLLs in memory and will not automatically reload the changed file.

## Verify DLL exports

Use the bundled object inspector:

```powershell
& lib\w64devkit-2.10.0\w64devkit\bin\objdump.exe `
  -p lib\native\rawdrive\rawdrive.dll |
  Select-String "rd_|DLL Name"
```

The export list should contain:

```text
rd_is_elevated
rd_query_drive
rd_write_image
rd_write_physical
```

Its non-system runtime dependencies should remain minimal. The current build uses standard Windows system DLLs
such as `KERNEL32.dll`, `ADVAPI32.dll`, and `msvcrt.dll`.

## How Java finds the DLL

`storage.RawDriveNative` constructs an absolute path from the application working directory:

```java
Path DLL = Path.of(
    System.getProperty("user.dir"),
    "lib", "native", "rawdrive", "rawdrive.dll"
);

RawDriveNative INSTANCE = Native.load(
    DLL.toAbsolutePath().toString(),
    RawDriveNative.class
);
```

This explicit path is independent of the Windows `PATH` and `java.library.path`. It is why the IntelliJ working
directory must be `$PROJECT_DIR$`. The separate `java.library.path` setting in the run configuration is currently
for the Quantis JNI library, not for `rawdrive.dll`.

JDK 26 also expects native access to be enabled. Keep this VM option in the IntelliJ run configuration:

```text
--enable-native-access=javafx.graphics,ALL-UNNAMED
```

JNA is on the unnamed module because this project does not use a `module-info.java` file.

## C ABI and JNA mapping

The header uses `__declspec(dllexport)` to expose a stable C ABI. Do not export C++ classes or overloaded C++
functions. If the implementation is later compiled as C++, wrap declarations in `extern "C"` to prevent name
mangling.

Current parameter mappings are:

| C declaration | Java/JNA declaration | Notes |
| --- | --- | --- |
| `int` | `int` | 32-bit signed value on both sides |
| `uint32_t` | `int` | Bit width matches; validate unsigned ranges in Java |
| `uint64_t` | `long` | Bit width matches; Java `long` is signed |
| `uint64_t *` output | `LongByReference` | Native function writes into caller-provided storage |
| `int *` output | `IntByReference` | Used for flags and bus type |
| `const unsigned char *` | `byte[]` | JNA copies the Java byte array for the call |
| `const wchar_t *` input | `WString` | Windows UTF-16 string input |
| `wchar_t *` output | `Memory`/`Pointer` | Allocate `characters * Native.WCHAR_SIZE`, then use `getWideString(0)` |

The native functions use the compiler's default C calling convention, and `RawDriveNative` extends JNA's
`Library`, which uses the corresponding default mapper. If a future function is declared `__stdcall`, the Java
interface must use JNA's `StdCallLibrary` consistently instead.

## Adding another native function

Use this sequence so the C and Java declarations do not drift apart:

1. Add the exported prototype to `native/rawdrive/raw_drive.h` using fixed-width integer types.
2. Implement it in `native/rawdrive/raw_drive.c`.
3. Return `0` for success, a documented negative project code for policy/validation failures, or a positive
   `GetLastError()` value for Windows failures.
4. Add the exactly matching method declaration to `storage.RawDriveNative`.
5. Wrap the low-level call and error translation in `storage.JnaPhysicalDriveService`. Application UI code should
   not call `RawDriveNative.INSTANCE` directly.
6. Rebuild with `build-native.ps1` and confirm the new export with `objdump`.
7. Rebuild Java with `build.ps1` or IntelliJ **Build | Rebuild Project**.
8. Add a non-destructive image-file test before considering any hardware test.

Prefer primitive arguments and explicit buffers. If a structure is necessary, its C field order, field widths,
packing, and alignment must exactly match a JNA `Structure` annotated with `@FieldOrder`. A mismatch can corrupt
memory without producing a useful Java exception.

## IntelliJ dependency configuration

`NetworkUserRegistration.iml` declares both JNA JARs and attaches their source JARs. Verify the module dependency
named **JNA 5.19.1** under **File | Project Structure | Modules | Dependencies**. If reconstructing it manually,
add these as module libraries:

```text
lib/jna-5.19.1/jna-5.19.1.jar
lib/jna-5.19.1/jna-platform-5.19.1.jar
```

Attach, but do not put on the runtime classpath, these source archives:

```text
lib/jna-5.19.1/jna-5.19.1-sources.jar
lib/jna-5.19.1/jna-platform-5.19.1-sources.jar
```

The PowerShell `build.ps1` and `run.ps1` scripts discover the two runtime JARs from `lib/jna-5.19.1` automatically.

## Verification order

Use the least privileged and least destructive verification first:

1. Run `build-native.ps1`.
2. Run `build.ps1`.
3. Run `storage.RawByteWriterVerification`. It calls `rd_write_image` against a temporary 4096-byte file and never
   opens a physical-drive path.
4. Run `storage.PhysicalDriveInventoryVerification` to exercise read-only drive discovery.
5. Use `storage.PhysicalDriveWriteManualTest` only when deliberately testing with a disposable, backed-up, offline
   disk. With no arguments it performs no write.

Never substitute a real disk into an automated test. The manual write test requires all of these arguments:

```text
--execute <driveNumber> <offset> <hexBytes> WRITE-PHYSICALDRIVE-<driveNumber>
```

## Troubleshooting

### `UnsatisfiedLinkError` or “Unable to load library”

- Confirm `lib/native/rawdrive/rawdrive.dll` exists.
- Confirm the IntelliJ working directory is `$PROJECT_DIR$`.
- Rebuild the DLL and restart the Java process.
- Confirm that Java and the DLL are both x64.

### `NoClassDefFoundError: com/sun/jna/...`

The JNA JARs are missing from the module runtime classpath. Reload `NetworkUserRegistration.iml` or add both JNA
runtime JARs under Module Dependencies.

### Native-access warning on JDK 26

Add `--enable-native-access=ALL-UNNAMED`. The full application also includes `javafx.graphics` in that option.

### Windows error 5: Access is denied

For inventory or a physical write, start IntelliJ itself with **Run as administrator**; elevating only a separate
terminal does not elevate an already-running IntelliJ process. A physical write additionally requires the selected
non-system disk to be offline. Compilation and disk-image verification do not require elevation.

### Error 193: `%1 is not a valid Win32 application`

This normally means the JVM and DLL architectures differ. Use the bundled Windows x64 JDK and the DLL produced by
the bundled x64 w64devkit compiler.

### Function not found

The Java interface and DLL are from different builds, or the function was not exported. Rebuild, inspect exports
with `objdump`, check spelling and parameter order, and restart Java.

## Security and maintenance rules

- Treat the Java UI as untrusted input; repeat destructive-operation checks in C.
- Never infer or automatically select a physical write target.
- Check offset plus length without integer overflow and enforce a maximum transfer size.
- Keep system-disk and offline-state checks fail-closed.
- Flush writes and verify them by reading back.
- Log target number, offset, length, result, and error code, but avoid logging sensitive payload bytes.
- Commit C source, header, build script, compiler licenses, JNA artifacts, and the matching built DLL together so an
  self-contained checkout cannot accidentally combine incompatible revisions.
