package random.nist;

import java.util.Arrays;

/** Java port of upstream src/overlappingTemplateMatchings.c for the all-ones template. */
public final class OverlappingTemplateTest implements NistRandomnessTest {
    private static final int SUBSTRING_LENGTH = 1032;
    private final int templateLength;

    public OverlappingTemplateTest(int templateLength) {
        if (templateLength <= 0 || templateLength > SUBSTRING_LENGTH) {
            throw new IllegalArgumentException("Invalid overlapping-template length.");
        }
        this.templateLength = templateLength;
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int blocks = sequence.length() / SUBSTRING_LENGTH;
        if (blocks == 0) {
            throw new IllegalArgumentException("Overlapping-template test requires at least 1032 bits.");
        }
        double lambda = (double) (SUBSTRING_LENGTH - templateLength + 1) / Math.scalb(1.0, templateLength);
        double eta = lambda / 2.0;
        double[] probabilities = new double[6];
        double sum = 0.0;
        for (int index = 0; index < 5; index++) {
            probabilities[index] = probability(index, eta);
            sum += probabilities[index];
        }
        probabilities[5] = 1.0 - sum;

        int[] frequencies = new int[6];
        for (int block = 0; block < blocks; block++) {
            int matches = 0;
            for (int start = 0; start <= SUBSTRING_LENGTH - templateLength; start++) {
                boolean match = true;
                for (int offset = 0; offset < templateLength; offset++) {
                    if (sequence.bitAt(block * SUBSTRING_LENGTH + start + offset) != 1) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    matches++;
                }
            }
            frequencies[Math.min(matches, 5)]++;
        }

        double chiSquared = 0.0;
        for (int index = 0; index < frequencies.length; index++) {
            double expected = blocks * probabilities[index];
            double difference = frequencies[index] - expected;
            chiSquared += difference * difference / expected;
        }
        double pValue = NistMath.regularizedGammaQ(2.5, chiSquared / 2.0);
        return NistTestResult.of("Overlapping Template", pValue, significanceLevel,
                "blocks=" + blocks + ", frequencies=" + Arrays.toString(frequencies)
                        + ", chiSquared=" + chiSquared + ", lambda=" + lambda);
    }

    private double probability(int occurrences, double eta) {
        if (occurrences == 0) {
            return Math.exp(-eta);
        }
        double sum = 0.0;
        for (int index = 1; index <= occurrences; index++) {
            sum += Math.exp(-eta - occurrences * Math.log(2.0) + index * Math.log(eta)
                    - NistMath.logGamma(index + 1.0) + NistMath.logGamma(occurrences)
                    - NistMath.logGamma(index) - NistMath.logGamma(occurrences - index + 1.0));
        }
        return sum;
    }
}
