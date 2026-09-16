# Quantis USB-4M setup

## What is already included

The project is prepared for the ID Quantique Quantis USB-4M identified by
USB hardware ID `VID_0ABA&PID_0102`.

| Component | Project location | Purpose |
|---|---|---|
| Java JNI declarations | `src/com/idquantique/quantis` | Calls the vendor JNI entry points directly |
| Application adapter | `src/random` | Selection, detection, and entropy-source abstraction |
| Windows x64 JNI library | `lib/quantis/Quantis.dll` | Native bridge and Quantis USB implementation |
| Official native source | `third_party/quantis-20.2.3/native` | Reproducible DLL build |
| Official signed USB driver | `third_party/quantis-20.2.3/driver/QuantisUsb` | Associates the USB device with Windows WinUSB |
| Native build command | `build-quantis.ps1` | Rebuilds the x64 JNI library |

The upstream packaged runtime is named
`QuantisRNG-20.2.3-Windows-win32.zip` and contains a 32-bit DLL. It cannot be
loaded by the project's Windows x64 JDK. Consequently, this repository builds
an x64 DLL from the official 20.2.3 native/JNI source.

The resulting DLL imports only `KERNEL32.dll`, `msvcrt.dll`, and
`SETUPAPI.dll`, and loads the Windows-provided `WinUSB.dll` while opening the
device. Those are Windows components, so no additional runtime DLL needs to be
copied beside `Quantis.dll`.

## 1. Install the device driver once

The DLL is an application library; it is not the USB device driver. Windows
must first associate the hardware with the supplied signed WinUSB driver.

Start PowerShell as Administrator in the repository root and run:

```powershell
pnputil.exe /add-driver ".\third_party\quantis-20.2.3\driver\QuantisUsb\QuantisUsb.inf" /install
```

Alternatively, open Device Manager, right-click **Quantis USB** under
**Other devices**, choose **Update driver**, then **Browse my computer for
drivers**, and select:

```text
<project>\third_party\quantis-20.2.3\driver\QuantisUsb
```

Enable **Include subfolders**. After installation, reconnect the device if
necessary. Administrator rights are required only for driver installation,
not normally for reading random data.

The included catalog has a valid ID Quantique SA signature. The package is a
legacy 2010 WinUSB driver shipped in the vendor's 20.2.3 release. If current
Windows security policy rejects it, obtain an updated signed legacy-device
driver from ID Quantique instead of disabling signature enforcement or Memory
Integrity.

## 2. Verify Windows device state

In Device Manager, the device should appear as **Quantis USB Random Number
Generator** under **IDQ devices**, without a warning icon or Code 28.

For a command-line check:

```powershell
pnputil.exe /enum-devices /connected
```

Locate `USB\VID_0ABA&PID_0102`. Its status must be `Started`, and it must no
longer report `CM_PROB_FAILED_INSTALL` or problem code 28.

## 3. Configure IntelliJ IDEA

The committed `RegistrationApplication` run configuration already contains:

```text
--enable-native-access=javafx.graphics,ALL-UNNAMED
-Djava.library.path="$PROJECT_DIR$/lib/quantis"
```

If creating another IntelliJ Application configuration, copy those options.
Use the project-local Windows x64 JDK 26.0.2. A 32-bit Java runtime cannot load
the included x64 DLL.

The application defaults to Java `SecureRandom`. To select the connected USB
device explicitly, add these VM options:

```text
-Dentropy.provider=quantis
-Dquantis.device.type=USB
-Dquantis.device.number=0
```

Device numbering starts at zero. Leave the default provider in place until
the driver has been installed and device detection succeeds.

## 4. Verify from Java

Run the committed **Quantis diagnostics** configuration in IntelliJ. It checks
USB device index zero, captures 125,000 bytes from an available device, and
runs the complete Java NIST SP 800-22 first-level suite over that 1,000,000-bit
sample. Every p-value and a pass/fail summary are printed to the Run console.

The diagnostic accepts:

```text
QuantisDiagnostics [device-number] [--nist]
```

Remove `--nist` from the run configuration to perform only the non-destructive
connection check. A single tested stream is a diagnostic result, not NIST
certification; complete interpretation and second-level testing limitations
are documented in `docs/NIST_SP_800_22_JAVA_PORT.md`.

The non-destructive connection check is:

```java
QuantisDeviceStatus status =
        QuantisDeviceDetector.check(QuantisDeviceType.USB, 0);
System.out.println(status.status() + ": " + status.message());
```

Expected results:

| Status | Meaning |
|---|---|
| `AVAILABLE` | DLL loaded, driver is working, and device index 0 exists |
| `DEVICE_NOT_FOUND` | DLL loaded, but the driver reports no matching device |
| `NATIVE_LIBRARY_NOT_AVAILABLE` | DLL is missing, wrong architecture, or cannot be loaded |
| `ERROR` | The native API loaded but returned another operational failure |

`QuantisDeviceDetector.isConnected(QuantisDeviceType.USB, 0)` provides the
same live check as a boolean. It can be called again to detect removal.

## 5. Rebuild the x64 JNI library

The repository includes the compiler and JDK headers required by the build.
From the project root run:

```powershell
.\build-quantis.ps1
```

The script compiles only the required USB, C API, and JNI translation units,
with PCI/PCIe support disabled, and writes:

```text
lib\quantis\Quantis.dll
```

No Visual Studio installation is required. The DLL must be rebuilt after
changing the retained native source. The Java build itself does not compile C
code because a ready x64 DLL is committed for direct IntelliJ use.

## 6. Release packaging

`build-release.ps1` copies every DLL in `lib/quantis` into the application
image and configures its directory as `java.library.path`. Distribute the
complete application image, not only its JAR.

The release image includes the signed package under `drivers\QuantisUsb`, a
copy of this guide, and the upstream license. The application intentionally
does not install the driver itself. Install it separately on each destination
computer before using the Quantis entropy provider.

## Troubleshooting

### `UnsatisfiedLinkError: no Quantis in java.library.path`

Confirm `lib\quantis\Quantis.dll` exists and the run configuration contains
the project-relative `java.library.path` option shown above.

### `%1 is not a valid Win32 application`

Java and the DLL have different architectures. Use the bundled Windows x64
JDK and the included x64 DLL; do not substitute the upstream win32 DLL.

### `QuantisCount` returns zero

The JNI library is working, but Windows exposes no usable device. Check the
cable, Code 28, the driver association, and whether the device still appears
under **IDQ devices**.

### JNI method is reported missing

Keep the Java class in package `com.idquantique.quantis` and retain the native
method names. They are part of the JNI ABI exported by `Quantis.dll`.

### Driver installation is rejected

Do not download replacement DLLs or drivers from generic driver websites.
Use an updated signed package from ID Quantique when the supplied legacy
driver is incompatible with the destination Windows policy.
