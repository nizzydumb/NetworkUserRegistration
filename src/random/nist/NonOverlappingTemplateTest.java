package random.nist;

import java.util.ArrayList;
import java.util.List;

/** Java port of upstream src/nonOverlappingTemplateMatchings.c. */
public final class NonOverlappingTemplateTest implements NistRandomnessTest {
    private static final int BLOCKS = 8;
    private final int templateLength;
    private final List<Integer> templates;

    public NonOverlappingTemplateTest(int templateLength) {
        if (templateLength < 2 || templateLength > 21) {
            throw new IllegalArgumentException("Non-overlapping template length must be between 2 and 21.");
        }
        this.templateLength = templateLength;
        this.templates = unborderedTemplates(templateLength);
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        return testAll(sequence, significanceLevel).getFirst();
    }

    @Override
    public List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        int blockLength = sequence.length() / BLOCKS;
        if (blockLength < templateLength) {
            throw new IllegalArgumentException("Sequence is too short for non-overlapping template testing.");
        }
        double twoToM = Math.scalb(1.0, templateLength);
        double lambda = (blockLength - templateLength + 1.0) / twoToM;
        double variance = blockLength * (1.0 / twoToM
                - (2.0 * templateLength - 1.0) / (twoToM * twoToM));
        List<NistTestResult> results = new ArrayList<>(templates.size());
        for (int templateIndex = 0; templateIndex < templates.size(); templateIndex++) {
            int template = templates.get(templateIndex);
            int[] matches = new int[BLOCKS];
            for (int block = 0; block < BLOCKS; block++) {
                int end = blockLength - templateLength;
                for (int offset = 0; offset <= end; offset++) {
                    if (matches(sequence, block * blockLength + offset, template)) {
                        matches[block]++;
                        offset += templateLength - 1;
                    }
                }
            }
            double chiSquared = 0.0;
            for (int count : matches) {
                double difference = count - lambda;
                chiSquared += difference * difference / variance;
            }
            double pValue = NistMath.regularizedGammaQ(BLOCKS / 2.0, chiSquared / 2.0);
            results.add(NistTestResult.of("Non-overlapping Template " + binary(template), pValue,
                    significanceLevel, "templateIndex=" + templateIndex + ", chiSquared=" + chiSquared));
        }
        return List.copyOf(results);
    }

    private boolean matches(BitSequence sequence, int start, int template) {
        for (int offset = 0; offset < templateLength; offset++) {
            int expected = (template >>> (templateLength - 1 - offset)) & 1;
            if (sequence.bitAt(start + offset) != expected) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> unborderedTemplates(int length) {
        List<Integer> result = new ArrayList<>();
        for (int candidate = 0; candidate < (1 << length); candidate++) {
            boolean bordered = false;
            for (int border = 1; border < length; border++) {
                int prefix = candidate >>> (length - border);
                int suffix = candidate & ((1 << border) - 1);
                if (prefix == suffix) {
                    bordered = true;
                    break;
                }
            }
            if (!bordered) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private String binary(int template) {
        return String.format("%" + templateLength + "s", Integer.toBinaryString(template)).replace(' ', '0');
    }
}
