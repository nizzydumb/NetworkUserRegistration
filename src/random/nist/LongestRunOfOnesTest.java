package random.nist;

import java.util.Arrays;

/** Java port of upstream src/longestRunOfOnes.c. */
public final class LongestRunOfOnesTest implements NistRandomnessTest {
    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int length = sequence.length();
        if (length < 128) {
            throw new IllegalArgumentException("Longest-run test requires at least 128 bits.");
        }

        int blockLength;
        int[] boundaries;
        double[] probabilities;
        if (length < 6_272) {
            blockLength = 8;
            boundaries = new int[]{1, 2, 3, 4};
            probabilities = new double[]{0.21484375, 0.3671875, 0.23046875, 0.1875};
        } else if (length < 750_000) {
            blockLength = 128;
            boundaries = new int[]{4, 5, 6, 7, 8, 9};
            probabilities = new double[]{0.1174035788, 0.242955959, 0.249363483,
                    0.17517706, 0.102701071, 0.112398847};
        } else {
            blockLength = 10_000;
            boundaries = new int[]{10, 11, 12, 13, 14, 15, 16};
            probabilities = new double[]{0.0882, 0.2092, 0.2483, 0.1933, 0.1208, 0.0675, 0.0727};
        }

        int blocks = length / blockLength;
        int[] frequencies = new int[probabilities.length];
        for (int block = 0; block < blocks; block++) {
            int run = 0;
            int longest = 0;
            for (int offset = 0; offset < blockLength; offset++) {
                if (sequence.bitAt(block * blockLength + offset) == 1) {
                    longest = Math.max(longest, ++run);
                } else {
                    run = 0;
                }
            }
            int category = longest <= boundaries[0] ? 0
                    : longest >= boundaries[boundaries.length - 1] ? boundaries.length - 1
                    : longest - boundaries[0];
            frequencies[category]++;
        }

        double chiSquared = 0.0;
        for (int index = 0; index < frequencies.length; index++) {
            double expected = blocks * probabilities[index];
            double difference = frequencies[index] - expected;
            chiSquared += difference * difference / expected;
        }
        int degreesOfFreedom = probabilities.length - 1;
        double pValue = NistMath.regularizedGammaQ(degreesOfFreedom / 2.0, chiSquared / 2.0);
        return NistTestResult.of("Longest Run of Ones", pValue, significanceLevel,
                "chiSquared=" + chiSquared + ", frequencies=" + Arrays.toString(frequencies));
    }
}
