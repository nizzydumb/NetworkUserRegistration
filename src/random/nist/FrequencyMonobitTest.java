package random.nist;

/** Java port of upstream src/frequency.c. */
public final class FrequencyMonobitTest implements NistRandomnessTest {
    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        requireSequence(sequence);
        long sum = 0;
        for (int index = 0; index < sequence.length(); index++) {
            sum += 2L * sequence.bitAt(index) - 1L;
        }

        double observed = Math.abs(sum) / Math.sqrt(sequence.length());
        double pValue = NistMath.erfc(observed / Math.sqrt(2.0));
        return NistTestResult.of(
                "Frequency (Monobit)",
                pValue,
                significanceLevel,
                "partialSum=" + sum + ", normalizedSum=" + ((double) sum / sequence.length())
        );
    }

    private void requireSequence(BitSequence sequence) {
        if (sequence == null || sequence.length() == 0) {
            throw new IllegalArgumentException("Frequency test requires at least one bit.");
        }
    }
}
