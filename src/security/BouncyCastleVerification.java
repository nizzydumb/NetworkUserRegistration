package security;

import java.security.MessageDigest;
import java.security.Provider;

/** Simple offline smoke test for the bundled Bouncy Castle provider. */
public final class BouncyCastleVerification {
    private BouncyCastleVerification() {
    }

    public static void main(String[] args) throws Exception {
        Provider provider = BouncyCastleSupport.ensureRegistered();
        MessageDigest digest = MessageDigest.getInstance("SHA-256", BouncyCastleSupport.PROVIDER_NAME);
        if (digest.getProvider() != provider) {
            throw new AssertionError("SHA-256 was not supplied by the registered Bouncy Castle provider.");
        }
        System.out.println("Bouncy Castle provider verified: " + provider.getName() + " "
                + provider.getVersionStr());
    }
}
