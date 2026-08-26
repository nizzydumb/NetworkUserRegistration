package random.nist;

import java.util.Arrays;

/** Java port of upstream src/linearComplexity.c using Berlekamp-Massey over GF(2). */
public final class LinearComplexityTest implements NistRandomnessTest {
    private static final double[] PROBABILITIES = {
            0.01047, 0.03125, 0.12500, 0.50000, 0.25000, 0.06250, 0.020833
    };
    private final int blockLength;

    public LinearComplexityTest(int blockLength) {
        if (blockLength <= 0) {
            throw new IllegalArgumentException("Linear-complexity block length must be positive.");
        }
        this.blockLength = blockLength;
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int blocks = sequence.length() / blockLength;
        if (blocks == 0) {
            throw new IllegalArgumentException("Linear-complexity test requires at least one complete block.");
        }
        int[] frequencies = new int[7];
        for (int block = 0; block < blocks; block++) {
            int complexity = berlekampMassey(sequence, block * blockLength);
            int paritySign = (blockLength % 2 == 0) ? 1 : -1;
            int meanSign = ((blockLength + 1) % 2 == 0) ? -1 : 1;
            double mean = blockLength / 2.0 + (9.0 + meanSign) / 36.0
                    - Math.scalb(1.0, -blockLength) * (blockLength / 3.0 + 2.0 / 9.0);
            double transformed = paritySign * (complexity - mean) + 2.0 / 9.0;
            int category = transformed <= -2.5 ? 0 : transformed <= -1.5 ? 1 : transformed <= -0.5 ? 2
                    : transformed <= 0.5 ? 3 : transformed <= 1.5 ? 4 : transformed <= 2.5 ? 5 : 6;
            frequencies[category]++;
        }
        double chiSquared = 0.0;
        for (int index = 0; index < frequencies.length; index++) {
            double expected = blocks * PROBABILITIES[index];
            double difference = frequencies[index] - expected;
            chiSquared += difference * difference / expected;
        }
        double pValue = NistMath.regularizedGammaQ(3.0, chiSquared / 2.0);
        return NistTestResult.of("Linear Complexity", pValue, significanceLevel,
                "blocks=" + blocks + ", frequencies=" + Arrays.toString(frequencies)
                        + ", chiSquared=" + chiSquared);
    }

    private int berlekampMassey(BitSequence sequence, int start) {
        int[] previous = new int[blockLength];
        int[] connection = new int[blockLength];
        int[] saved = new int[blockLength];
        int[] shifted = new int[blockLength];
        previous[0] = 1;
        connection[0] = 1;
        int complexity = 0;
        int lastUpdate = -1;
        for (int position = 0; position < blockLength; position++) {
            int discrepancy = sequence.bitAt(start + position);
            for (int index = 1; index <= complexity; index++) {
                discrepancy += connection[index] * sequence.bitAt(start + position - index);
            }
            discrepancy %= 2;
            if (discrepancy == 1) {
                System.arraycopy(connection, 0, saved, 0, blockLength);
                Arrays.fill(shifted, 0);
                int shift = position - lastUpdate;
                for (int index = 0; index + shift < blockLength; index++) {
                    if (previous[index] == 1) {
                        shifted[index + shift] = 1;
                    }
                }
                for (int index = 0; index < blockLength; index++) {
                    connection[index] ^= shifted[index];
                }
                if (complexity <= position / 2) {
                    complexity = position + 1 - complexity;
                    lastUpdate = position;
                    System.arraycopy(saved, 0, previous, 0, blockLength);
                }
            }
        }
        return complexity;
    }
}
