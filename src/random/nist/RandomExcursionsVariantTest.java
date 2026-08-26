package random.nist;

import java.util.ArrayList;
import java.util.List;

/** Java port of upstream src/randomExcursionsVariant.c. */
public final class RandomExcursionsVariantTest implements NistRandomnessTest {
    private static final int[] STATES = {-9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        return testAll(sequence, significanceLevel).getFirst();
    }

    @Override
    public List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        RandomExcursionsTest.Walk walk = RandomExcursionsTest.Walk.from(sequence);
        int constraint = (int) Math.max(0.005 * Math.sqrt(sequence.length()), 500.0);
        List<NistTestResult> results = new ArrayList<>(STATES.length);
        for (int state : STATES) {
            if (walk.cycles() < constraint) {
                results.add(NistTestResult.of("Random Excursions Variant state " + state, 0.0,
                        significanceLevel, "notApplicable=true, cycles=" + walk.cycles()
                                + ", required=" + constraint));
                continue;
            }
            int visits = 0;
            for (int sum : walk.sums()) {
                if (sum == state) {
                    visits++;
                }
            }
            double denominator = Math.sqrt(2.0 * walk.cycles() * (4.0 * Math.abs(state) - 2.0));
            double pValue = NistMath.erfc(Math.abs(visits - walk.cycles()) / denominator);
            results.add(NistTestResult.of("Random Excursions Variant state " + state, pValue,
                    significanceLevel, "cycles=" + walk.cycles() + ", visits=" + visits));
        }
        return List.copyOf(results);
    }
}
