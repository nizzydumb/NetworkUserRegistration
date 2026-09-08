package security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

/** Registers and exposes the bundled Bouncy Castle JCA provider on demand. */
public final class BouncyCastleSupport {
    public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    private BouncyCastleSupport() {
    }

    public static synchronized Provider ensureRegistered() {
        Provider provider = Security.getProvider(PROVIDER_NAME);
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
            provider = Security.getProvider(PROVIDER_NAME);
        }
        if (provider == null) {
            throw new IllegalStateException("Bouncy Castle provider registration failed.");
        }
        return provider;
    }
}
