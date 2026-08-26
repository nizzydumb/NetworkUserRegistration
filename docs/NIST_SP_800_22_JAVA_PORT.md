# NIST SP 800-22 Java test-suite guide

## 1. Purpose and scope

This project contains a pure-Java port of the 15 statistical-test categories from NIST SP 800-22 Rev. 1a and the
NIST Statistical Test Suite (STS) 2.1.2 C reference implementation.

The tests look for statistical patterns that would be unusual in random binary data. They are diagnostic tests;
they do not prove that a generator is unpredictable, correctly designed, or cryptographically secure. NIST states
that statistical testing cannot replace analysis of a generator or cryptanalysis.

For a hardware entropy source such as Quantis, SP 800-22 complements—but does not replace—the device health tests,
the physical entropy-source model, and SP 800-90B entropy validation.

Official specification:
<https://csrc.nist.gov/pubs/sp/800/22/r1/upd1/final>

## 2. Source and migration provenance

The original C distribution is retained under `third_party/nist-sts-2.1.2`.

| Item | Value |
|---|---|
| NIST package | STS 2.1.2 |
| Download URL | `https://csrc.nist.gov/CSRC/media/Projects/Random-Bit-Generation/documents/sts-2_1_2.zip` |
| Retrieved | 2026-08-25 |
| SHA-256 | `0238D2F1D26E120E3CC748ED2D4C674CDC636DE37FC4027C76CC2A394FFF9157` |
| C build environment | WSL2, GCC, Make, `libm` |
| Java package | `random.nist` |
| Java version used for verification | JDK 26 |

The upstream source is a behavioral reference and should not be edited. Java code and verification code live in
`src/random/nist`.

## 3. How the C implementation was migrated

The migration was a manual, test-by-test reimplementation rather than a mechanical source translation.

### 3.1 Structural mapping

| C implementation | Java implementation |
|---|---|
| Global `epsilon[]` bit array | Immutable `BitSequence` |
| Global `stats[]` and `results[]` files | `NistTestResult` records |
| One C function per category | One `NistRandomnessTest` class per category |
| `calloc`/`free` working arrays | Java-owned arrays and collections |
| `cephes_igamc`, `erfc`, normal CDF | `NistMath` |
| Ogg real DFT implementation | Arbitrary-length Bluestein FFT backed by a radix-2 FFT |
| C GF(2) matrix operations | Packed 32-bit rows and XOR Gaussian elimination |
| Template files | Generated unbordered binary templates in numeric order |
| Interactive `assess` program | `NistTestSuite.standard()` API |

### 3.2 Porting procedure

Each test was migrated using the same process:

1. Read the C source, formulas, loop boundaries, bin definitions, and default parameters.
2. Run the original C suite against its supplied `data.pi` dataset.
3. Record the C p-values.
4. Implement the equivalent algorithm in Java.
5. Preserve C integer-division, circular indexing, discarded-bit, and category-boundary behavior.
6. Compare Java p-values with the C output.
7. Accept parity only when the difference was no greater than `0.0000006`, which accounts for the C report being
   rounded to six decimal places.

Fourteen categories were compared over ten independent 100,000-bit slices of `data.pi`. The Universal test was
compared using 1,000,000 bits because it is not applicable to a 100,000-bit stream. Approximately 1,871 C/Java
reference comparisons were performed.

### 3.3 Intentional implementation differences

- The Java API returns structured results instead of writing reports into test-specific directories.
- The FFT algorithm differs internally, but computes the same DFT statistic and matched the C p-values.
- Length-9 non-overlapping templates are generated as unbordered words instead of read from `template9`; their
  numeric order matches the reference file.
- Mathematical functions use a Java complementary-error-function approximation, Lanczos log-gamma, and
  convergent series/continued-fraction incomplete-gamma implementation. Their resulting p-values matched the C
  reference precision.
- Invalid input is normally rejected with `IllegalArgumentException` instead of producing an incomplete text file.

## 4. Input formats

### 4.1 Byte input

The standard suite accepts bytes:

```java
byte[] bytes = entropySource.nextBytes(125_000);
NistSuiteResult report = NistTestSuite.standard().run(bytes);
```

`BitSequence.fromBytes()` expands each byte most-significant bit first. For example, byte `0xA1` becomes:

