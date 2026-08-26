package random;

import java.security.SecureRandom;

/** Default source for development and for installations without Quantis hardware. */
public final class SystemEntropySource implements EntropySource {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public byte[] nextBytes(int length) {
        validateLength(length);
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String name() {
        return "System SecureRandom";
    }

    private void validateLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Random byte length cannot be negative.");
        }
    }
}
