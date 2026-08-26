package com.idquantique.quantis;

import java.util.Objects;

/**
 * Direct JNI binding for the ID Quantique Quantis native library.
 *
 * <p>The package, class, and native method names intentionally match the
 * reference Quantis Java wrapper because they form part of the JNI ABI.</p>
 */
public final class Quantis {
    /** Maximum number of bytes accepted by one QuantisRead native call. */
    public static final int MAX_READ_SIZE = 4096;

    static {
        System.loadLibrary("Quantis");
    }

    public enum QuantisDeviceType {
        QUANTIS_DEVICE_PCI(1),
        QUANTIS_DEVICE_USB(2);

        private final int value;

        QuantisDeviceType(int value) {
            this.value = value;
        }

        int value() {
            return value;
        }
    }

    private final QuantisDeviceType deviceType;
    private final int deviceNumber;

    public Quantis(QuantisDeviceType deviceType, int deviceNumber) {
        this.deviceType = Objects.requireNonNull(deviceType, "Quantis device type is required.");
        if (deviceNumber < 0) {
            throw new IllegalArgumentException("Quantis device number cannot be negative.");
        }
        this.deviceNumber = deviceNumber;
    }

    public static int count(QuantisDeviceType deviceType) throws QuantisException {
        Objects.requireNonNull(deviceType, "Quantis device type is required.");
        return QuantisCount(deviceType.value());
    }

    /** Compatibility spelling used by the reference Java wrapper. */
    public static int Count(QuantisDeviceType deviceType) throws QuantisException {
        return count(deviceType);
    }

    public byte[] read(int size) throws QuantisException {
        if (size < 0 || size > MAX_READ_SIZE) {
            throw new IllegalArgumentException("A native Quantis read must contain between 0 and "
                    + MAX_READ_SIZE + " bytes.");
        }
        return size == 0 ? new byte[0] : QuantisRead(deviceType.value(), deviceNumber, size);
    }

    /** Compatibility spelling used by the reference Java wrapper. */
    public byte[] Read(int size) throws QuantisException {
        return read(size);
    }

    /** Reads an arbitrary amount by respecting the native 4096-byte call limit. */
    public synchronized byte[] readBytes(int size) throws QuantisException {
        if (size < 0) {
            throw new IllegalArgumentException("Quantis byte count cannot be negative.");
        }
        byte[] output = new byte[size];
        int offset = 0;
        while (offset < size) {
            int chunkSize = Math.min(MAX_READ_SIZE, size - offset);
            byte[] chunk = read(chunkSize);
            if (chunk == null || chunk.length != chunkSize) {
                throw new QuantisException("Quantis returned " + (chunk == null ? "null" : chunk.length)
                        + " bytes when " + chunkSize + " were requested.");
            }
            System.arraycopy(chunk, 0, output, offset, chunkSize);
            offset += chunkSize;
        }
        return output;
    }

    private static native int QuantisCount(int deviceType) throws QuantisException;

    private static native byte[] QuantisRead(int deviceType, int deviceNumber, int size)
            throws QuantisException;
}
