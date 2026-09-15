package storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Non-destructive native/JNA verification. It never opens a physical drive. */
public final class RawByteWriterVerification {
    private RawByteWriterVerification() {}

    public static void main(String[] args) throws Exception {
        JnaPhysicalDriveService writer = new JnaPhysicalDriveService();
        Path image = Files.createTempFile("raw-byte-writer-test-", ".img");
        try {
            Files.write(image, new byte[4096]);
            byte[] expected = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
            writer.writeImageForVerification(image, 1024, expected);
            byte[] actual = Arrays.copyOfRange(Files.readAllBytes(image), 1024, 1028);
            if (!Arrays.equals(expected, actual)) throw new AssertionError("Image contents did not match.");
            expectIllegalArgument(() -> new RawByteWriteRequest(0, -1, expected), "negative offset");
            expectIllegalArgument(() -> new RawByteWriteRequest(0, 0, new byte[0]), "empty bytes");
            try {
                writer.writeImageForVerification(image, 4095, expected);
                throw new AssertionError("An out-of-range image write was accepted.");
            } catch (RawDriveException expectedFailure) {
                if (expectedFailure.errorCode() != -4) throw expectedFailure;
            }
            System.out.println("Native/JNA disk-image verification passed; no physical drive was opened.");
        } finally {
            Files.deleteIfExists(image);
        }
    }

    private static void expectIllegalArgument(Runnable operation, String scenario) {
        try { operation.run(); throw new AssertionError("Expected rejection for " + scenario); }
        catch (IllegalArgumentException expected) { /* expected */ }
    }
}
