package random.nist;

/** Java port of upstream src/approximateEntropy.c. */
public final class ApproximateEntropyTest implements NistRandomnessTest {
    private final int blockLength;

    public ApproximateEntropyTest(int blockLength) {
        if (blockLength < 1 || blockLength > 25) {
            throw new IllegalArgumentException("Approximate-entropy block length must be between 1 and 25.");
        }
        this.blockLength = blockLength;
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int length = sequence.length();
        if (length == 0) {
            throw new IllegalArgumentException("Approximate-entropy test requires a non-empty sequence.");
        }
        double phiM = phi(sequence, blockLength);
        double phiMPlusOne = phi(sequence, blockLength + 1);
        double approximateEntropy = phiM - phiMPlusOne;
        double chiSquared = 2.0 * length * (Math.log(2.0) - approximateEntropy);
        double pValue = NistMath.regularizedGammaQ(Math.scalb(1.0, blockLength - 1), chiSquared / 2.0);
        return NistTestResult.of("Approximate Entropy", pValue, significanceLevel,
                "chiSquared=" + chiSquared + ", phiM=" + phiM + ", phiMPlusOne=" + phiMPlusOne);
    }

    private double phi(BitSequence sequence, int size) {
        int[] counts = new int[1 << size];
        int length = sequence.length();
        for (int start = 0; start < length; start++) {
            int pattern = 0;
            for (int offset = 0; offset < size; offset++) {
                pattern = (pattern << 1) | sequence.bitAt((start + offset) % length);
            }
            counts[pattern]++;
        }
        double sum = 0.0;
        for (int count : counts) {
            if (count > 0) {
                sum += count * Math.log((double) count / length);
            }
        }
        return sum / length;
    }
}
