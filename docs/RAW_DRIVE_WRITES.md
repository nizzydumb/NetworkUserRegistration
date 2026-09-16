# Native/JNA physical-drive access on Windows

This feature can permanently destroy partitions, filesystems, recovery data, and operating systems. The main
window only contains a read-only physical-drive dropdown. There is no write page or write button. Physical writes
are exposed through a deliberately explicit manual integration-test class.

## Architecture

- `native/rawdrive/raw_drive.c` is the Windows C implementation using `CreateFileW`, `DeviceIoControl`,
  `SetFilePointerEx`, `WriteFile`, `FlushFileBuffers`, and `ReadFile`.
- `lib/native/rawdrive/rawdrive.dll` is the compiled x64 library used at runtime.
- `storage.RawDriveNative` contains the direct JNA declarations.
- `storage.JnaPhysicalDriveService` exposes disk inventory and writes to Java.
- `storage.PhysicalDriveInfo` is the dropdown model.
- `storage.RawByteWriterVerification` tests the same DLL against a temporary image file only.
- `storage.PhysicalDriveInventoryVerification` tests drive discovery without writing.
- `storage.PhysicalDriveWriteManualTest` is the opt-in destructive integration test.

JNA does not bypass Windows permissions. Start IntelliJ itself with **Run as administrator**, then run the JavaFX
application. DLL and JVM architecture must match; both bundled builds are Windows x64.

For the complete compiler, ABI mapping, IntelliJ, extension, verification, and troubleshooting procedure, see
`docs/C_JNA_DEVELOPMENT.md`.

## Dependencies and IntelliJ

JNA 5.19.1 and JNA Platform 5.19.1 binaries and sources are under `lib/jna-5.19.1`. They are declared in
`NetworkUserRegistration.iml`, so no Maven or internet access is required. The run configuration must use the
project root as its working directory because the DLL is resolved from `lib/native/rawdrive/rawdrive.dll`.

To rebuild the DLL after editing C:

```powershell
.\build-native.ps1
```

The script uses the committed w64devkit 2.10.0 compiler at
`lib/w64devkit-2.10.0/w64devkit/bin/gcc.exe`. Compiler copyright notices are retained inside that distribution.

## Native return contract

Zero means success. Positive values are Windows system error codes. Negative library codes are:

- `-1`: invalid argument
- `-2`: disk is online
- `-3`: disk contains the active Windows installation, or that check could not be performed safely
- `-4`: write is outside the device/file boundary
- `-5`: read-back differs from the requested bytes

The native `rd_write_physical` function repeats all critical checks even if it is called without the JavaFX UI. It
rejects system disks, online disks, empty writes, writes over 1 MiB, and out-of-bounds ranges. It flushes and reads
the requested range back before returning success. Because Windows raw-disk I/O is sector based, a byte-addressed
request uses a read-modify-write of only the surrounding sector(s), preserving their other bytes.

## Using the dropdown

The **Physical Drive** dropdown is always visible in the main-window header. It shows the disk number, capacity,
model, online/offline state, and whether a volume on the disk currently has a drive-letter or directory mount
point. It starts with no selected disk, refreshes its inventory whenever it is opened, and re-queries the selected
disk every two seconds. Selecting a drive has no side effect and cannot initiate a write.

Disk numbers can change after reconnecting hardware. Re-check the displayed model and size every time.

## Safe verification

This test creates and deletes a 4096-byte temporary image. It calls only `rd_write_image`; it never opens a
`PhysicalDrive` path:

```powershell
.\build.ps1
$cp = @(
  "out\production\NetworkUserRegistration",
  "lib\jna-5.19.1\jna-5.19.1.jar",
  "lib\jna-5.19.1\jna-platform-5.19.1.jar"
) -join [IO.Path]::PathSeparator
& lib\jdk-26.0.2\bin\java.exe --enable-native-access=ALL-UNNAMED `
  -cp $cp storage.RawByteWriterVerification
```

Never use a real disk in automated tests.

## Read-only inventory test

Run `storage.PhysicalDriveInventoryVerification` from IntelliJ. It prints the elevation state and every drive found,
then exits. It never calls the native write function.

## Explicit physical-write test

Run `storage.PhysicalDriveWriteManualTest` only against a disposable and fully backed-up disk. The implementation
does not require the disk to be offline, but Windows or the storage driver may still reject raw writes while its
volumes are mounted. A successful write to a mounted filesystem can corrupt it.
With no program arguments it prints help and performs no write. An actual invocation requires all five arguments:

```text
--execute 2 0x100000 DEADBEEF WRITE-PHYSICALDRIVE-2
```

The test re-enumerates the explicitly numbered drive and rejects the Windows system disk. The native DLL repeats
the system-disk check, performs the sector-aware write, flushes it, and verifies the requested bytes by reading them
back. Online/offline state remains visible in the inventory but is informational rather than an application gate.
