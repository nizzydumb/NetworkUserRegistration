package random.nist;

import java.util.ArrayList;
import java.util.List;

/** Java port of upstream src/randomExcursions.c. */
public final class RandomExcursionsTest implements NistRandomnessTest {
    private static final int[] STATES = {-4, -3, -2, -1, 1, 2, 3, 4};
    private static final double[][] PROBABILITIES = {
            {},
            {0.5, 0.25, 0.125, 0.0625, 0.03125, 0.03125},
            {0.75, 0.0625, 0.046875, 0.03515625, 0.0263671875, 0.0791015625},
            {0.8333333333, 0.02777777778, 0.02314814815, 0.01929012346, 0.01607510288, 0.0803755143},
            {0.875, 0.015625, 0.013671875, 0.01196289063, 0.01046752930, 0.0732727051}
    };

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        return testAll(sequence, significanceLevel).getFirst();
    }

    @Override
    public List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        Walk walk = Walk.from(sequence);
        double constraint = Math.max(0.005 * Math.sqrt(sequence.length()), 500.0);
        List<NistTestResult> results = new ArrayList<>(STATES.length);
        if (walk.cycles() < constraint) {
            for (int state : STATES) {
                results.add(NistTestResult.of("Random Excursions state " + state, 0.0, significanceLevel,
                        "notApplicable=true, cycles=" + walk.cycles() + ", required=" + constraint));
            }
            return List.copyOf(results);
        }

        int[][] frequencies = new int[STATES.length][6];
        int cycleStart = 0;
        for (int cycleEnd : walk.cycleEnds()) {
            int[] visits = new int[STATES.length];
            for (int index = cycleStart; index < cycleEnd; index++) {
                int stateIndex = stateIndex(walk.sums()[index]);
                if (stateIndex >= 0) {
                    visits[stateIndex]++;
                }
            }
            for (int state = 0; state < STATES.length; state++) {
                frequencies[state][Math.min(visits[state], 5)]++;
            }
            cycleStart = cycleEnd + 1;
        }
        for (int stateIndex = 0; stateIndex < STATES.length; stateIndex++) {
            int absoluteState = Math.abs(STATES[stateIndex]);
            double chiSquared = 0.0;
            for (int visits = 0; visits < 6; visits++) {
                double expected = walk.cycles() * PROBABILITIES[absoluteState][visits];
                double difference = frequencies[stateIndex][visits] - expected;
                chiSquared += difference * difference / expected;
            }
            double pValue = NistMath.regularizedGammaQ(2.5, chiSquared / 2.0);
            results.add(NistTestResult.of("Random Excursions state " + STATES[stateIndex], pValue,
                    significanceLevel, "cycles=" + walk.cycles() + ", chiSquared=" + chiSquared));
        }
        return List.copyOf(results);
    }

    private int stateIndex(int state) {
        return state >= -4 && state <= -1 ? state + 4 : state >= 1 && state <= 4 ? state + 3 : -1;
    }

    static record Walk(int[] sums, List<Integer> cycleEnds, int cycles) {
        static Walk from(BitSequence sequence) {
            if (sequence.length() == 0) {
                throw new IllegalArgumentException("Random excursions tests require a non-empty sequence.");
            }
            int[] sums = new int[sequence.length()];
            List<Integer> cycleEnds = new ArrayList<>();
            sums[0] = 2 * sequence.bitAt(0) - 1;
            for (int index = 1; index < sequence.length(); index++) {
                sums[index] = sums[index - 1] + 2 * sequence.bitAt(index) - 1;
                if (sums[index] == 0) {
                    cycleEnds.add(index);
                }
            }
            if (sums[sums.length - 1] != 0) {
                cycleEnds.add(sums.length);
            }
            return new Walk(sums, List.copyOf(cycleEnds), cycleEnds.size());
        }
    }
}
