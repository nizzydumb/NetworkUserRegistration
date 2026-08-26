package random;

/** Result categories returned while detecting a Quantis device. */
public enum QuantisStatus {
    AVAILABLE,
    DEVICE_NOT_FOUND,
    SDK_NOT_INSTALLED,
    NATIVE_LIBRARY_NOT_AVAILABLE,
    UNSUPPORTED_INTEGRATION,
    ERROR
}
