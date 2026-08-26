package random.nist;

import java.util.List;

public interface NistRandomnessTest {
    NistTestResult test(BitSequence sequence, double significanceLevel);

    default List<NistTestResult> testAll(BitSequence sequence, double significanceLevel) {
        return List.of(test(sequence, significanceLevel));
    }
}
