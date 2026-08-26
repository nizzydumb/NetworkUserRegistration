package random;

import com.idquantique.quantis.Quantis;
import com.idquantique.quantis.QuantisException;
import logging.AppLogger;

/**
 * Application adapter over the direct Quantis JNI binding.
 */
public final class QuantisEntropySource implements EntropySource {
    private final QuantisDeviceType deviceType;
    private final int deviceNumber;
    private final Quantis quantis;

    public QuantisEntropySource(QuantisDeviceType deviceType, int deviceNumber) {
        if (deviceNumber < 0) {
            throw new IllegalArgumentException("Quantis device number cannot be negative.");
        }
        if (deviceType == null) {
            throw new IllegalArgumentException("Quantis device type is required.");
        }
        if (!deviceType.supportsLocalJni()) {
            throw new IllegalArgumentException(deviceType + " requires " + deviceType.integration()
                    + " integration and cannot be opened through the local JNI adapter.");
        }
        this.deviceType = deviceType;
        this.deviceNumber = deviceNumber;

        try {
            int count = QuantisDeviceDetector.detectedDeviceCount(deviceType);
            if (deviceNumber >= count) {
                throw new IllegalStateException("Quantis " + deviceType + " device #" + deviceNumber
                        + " was requested, but only " + count + " device(s) were detected.");
            }
            this.quantis = new Quantis(deviceType.nativeType(), deviceNumber);
            AppLogger.info("QUANTIS_INITIALIZED type=" + deviceType + " device=" + deviceNumber);
        } catch (QuantisException | LinkageError exception) {
            throw initializationFailure(exception);
        }
    }

    @Override
    public byte[] nextBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Random byte length cannot be negative.");
        }
        try {
            return quantis.readBytes(length);
        } catch (QuantisException exception) {
            AppLogger.error("QUANTIS_READ_FAILED type=" + deviceType + " device=" + deviceNumber, exception);
            throw new IllegalStateException("Quantis failed to provide random bytes.", exception);
        }
    }

    @Override
    public boolean isAvailable() {
        return QuantisDeviceDetector.isConnected(deviceType, deviceNumber);
    }

    @Override
    public String name() {
        return "Quantis " + deviceType + " #" + deviceNumber;
    }

    private IllegalStateException initializationFailure(Throwable exception) {
        AppLogger.error("QUANTIS_INITIALIZATION_FAILED type=" + deviceType + " device=" + deviceNumber,
                exception);
        return new IllegalStateException(
                "Could not initialize Quantis. Install the matching driver and place the native libraries in "
                        + "lib/quantis. See lib/quantis/README.md.",
                exception
        );
    }
}
