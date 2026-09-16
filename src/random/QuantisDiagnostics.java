package random;

import random.nist.NistSuiteResult;
import random.nist.NistTestResult;
import random.nist.NistTestSuite;

import java.util.Locale;

/** Command-line diagnostic for the local Quantis JNI connection and random stream. */
public final class QuantisDiagnostics {
    private static final int NIST_SAMPLE_BYTES = NistTestSuite.RECOMMENDED_STREAM_LENGTH / Byte.SIZE;

    private QuantisDiagnostics() {
    }

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        QuantisDeviceStatus status =
                QuantisDeviceDetector.check(QuantisDeviceType.USB, arguments.deviceNumber());

        System.out.println("Quantis USB device #" + arguments.deviceNumber());
        System.out.println("Status: " + status.status());
        System.out.println("Detected USB devices: " + status.detectedDeviceCount());
        System.out.println("Details: " + status.message());

        if (!arguments.runNist()) {
            System.out.println("NIST suite not requested. Add --nist to capture and test a 1,000,000-bit sample.");
            return;
        }
        if (!status.connected()) {
            System.out.println("NIST suite skipped because the requested Quantis device is unavailable.");
            return;
        }

        runNistSuite(arguments.deviceNumber());
    }

    private static void runNistSuite(int deviceNumber) {
        System.out.printf(
                Locale.ROOT,
                "%nCapturing %,d bytes (%,d bits) from Quantis USB device #%d...%n",
                NIST_SAMPLE_BYTES,
                NistTestSuite.RECOMMENDED_STREAM_LENGTH,
                deviceNumber
        );
        QuantisEntropySource entropySource = new QuantisEntropySource(QuantisDeviceType.USB, deviceNumber);
        byte[] sample = entropySource.nextBytes(NIST_SAMPLE_BYTES);

        System.out.println("Running NIST SP 800-22 first-level tests at alpha="
                + NistTestSuite.DEFAULT_SIGNIFICANCE_LEVEL + "...");
        NistSuiteResult report = NistTestSuite.standard().run(sample);
        for (NistTestResult result : report.results()) {
            System.out.printf(
                    Locale.ROOT,
                    "%-48s p=%-12.8f %s  %s%n",
                    result.testName(),
                    result.pValue(),
                    result.passed() ? "PASS" : "FAIL",
                    result.details()
            );
        }

        System.out.printf(
                Locale.ROOT,
                "%nNIST result: %d passed, %d failed, %d total.%n",
                report.passedResults(),
                report.failedResults(),
                report.results().size()
        );
        System.out.println(report.allPassed()
                ? "Sample result: all applicable first-level results passed."
                : "Sample result: one or more first-level results failed or were not applicable.");
        System.out.println("A single stream is a diagnostic sample, not NIST certification or device validation.");
    }

    private record Arguments(int deviceNumber, boolean runNist) {
        private static Arguments parse(String[] args) {
            int deviceNumber = 0;
            boolean deviceNumberProvided = false;
            boolean runNist = false;
            for (String argument : args) {
                if ("--nist".equalsIgnoreCase(argument)) {
                    runNist = true;
                } else if (!deviceNumberProvided) {
                    deviceNumber = Integer.parseInt(argument);
                    if (deviceNumber < 0) {
                        throw new IllegalArgumentException("Quantis device number cannot be negative.");
                    }
                    deviceNumberProvided = true;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + argument
                            + ". Usage: QuantisDiagnostics [device-number] [--nist]");
                }
            }
            return new Arguments(deviceNumber, runNist);
        }
    }
}
