package random.nist;

import java.util.List;

/** Java port of upstream src/cusum.c. */
public final class CumulativeSumsTest implements NistRandomnessTest {
    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        return testAll(sequence, significanceLevel).getFirst();
    }

    @Override
    public List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        if (sequence == null || sequence.length() == 0) {
            throw new IllegalArgumentException("Cumulative sums test requires at least one bit.");
        }
        int sum = 0;
        int supremum = 0;
        int infimum = 0;
        for (int index = 0; index < sequence.length(); index++) {
            sum += sequence.bitAt(index) == 1 ? 1 : -1;
            supremum = Math.max(supremum, sum);
            infimum = Math.min(infimum, sum);
        }
        int forward = Math.max(supremum, -infimum);
        int reverse = Math.max(supremum - sum, sum - infimum);
        return List.of(
                result("Cumulative Sums (Forward)", sequence.length(), forward, significanceLevel),
                result("Cumulative Sums (Reverse)", sequence.length(), reverse, significanceLevel)
        );
    }

    private NistTestResult result(String name, int length, int maximum, double alpha) {
        if (maximum == 0) {
            return NistTestResult.of(name, 1.0, alpha, "maximumPartialSum=0");
        }
        double root = Math.sqrt(length);
        double sum1 = 0.0;
        for (int k = (-length / maximum + 1) / 4; k <= (length / maximum - 1) / 4; k++) {
            sum1 += NistMath.normal(((4.0 * k + 1.0) * maximum) / root);
            sum1 -= NistMath.normal(((4.0 * k - 1.0) * maximum) / root);
        }
        double sum2 = 0.0;
        for (int k = (-length / maximum - 3) / 4; k <= (length / maximum - 1) / 4; k++) {
            sum2 += NistMath.normal(((4.0 * k + 3.0) * maximum) / root);
            sum2 -= NistMath.normal(((4.0 * k + 1.0) * maximum) / root);
        }
        double pValue = 1.0 - sum1 + sum2;
        return NistTestResult.of(name, pValue, alpha, "maximumPartialSum=" + maximum);
    }
}
