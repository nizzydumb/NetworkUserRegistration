package random.nist;

import java.util.Arrays;

/** Java port of upstream src/universal.c (Maurer's Universal Statistical Test). */
public final class UniversalStatisticalTest implements NistRandomnessTest {
    private static final double[] EXPECTED = {
            0, 0, 0, 0, 0, 0, 5.2177052, 6.1962507, 7.1836656,
            8.1764248, 9.1723243, 10.170032, 11.168765, 12.168070,
            13.167693, 14.167488, 15.167379
    };
    private static final double[] VARIANCE = {
            0, 0, 0, 0, 0, 0, 2.954, 3.125, 3.238, 3.311, 3.356,
            3.384, 3.401, 3.410, 3.416, 3.419, 3.421
    };

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int length = sequence.length();
        int blockLength = selectBlockLength(length);
        if (blockLength < 6) {
            throw new IllegalArgumentException("Universal test requires at least 387840 bits.");
        }
        int initializationBlocks = 10 * (1 << blockLength);
        int testBlocks = length / blockLength - initializationBlocks;
        int[] lastSeen = new int[1 << blockLength];
        Arrays.fill(lastSeen, 0);
        for (int block = 1; block <= initializationBlocks; block++) {
            lastSeen[pattern(sequence, block - 1, blockLength)] = block;
        }
        double sum = 0.0;
        for (int block = initializationBlocks + 1; block <= initializationBlocks + testBlocks; block++) {
            int pattern = pattern(sequence, block - 1, blockLength);
            sum += Math.log(block - lastSeen[pattern]) / Math.log(2.0);
            lastSeen[pattern] = block;
        }
        double phi = sum / testBlocks;
        double correction = 0.7 - 0.8 / blockLength
                + (4.0 + 32.0 / blockLength) * Math.pow(testBlocks, -3.0 / blockLength) / 15.0;
        double sigma = correction * Math.sqrt(VARIANCE[blockLength] / testBlocks);
        double pValue = NistMath.erfc(Math.abs(phi - EXPECTED[blockLength]) / (Math.sqrt(2.0) * sigma));
        return NistTestResult.of("Universal Statistical", pValue, significanceLevel,
                "blockLength=" + blockLength + ", initializationBlocks=" + initializationBlocks
                        + ", testBlocks=" + testBlocks + ", phi=" + phi + ", sigma=" + sigma);
    }

    private int selectBlockLength(int length) {
        int selected = 5;
        int[] thresholds = {387840, 904960, 2068480, 4654080, 10342400, 22753280,
                49643520, 107560960, 231669760, 496435200, 1059061760};
        for (int index = 0; index < thresholds.length; index++) {
            if (length >= thresholds[index]) {
                selected = index + 6;
            }
        }
        return selected;
    }

    private int pattern(BitSequence sequence, int blockIndex, int blockLength) {
        int value = 0;
        int start = blockIndex * blockLength;
        for (int offset = 0; offset < blockLength; offset++) {
            value = (value << 1) | sequence.bitAt(start + offset);
        }
        return value;
    }
}
