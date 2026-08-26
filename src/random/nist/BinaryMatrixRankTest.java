package random.nist;

/** Java port of upstream src/rank.c and its GF(2) matrix operations. */
public final class BinaryMatrixRankTest implements NistRandomnessTest {
    private static final int SIZE = 32;

    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int matrices = sequence.length() / (SIZE * SIZE);
        if (matrices == 0) {
            throw new IllegalArgumentException("Binary matrix rank test requires at least 1024 bits.");
        }

        double p32 = rankProbability(32);
        double p31 = rankProbability(31);
        double p30 = 1.0 - p32 - p31;
        int fullRank = 0;
        int rank31 = 0;
        for (int matrix = 0; matrix < matrices; matrix++) {
            int rank = rank(sequence, matrix * SIZE * SIZE);
            if (rank == 32) {
                fullRank++;
            } else if (rank == 31) {
                rank31++;
            }
        }
        int rank30OrLess = matrices - fullRank - rank31;
        double chiSquared = contribution(fullRank, matrices * p32)
                + contribution(rank31, matrices * p31)
                + contribution(rank30OrLess, matrices * p30);
        double pValue = Math.exp(-chiSquared / 2.0);
        return NistTestResult.of("Binary Matrix Rank", pValue, significanceLevel,
                "matrices=" + matrices + ", rank32=" + fullRank + ", rank31=" + rank31
                        + ", rank30OrLess=" + rank30OrLess + ", chiSquared=" + chiSquared);
    }

    private int rank(BitSequence sequence, int start) {
        int[] rows = new int[SIZE];
        for (int row = 0; row < SIZE; row++) {
            int value = 0;
            for (int column = 0; column < SIZE; column++) {
                value = (value << 1) | sequence.bitAt(start + row * SIZE + column);
            }
            rows[row] = value;
        }

        int rank = 0;
        for (int column = 31; column >= 0; column--) {
            int pivot = rank;
            while (pivot < SIZE && ((rows[pivot] >>> column) & 1) == 0) {
                pivot++;
            }
            if (pivot == SIZE) {
                continue;
            }
            int temporary = rows[rank];
            rows[rank] = rows[pivot];
            rows[pivot] = temporary;
            for (int row = 0; row < SIZE; row++) {
                if (row != rank && ((rows[row] >>> column) & 1) != 0) {
                    rows[row] ^= rows[rank];
                }
            }
            rank++;
        }
        return rank;
    }

    private double rankProbability(int rank) {
        double product = 1.0;
        for (int index = 0; index < rank; index++) {
            product *= (1.0 - Math.scalb(1.0, index - SIZE)) * (1.0 - Math.scalb(1.0, index - SIZE))
                    / (1.0 - Math.scalb(1.0, index - rank));
        }
        return Math.pow(2.0, rank * (2.0 * SIZE - rank) - SIZE * SIZE) * product;
    }

    private double contribution(double observed, double expected) {
        double difference = observed - expected;
        return difference * difference / expected;
    }
}