```text
1 0 1 0 0 0 0 1
```

The complete standard suite requires at least 1,000,000 bits, equal to 125,000 bytes. Extra bytes are included in
the tested sequence; they are not automatically truncated.

### 4.2 ASCII bit input

Reference data can be loaded with:

```java
BitSequence bits = BitSequence.fromAsciiFile(path);
```

The file may contain `0`, `1`, and whitespace only. Any other character is rejected.

### 4.3 Maximum representable input

`BitSequence` uses a Java byte array with one array element per bit and indexes it with `int` values. Its absolute
representation limit is therefore `Integer.MAX_VALUE` bits. `fromBytes()` additionally limits source arrays to
`floor(Integer.MAX_VALUE / 8)`, or 268,435,455 bytes. Actual usable sizes are much smaller and depend on JVM heap,
especially because FFT and excursion tests allocate working arrays proportional to the bit count.

## 5. Common parameters and output

### 5.1 Significance level

The standard significance level is:

```text
alpha = 0.01
```

The suite accepts any finite value strictly between `0` and `1`. Values outside that interval are rejected.

Each test begins with the null hypothesis that the sequence is random with respect to the tested property.

| Result | Interpretation |
|---|---|
| `pValue >= alpha` | Do not reject the randomness hypothesis for this test |
| `pValue < alpha` | Reject the hypothesis for this test and this sequence |
| `pValue = 0` with `notApplicable=true` | Excursion test lacked enough cycles; this is not an ordinary statistical failure |

At `alpha = 0.01`, even ideal random streams are expected to fail approximately 1% of independent tests. Therefore,
`allPassed()` is a literal convenience value, not a requirement that every good generator must always satisfy.

### 5.2 Result record

Each individual result contains:

```java
public record NistTestResult(
        String testName,
        double pValue,
        double significanceLevel,
        boolean passed,
        String details
) {}
```

- `testName`: category and, where relevant, direction, template, or state.
- `pValue`: probability measure used for the hypothesis decision.
- `significanceLevel`: the selected alpha value.
- `passed`: `true` when `pValue >= significanceLevel`.
- `details`: intermediate values such as chi-square, block counts, cycles, or observed runs.

The default complete suite produces 188 individual results because several categories return multiple p-values:

```text
1 + 1 + 2 + 1 + 1 + 1 + 1 + 148 + 1 + 1 + 1 + 8 + 18 + 2 + 1 = 188
```

## 6. Standard configuration and parameter limits

The following table distinguishes the defaults used by `NistTestSuite.standard()` from the bounds enforced by the
individual Java classes. The complete suite always enforces a minimum input of 1,000,000 bits, even where an
individual class can technically execute on less data.

| Test | Java class | Standard parameter | Individual minimum input | Parameter bounds enforced by Java | Results |
|---|---|---:|---:|---|---:|
| Frequency/Monobit | `FrequencyMonobitTest` | none | 1 bit | none | 1 |
| Block Frequency | `BlockFrequencyTest` | `M=128` | `M` bits | `M >= 1`; practical maximum is input length | 1 |
| Cumulative Sums | `CumulativeSumsTest` | forward and reverse | 1 bit | none | 2 |
| Runs | `RunsTest` | none | 2 bits | none | 1 |
| Longest Run of Ones | `LongestRunOfOnesTest` | automatic `M` | 128 bits | `M` selected from 8, 128, or 10,000 | 1 |
| Binary Matrix Rank | `BinaryMatrixRankTest` | 32x32 matrices | 1,024 bits | matrix size fixed at 32x32 | 1 |
| Discrete Fourier Transform | `DiscreteFourierTransformTest` | 95% threshold | 2 bits | no user parameter | 1 |
| Non-overlapping Template | `NonOverlappingTemplateTest` | `m=9`, 148 templates, 8 blocks | `8*m` bits | constructor accepts `2 <= m <= 21`; only `m=9` is reference-validated | 148 at `m=9` |
| Overlapping Template | `OverlappingTemplateTest` | `m=9`, block `M=1032` | 1,032 bits | `1 <= m <= 1032`; reference validation uses 9 | 1 |
| Universal Statistical | `UniversalStatisticalTest` | automatic `L` | 387,840 bits | `6 <= L <= 16`, selected by input length | 1 |
| Approximate Entropy | `ApproximateEntropyTest` | `m=10` | 1 bit technically | `1 <= m <= 25`; NIST meaningful-size guidance must also be followed | 1 |
| Random Excursions | `RandomExcursionsTest` | states -4..-1 and 1..4 | 1 bit technically | applicable only with enough cycles | 8 |
| Random Excursions Variant | `RandomExcursionsVariantTest` | states -9..-1 and 1..9 | 1 bit technically | applicable only with enough cycles | 18 |
| Serial | `SerialTest` | `m=16` | non-empty sequence | `2 <= m <= 25`; practical maximum depends on heap | 2 |
| Linear Complexity | `LinearComplexityTest` | `M=500` | `M` bits | `M >= 1`; practical maximum depends on CPU and heap | 1 |

