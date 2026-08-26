# Quantis SDK drop-in directory

Obtain the current Quantis Software package for the exact USB/PCIe device and Windows architecture directly from
ID Quantique. Do not use an unrelated or older binary package.

The Java JNI declarations are compiled into this project under
`src/com/idquantique/quantis`; do not add another Quantis Java wrapper JAR because it would duplicate those classes.
Quantis JARs are intentionally excluded from both the build and runtime classpaths.

Place the vendor native files in this directory:

- `Quantis.dll` and every native DLL shipped beside it.

Install the vendor device driver separately and first verify the device with EasyQuantis or the vendor CLI.

`run.ps1` adds this directory to `java.library.path`, allowing `System.loadLibrary("Quantis")` to load
`Quantis.dll`. The direct native entry points used initially are `QuantisCount` and `QuantisRead`.

To select the device when launching manually, use:

```text
-Dentropy.provider=quantis -Dquantis.device.type=PCIE -Dquantis.device.number=0
```

Available enum/configuration values are `PCIE`, `USB`, `QRNG_CHIP`, `APPLIANCE`, and `EVALUATION_KIT`.
Only `PCIE` and `USB` use this local JNI adapter. The other values are represented now so their embedded,
network, or vendor-specific transports can be added without changing application logic. The application defaults
to Java `SecureRandom` until the Quantis provider is explicitly selected.

Check hardware status without opening a random stream:

```java
QuantisDeviceStatus status = QuantisDeviceDetector.check(QuantisDeviceType.PCIE, 0);
if (status.connected()) {
    // Device is ready.
} else {
    System.out.println(status.status() + ": " + status.message());
}
```

`QuantisDeviceDetector.isConnected(type, number)` is available for a simple boolean check. Calling
`EntropySource.isAvailable()` on `QuantisEntropySource` performs the same live check, so USB removal can be
detected after initialization.
