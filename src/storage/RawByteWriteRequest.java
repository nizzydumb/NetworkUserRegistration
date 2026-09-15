package storage;

import java.util.HexFormat;

/** Immutable byte-offset write request for a raw device or disk-image file. */
public record RawByteWriteRequest(int driveNumber, long byteOffset, byte[] bytes) {
    public RawByteWriteRequest {
        if (driveNumber < 0) {
            throw new IllegalArgumentException("Drive number cannot be negative.");
        }
        if (byteOffset < 0) {
            throw new IllegalArgumentException("Byte offset cannot be negative.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("At least one byte is required.");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    public String hexBytes() {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
