package random;

import com.idquantique.quantis.Quantis;
import com.idquantique.quantis.QuantisException;

/** Detects local Quantis PCIe/USB devices without opening a random-data stream. */
public final class QuantisDeviceDetector {
    private QuantisDeviceDetector() {
    }

    public static QuantisDeviceStatus check(QuantisDeviceType deviceType, int deviceNumber) {
        if (deviceType == null) {
            throw new IllegalArgumentException("Quantis device type is required.");
        }
        if (deviceNumber < 0) {
            throw new IllegalArgumentException("Quantis device number cannot be negative.");
        }
        if (!deviceType.supportsLocalJni()) {
            return result(deviceType, deviceNumber, QuantisStatus.UNSUPPORTED_INTEGRATION, 0,
                    deviceType + " requires " + deviceType.integration() + " integration, not local JNI.");
        }

        try {
            int count = detectedDeviceCount(deviceType);
            if (deviceNumber < count) {
                return result(deviceType, deviceNumber, QuantisStatus.AVAILABLE, count,
                        "Quantis " + deviceType + " device #" + deviceNumber + " is available.");
            }
            return result(deviceType, deviceNumber, QuantisStatus.DEVICE_NOT_FOUND, count,
                    "Requested device #" + deviceNumber + ", but " + count + " Quantis " + deviceType
                            + " device(s) were detected.");
        } catch (LinkageError exception) {
            return result(deviceType, deviceNumber, QuantisStatus.NATIVE_LIBRARY_NOT_AVAILABLE, 0,
                    "The Quantis native library or one of its dependencies could not be loaded: "
                            + safeMessage(exception));
        } catch (QuantisException | RuntimeException exception) {
            return result(deviceType, deviceNumber, QuantisStatus.ERROR, 0,
                    "Quantis detection failed: " + safeMessage(exception));
        }
    }

    public static boolean isConnected(QuantisDeviceType deviceType, int deviceNumber) {
        return check(deviceType, deviceNumber).connected();
    }

    public static int detectedDeviceCount(QuantisDeviceType deviceType) throws QuantisException {
        if (deviceType == null || !deviceType.supportsLocalJni()) {
            throw new IllegalArgumentException("Only Quantis PCIe and USB devices support local JNI detection.");
        }

        return Quantis.count(deviceType.nativeType());
    }

    private static QuantisDeviceStatus result(
            QuantisDeviceType type,
            int number,
            QuantisStatus status,
            int count,
            String message
    ) {
        return new QuantisDeviceStatus(type, number, status, count, message);
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
