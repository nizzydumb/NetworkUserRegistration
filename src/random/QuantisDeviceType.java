package random;

import com.idquantique.quantis.Quantis;

import java.util.Locale;

/** Quantis hardware families and the transport each one requires. */
public enum QuantisDeviceType {
    PCIE(Integration.JNI, "QUANTIS_DEVICE_PCI"),
    USB(Integration.JNI, "QUANTIS_DEVICE_USB"),
    QRNG_CHIP(Integration.EMBEDDED, null),
    APPLIANCE(Integration.NETWORK, null),
    EVALUATION_KIT(Integration.VENDOR_SPECIFIC, null);

    private final Integration integration;
    private final String vendorJniName;

    QuantisDeviceType(Integration integration, String vendorJniName) {
        this.integration = integration;
        this.vendorJniName = vendorJniName;
    }

    public Integration integration() {
        return integration;
    }

    public boolean supportsLocalJni() {
        return integration == Integration.JNI;
    }

    String vendorJniName() {
        if (!supportsLocalJni()) {
            throw new IllegalStateException(this + " does not use the local Quantis JNI wrapper.");
        }
        return vendorJniName;
    }

    Quantis.QuantisDeviceType nativeType() {
        return switch (this) {
            case PCIE -> Quantis.QuantisDeviceType.QUANTIS_DEVICE_PCI;
            case USB -> Quantis.QuantisDeviceType.QUANTIS_DEVICE_USB;
            default -> throw new IllegalStateException(this + " does not use the local Quantis JNI wrapper.");
        };
    }

    public static QuantisDeviceType fromConfiguration(String value) {
        String normalized = value == null ? "PCIE" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PCI", "PCIE", "PCI-E" -> PCIE;
            case "USB" -> USB;
            case "CHIP", "QRNG_CHIP", "EMBEDDED" -> QRNG_CHIP;
            case "APPLIANCE", "NETWORK" -> APPLIANCE;
            case "EVALUATION_KIT", "EVAL_KIT", "EVAL" -> EVALUATION_KIT;
            default -> throw new IllegalArgumentException("Unsupported Quantis device type: " + value);
        };
    }

    public enum Integration {
        JNI,
        EMBEDDED,
        NETWORK,
        VENDOR_SPECIFIC
    }
}
