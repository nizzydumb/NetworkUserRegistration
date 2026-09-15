# Guarded raw byte-offset writes on Windows

Raw physical-drive writes can destroy filesystems, partition tables, recovery data, and operating systems. The
implementation in this project deliberately supports physical writes only to a USB disk that Windows reports as
offline. It never selects a disk automatically.

## Components

- `tools/write-raw-bytes.ps1`: guarded writer and read-back verification.
- `storage.RawByteWriteRequest`: validates and stores a drive number, byte offset, and bytes.
- `storage.WindowsRawByteWriter`: invokes the PowerShell writer from Java.
- `storage.RawByteWriterVerification`: non-destructive disk-image test.

## Before a physical write

1. Back up the target drive.
2. Identify its disk number in Disk Management or with `Get-Disk`.
3. Verify its model, capacity, and USB bus type. Disk numbers can change after reconnecting hardware.
4. Take the intended USB disk offline in Disk Management or an elevated PowerShell session.
5. Ensure the byte offset and data format come from the target device's documented layout.

The script rejects online disks and non-USB disks. It also validates the write range and reads the bytes back after
flushing them.

## Dry run

A dry run inspects the target but writes nothing:

```powershell
.\tools\write-raw-bytes.ps1 `
    -DriveNumber 2 `
    -ByteOffset 1048576 `
    -HexBytes "DE AD BE EF"
```

Review the reported device path, model, size, offset, and byte count.

## Explicit physical write

Run PowerShell as Administrator and supply both execution gates:

```powershell
.\tools\write-raw-bytes.ps1 `
    -DriveNumber 2 `
    -ByteOffset 1048576 `
    -HexBytes "DE AD BE EF" `
    -Execute `
    -Confirmation "WRITE-PHYSICALDRIVE-2"
```

The address is a zero-based byte offset from the start of `\\.\PhysicalDrive2`. Hex input may contain spaces or
`0x` prefixes. It must contain complete bytes.

## Java usage

```java
Path script = Path.of("tools", "write-raw-bytes.ps1");
WindowsRawByteWriter writer = new WindowsRawByteWriter(script);
RawByteWriteRequest request = new RawByteWriteRequest(
        2,
        1_048_576L,
        new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}
);

RawWriteResult inspection = writer.dryRun(request);
System.out.println(inspection.output());

RawWriteResult write = writer.writePhysical(
        request,
        WindowsRawByteWriter.confirmationToken(request.driveNumber())
);
```

The Java process must itself be elevated for the physical write. Check `RawWriteResult.successful()` and retain its
output in application logs. Do not build a UI that silently derives or hides the confirmation token.

## Safe test

The verification class creates a 4096-byte temporary `.img` file, writes four bytes at offset 1024 through
PowerShell, verifies the resulting file, checks invalid and out-of-range requests, and deletes it. The image mode
accepts only files with an `.img` extension and never opens a physical device:

```powershell
.\build.ps1
$bc = Get-ChildItem lib\bouncycastle-1.84\*.jar |
    Where-Object Name -NotLike "*-sources.jar" |
    Where-Object Name -NotLike "*-javadoc.jar" |
    ForEach-Object FullName
$cp = (@("out\production\NetworkUserRegistration") + $bc) -join [IO.Path]::PathSeparator
& lib\jdk-26.0.2\bin\java.exe -cp $cp storage.RawByteWriterVerification
```

Never use a physical disk in automated tests.
