package random;

/** Supplies cryptographically useful random bytes from a named source. */
public interface EntropySource {
    byte[] nextBytes(int length);

    boolean isAvailable();

    String name();
}