The upper bounds of 25 for Serial and Approximate Entropy prevent invalid Java bit shifts, but they are not a
promise that such values are practical. Both algorithms allocate arrays whose size grows as `2^m`. At `m=25`, a
single integer count array is approximately 128 MiB, and Approximate Entropy also evaluates `m+1`. Use the standard
values unless a separately validated configuration is required.

Likewise, non-overlapping template counts grow rapidly with `m`. The C reference caps processed templates at 148.
This Java port is validated for the standard `m=9` set of 148 templates; larger constructor values should not be
used as certified equivalents without new C/Java comparison tests and suitable resource limits.

## 7. Test-by-test meaning

### 7.1 Frequency/Monobit

Counts zeros and ones across the whole sequence. It tests whether their difference is consistent with an equal
probability of zero and one.

- Input: one bitstream.
- Main statistic: normalized partial sum.
- Output: one p-value.
- A low value indicates a global bias toward zeros or ones.

### 7.2 Block Frequency

Divides the sequence into `N=floor(n/M)` non-overlapping blocks of `M` bits and compares each block's fraction of
ones with 0.5. The final incomplete block is discarded.

- Parameter: block length `M`; standard value 128.
- Output: one chi-square-based p-value.
- A low value indicates local block-level bias even if the global zero/one balance looks reasonable.

### 7.3 Cumulative Sums

Maps zero to -1 and one to +1, then measures the largest excursion of the cumulative random walk from zero. It is
calculated in both forward and reverse directions.

- Output: two p-values.
- A low value indicates that partial sums travel unusually far from zero.

### 7.4 Runs

A run is an uninterrupted sequence of identical bits. The test compares the observed number of runs with the
number expected from the measured proportion of ones.

- Prerequisite: the zero/one frequency must be sufficiently balanced.
- Output: one p-value.
- A low value indicates oscillation that is too fast or too slow.

### 7.5 Longest Run of Ones

Divides the input into blocks and classifies the longest run of ones in each block.

| Input length `n` | Block length `M` | Categories |
|---:|---:|---|
| `128 <= n < 6,272` | 8 | <=1, 2, 3, >=4 |
| `6,272 <= n < 750,000` | 128 | <=4, 5, 6, 7, 8, >=9 |
| `n >= 750,000` | 10,000 | <=10, 11, 12, 13, 14, 15, >=16 |

- Output: one chi-square-based p-value.
- A low value indicates unusually short or long one-runs.

### 7.6 Binary Matrix Rank

Builds 32x32 matrices over GF(2), computes each rank, and groups ranks into 32, 31, and 30-or-less.

- Each matrix consumes 1,024 bits.
- Remaining bits are discarded.
- Output: one p-value.
- A low value indicates linear dependence among fixed-length subsequences.

### 7.7 Discrete Fourier Transform/Spectral

Maps bits to -1/+1, computes an arbitrary-length FFT, and counts spectral peaks below the 95% threshold
`sqrt(2.995732274*n)`.

- Expected count: `0.95*n/2` peaks.
- Output: one p-value.
- A low value indicates periodic or repetitive features in the bitstream.

### 7.8 Non-overlapping Template Matching

Splits the input into eight blocks and counts non-overlapping occurrences of each unbordered template. After a
match, scanning advances by the entire template length.

- Standard parameter: template length `m=9`.
- Standard templates: 148.
- Output: one p-value per template, therefore 148 results.
- A low value for a template indicates too many or too few non-overlapping occurrences of that pattern.

### 7.9 Overlapping Template Matching

