package random;

/** Creates the entropy source selected through JVM system properties. */
public final class EntropySources {
    private EntropySources() {
    }

    public static EntropySource configured() {
        String provider = System.getProperty("entropy.provider", "system").trim().toLowerCase();
        return switch (provider) {
            case "system" -> new SystemEntropySource();
            case "quantis" -> new QuantisEntropySource(
                    QuantisDeviceType.fromConfiguration(System.getProperty("quantis.device.type", "PCIE")),
                    Integer.getInteger("quantis.device.number", 0)
            );
            default -> throw new IllegalArgumentException("Unknown entropy provider: " + provider);
        };
    }
}
