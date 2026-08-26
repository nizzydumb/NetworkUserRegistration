package random.nist;

public record NistTestResult(
        String testName,
        double pValue,
        double significanceLevel,
        boolean passed,
        String details
) {
    public static NistTestResult of(String name, double pValue, double alpha, String details) {
        return new NistTestResult(name, pValue, alpha, pValue >= alpha, details);
    }
}
