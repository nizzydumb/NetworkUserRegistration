package random.nist;

/** Java port of upstream src/blockFrequency.c. */
public final class BlockFrequencyTest implements NistRandomnessTest {
    private final int blockLength;

    public BlockFrequencyTest(int blockLength) {
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Block length must be positive.");
        }
        this.blockLength = blockLength;
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int blocks = sequence.length() / blockLength;
        if (blocks == 0) {
            throw new IllegalArgumentException("Sequence must contain at least one complete block.");
        }
        double sum = 0.0;
        for (int block = 0; block < blocks; block++) {
            int ones = 0;
            for (int offset = 0; offset < blockLength; offset++) {
                ones += sequence.bitAt(block * blockLength + offset);
            }
            double difference = (double) ones / blockLength - 0.5;
            sum += difference * difference;
        }
        double chiSquared = 4.0 * blockLength * sum;
        double pValue = NistMath.regularizedGammaQ(blocks / 2.0, chiSquared / 2.0);
        return NistTestResult.of("Block Frequency", pValue, significanceLevel,
                "chiSquared=" + chiSquared + ", blocks=" + blocks + ", blockLength=" + blockLength);
    }
}
