package random.nist;

/** Java port of upstream src/runs.c. */
public final class RunsTest implements NistRandomnessTest {
    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        requireSequence(sequence);
        int ones = 0;
        for (int index = 0; index < sequence.length(); index++) {
            ones += sequence.bitAt(index);
        }

        int length = sequence.length();
        double proportion = (double) ones / length;
        if (Math.abs(proportion - 0.5) > 2.0 / Math.sqrt(length)) {
            return NistTestResult.of(
                    "Runs",
                    0.0,
                    significanceLevel,
                    "frequency prerequisite not met; proportion=" + proportion
            );
        }

        int runs = 1;
        for (int index = 1; index < length; index++) {
            if (sequence.bitAt(index) != sequence.bitAt(index - 1)) {
                runs++;
            }
        }

        double denominator = 2.0 * proportion * (1.0 - proportion) * Math.sqrt(2.0 * length);
        double argument = Math.abs(runs - 2.0 * length * proportion * (1.0 - proportion)) / denominator;
        double pValue = NistMath.erfc(argument);
        return NistTestResult.of(
                "Runs",
                pValue,
                significanceLevel,
                "proportion=" + proportion + ", observedRuns=" + runs
        );
    }

    private void requireSequence(BitSequence sequence) {
        if (sequence == null || sequence.length() < 2) {
            throw new IllegalArgumentException("Runs test requires at least two bits.");
        }
    }
}
