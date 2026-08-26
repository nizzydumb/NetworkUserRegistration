package random.nist;

import java.util.List;

/** Complete flattened output from the NIST SP 800-22 test suite. */
public record NistSuiteResult(List<NistTestResult> results, double significanceLevel) {
    public NistSuiteResult {
        results = List.copyOf(results);
    }

    public long passedResults() {
        return results.stream().filter(NistTestResult::passed).count();
    }

    public long failedResults() {
        return results.size() - passedResults();
    }

    public boolean allPassed() {
        return failedResults() == 0;
    }
}
