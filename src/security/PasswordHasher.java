package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 120_000;

    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(char[] password, String saltBase64, int iterations, String algorithm) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash password.", exception);
        }
    }

    public boolean matches(char[] password, UserCredentials credentials) {
        String candidateHash = hash(
                password,
                credentials.passwordSalt(),
                credentials.iterations(),
                credentials.algorithm()
        );
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(candidateHash),
                Base64.getDecoder().decode(credentials.passwordHash())
        );
    }
}
