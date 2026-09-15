package storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Non-destructive verification that writes only to a temporary disk-image file. */
public final class RawByteWriterVerification {
    private RawByteWriterVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        WindowsRawByteWriter writer = new WindowsRawByteWriter(projectRoot.resolve("tools/write-raw-bytes.ps1"));
        Path image = Files.createTempFile("raw-byte-writer-test-", ".img");
        try {
            Files.write(image, new byte[4096]);
            byte[] expected = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
            RawWriteResult result = writer.writeImage(image, 1024, expected);
            if (!result.successful()) {
                throw new AssertionError("Image write failed: " + result.output());
            }
            byte[] actual = Arrays.copyOfRange(Files.readAllBytes(image), 1024, 1028);
            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError("Image contents did not match the requested bytes.");
            }

            expectIllegalArgument(() -> new RawByteWriteRequest(0, -1, expected), "negative offset");
            expectIllegalArgument(() -> new RawByteWriteRequest(0, 0, new byte[0]), "empty bytes");

            RawWriteResult outOfRange = writer.writeImage(image, 4095, expected);
            if (outOfRange.successful()) {
                throw new AssertionError("An out-of-range image write was unexpectedly accepted.");
            }
            System.out.println("Raw byte writer image test passed. " + result.output());
        } finally {
            Files.deleteIfExists(image);
        }
    }

    private static void expectIllegalArgument(Runnable operation, String scenario) {
        try {
            operation.run();
            throw new AssertionError("Expected rejection for " + scenario + ".");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }
}