Counts overlapping occurrences of an all-ones pattern within 1,032-bit blocks. Match counts are grouped into
0, 1, 2, 3, 4, and 5-or-more.

- Standard parameter: nine consecutive ones.
- Output: one p-value.
- A low value indicates abnormal clustering of overlapping one-runs.

### 7.10 Maurer's Universal Statistical

Measures how far apart repeated `L`-bit patterns occur. Highly compressible or repetitive sequences tend to have a
different average log-distance than random data.

`L` is selected automatically:

| Minimum input bits | `L` |
|---:|---:|
| 387,840 | 6 |
| 904,960 | 7 |
| 2,068,480 | 8 |
| 4,654,080 | 9 |
| 10,342,400 | 10 |
| 22,753,280 | 11 |
| 49,643,520 | 12 |
| 107,560,960 | 13 |
| 231,669,760 | 14 |
| 496,435,200 | 15 |
| 1,059,061,760 | 16 |

The first `Q=10*2^L` blocks initialize the last-seen table; the remaining blocks form the statistic.

- Output: one p-value.
- A low value suggests that the sequence is more compressible or repetitive than expected.

### 7.11 Approximate Entropy

Counts all circular `m`-bit and `(m+1)`-bit patterns and compares their empirical pattern entropies.

- Standard parameter: `m=10`.
- Recommended relationship inherited from the C report: `m <= floor(log2(n))-5`.
- Output: one p-value.
- A low value indicates irregularity in the frequency relationship between patterns of adjacent lengths.

### 7.12 Random Excursions

Treats the bitstream as a -1/+1 random walk, divides it into cycles ending at zero, and counts visits to states
-4, -3, -2, -1, 1, 2, 3, and 4 during each cycle.

The test is applicable only when:

```text
J >= max(0.005*sqrt(n), 500)
```

where `J` is the number of cycles.

- Output: eight p-values, one for each state.
- A low value indicates an abnormal distribution of visits per cycle.
- When not applicable, the port preserves the C convention of returning zero and includes
  `notApplicable=true` in `details`.

### 7.13 Random Excursions Variant

Counts total visits to states -9 through -1 and 1 through 9 across the same random walk.

- Applicability constraint: the same minimum-cycle rule as Random Excursions.
- Output: 18 p-values.
- A low value indicates an abnormal total visit count for that state.
- When not applicable, `details` contains `notApplicable=true`.

### 7.14 Serial

Counts all circular `m`, `m-1`, and `m-2` patterns and calculates two differences between their psi-square
statistics.

- Standard parameter: `m=16`.
- Output: two p-values, Delta 1 and Delta 2.
- Low values indicate that overlapping patterns are not uniformly distributed across adjacent pattern lengths.

### 7.15 Linear Complexity

Divides the input into `M`-bit blocks and calculates the shortest linear feedback shift register capable of
generating each block using the Berlekamp-Massey algorithm over GF(2).

- Standard parameter: `M=500`.
- Remaining bits are discarded.
- Output: one p-value.
- A low value indicates sequences that are unusually easy or unusually difficult to represent with linear
  recurrences.

## 8. Reading and using a complete report

```java
EntropySource entropy = EntropySources.configured();
byte[] sample = entropy.nextBytes(125_000);

NistSuiteResult report = NistTestSuite.standard().run(sample);
for (NistTestResult result : report.results()) {
    System.out.printf(
            "%s | p=%.8f | %s | %s%n",
            result.testName(),
            result.pValue(),
            result.passed() ? "PASS" : "FAIL",
            result.details()
    );
}
```

Do not regenerate cryptographic keys merely until a sample passes every test. That would select data based on its
test outcomes and misuse the suite. Appropriate uses include device acceptance, offline diagnostics, regression
testing, and investigation of persistent or correlated failures.

## 9. What is not yet implemented

The Java code implements all first-level SP 800-22 test categories for one input stream. The C `assess` application
also performs second-level analysis across many independent streams:

- proportion of streams passing each test;
- uniformity of the collection of p-values;
- final multi-stream analysis report.

That aggregation/reporting layer has not yet been ported. Therefore, one `NistSuiteResult` should not be presented
as a complete NIST validation or certification. Formal entropy-source evaluation additionally requires the
SP 800-90B process and cannot be obtained from SP 800-22 output alone.
