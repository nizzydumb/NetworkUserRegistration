package random.nist;

import java.util.List;

/** Java port of upstream src/serial.c. */
public final class SerialTest implements NistRandomnessTest {
    private final int blockLength;

    public SerialTest(int blockLength) {
        if (blockLength < 2 || blockLength > 25) {
            throw new IllegalArgumentException("Serial-test block length must be between 2 and 25.");
        }
        this.blockLength = blockLength;
    }

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        return testAll(sequence, significanceLevel).getFirst();
    }

    @Override
    public List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        double psiM = psiSquared(sequence, blockLength);
        double psiM1 = psiSquared(sequence, blockLength - 1);
        double psiM2 = psiSquared(sequence, blockLength - 2);
        double delta1 = psiM - psiM1;
        double delta2 = psiM - 2.0 * psiM1 + psiM2;
        double p1 = NistMath.regularizedGammaQ(Math.scalb(1.0, blockLength - 2), delta1 / 2.0);
        double p2 = NistMath.regularizedGammaQ(Math.scalb(1.0, blockLength - 3), delta2 / 2.0);
        String details = "psiM=" + psiM + ", psiM-1=" + psiM1 + ", psiM-2=" + psiM2;
        return List.of(
                NistTestResult.of("Serial (Delta 1)", p1, significanceLevel, details),
                NistTestResult.of("Serial (Delta 2)", p2, significanceLevel, details)
        );
    }

    private double psiSquared(BitSequence sequence, int size) {
        if (size <= 0) {
            return 0.0;
        }
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
            sum += (double) count * count;
        }
        return sum * Math.scalb(1.0, size) / length - length;
    }
}
