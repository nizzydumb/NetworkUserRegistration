package random.nist;

import java.util.ArrayList;
import java.util.List;

/** Pure-Java NIST SP 800-22 Rev. 1a suite configured with the reference defaults. */
public final class NistTestSuite {
    public static final int RECOMMENDED_STREAM_LENGTH = 1_000_000;
    public static final double DEFAULT_SIGNIFICANCE_LEVEL = 0.01;

    private final List<NistRandomnessTest> tests;

    private NistTestSuite(List<NistRandomnessTest> tests) {
        this.tests = List.copyOf(tests);
    }

    public static NistTestSuite standard() {
        return new NistTestSuite(List.of(
                new FrequencyMonobitTest(),
                new BlockFrequencyTest(128),
                new CumulativeSumsTest(),
                new RunsTest(),
                new LongestRunOfOnesTest(),
                new BinaryMatrixRankTest(),
                new DiscreteFourierTransformTest(),
                new NonOverlappingTemplateTest(9),
                new OverlappingTemplateTest(9),
                new UniversalStatisticalTest(),
                new ApproximateEntropyTest(10),
                new RandomExcursionsTest(),
                new RandomExcursionsVariantTest(),
                new SerialTest(16),
                new LinearComplexityTest(500)
        ));
    }

    public NistSuiteResult run(byte[] randomBytes) {
        return run(BitSequence.fromBytes(randomBytes), DEFAULT_SIGNIFICANCE_LEVEL);
    }

    public NistSuiteResult run(BitSequence sequence, double significanceLevel) {
        if (sequence == null || sequence.length() < RECOMMENDED_STREAM_LENGTH) {
            throw new IllegalArgumentException("The complete NIST suite requires at least "
                    + RECOMMENDED_STREAM_LENGTH + " bits (" + (RECOMMENDED_STREAM_LENGTH / 8) + " bytes).");
        }
        if (!(significanceLevel > 0.0 && significanceLevel < 1.0)) {
            throw new IllegalArgumentException("Significance level must be between 0 and 1.");
        }

        List<NistTestResult> results = new ArrayList<>();
        for (NistRandomnessTest test : tests) {
            results.addAll(test.testAll(sequence, significanceLevel));
        }
        return new NistSuiteResult(results, significanceLevel);
    }
}
