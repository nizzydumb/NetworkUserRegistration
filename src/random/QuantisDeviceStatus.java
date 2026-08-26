package random;

/** Immutable result of a Quantis hardware detection attempt. */
public record QuantisDeviceStatus(
        QuantisDeviceType deviceType,
        int requestedDeviceNumber,
        QuantisStatus status,
        int detectedDeviceCount,
        String message
) {
    public boolean connected() {
        return status == QuantisStatus.AVAILABLE;
    }
}
