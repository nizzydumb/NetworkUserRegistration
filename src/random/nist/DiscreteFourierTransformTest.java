package random.nist;

/** Java port of upstream src/discreteFourierTransform.c using a Bluestein FFT. */
public final class DiscreteFourierTransformTest implements NistRandomnessTest {
    @Override
    public NistTestResult test(BitSequence sequence, double significanceLevel) {
        int length = sequence.length();
        if (length < 2) {
            throw new IllegalArgumentException("Discrete Fourier transform test requires at least two bits.");
        }
        double[] real = new double[length];
        double[] imaginary = new double[length];
        for (int index = 0; index < length; index++) {
            real[index] = 2.0 * sequence.bitAt(index) - 1.0;
        }
        BluesteinFft.transform(real, imaginary);

        double threshold = Math.sqrt(2.995732274 * length);
        int belowThreshold = 0;
        for (int index = 0; index < length / 2; index++) {
            if (Math.hypot(real[index], imaginary[index]) < threshold) {
                belowThreshold++;
            }
        }
        double expected = 0.95 * length / 2.0;
        double normalizedDifference = (belowThreshold - expected)
                / Math.sqrt(length / 4.0 * 0.95 * 0.05);
        double pValue = NistMath.erfc(Math.abs(normalizedDifference) / Math.sqrt(2.0));
        return NistTestResult.of("Discrete Fourier Transform", pValue, significanceLevel,
                "peaksBelowThreshold=" + belowThreshold + ", expected=" + expected
                        + ", normalizedDifference=" + normalizedDifference);
    }

    private static final class BluesteinFft {
        private BluesteinFft() {
        }

        private static void transform(double[] real, double[] imaginary) {
            int length = real.length;
            int convolutionLength = 1;
            while (convolutionLength < length * 2 + 1) {
                convolutionLength <<= 1;
            }
            double[] cosine = new double[length];
            double[] sine = new double[length];
            for (int index = 0; index < length; index++) {
                long squareModulo = (long) index * index % (2L * length);
                double angle = Math.PI * squareModulo / length;
                cosine[index] = Math.cos(angle);
                sine[index] = Math.sin(angle);
            }

            double[] aReal = new double[convolutionLength];
            double[] aImaginary = new double[convolutionLength];
            double[] bReal = new double[convolutionLength];
            double[] bImaginary = new double[convolutionLength];
            for (int index = 0; index < length; index++) {
                aReal[index] = real[index] * cosine[index] + imaginary[index] * sine[index];
                aImaginary[index] = -real[index] * sine[index] + imaginary[index] * cosine[index];
                bReal[index] = cosine[index];
                bImaginary[index] = sine[index];
                if (index != 0) {
                    bReal[convolutionLength - index] = cosine[index];
                    bImaginary[convolutionLength - index] = sine[index];
                }
            }
            convolve(aReal, aImaginary, bReal, bImaginary);
            for (int index = 0; index < length; index++) {
                real[index] = aReal[index] * cosine[index] + aImaginary[index] * sine[index];
                imaginary[index] = -aReal[index] * sine[index] + aImaginary[index] * cosine[index];
            }
        }

        private static void convolve(double[] xReal, double[] xImaginary, double[] yReal, double[] yImaginary) {
            radixTwo(xReal, xImaginary, false);
            radixTwo(yReal, yImaginary, false);
            for (int index = 0; index < xReal.length; index++) {
                double real = xReal[index] * yReal[index] - xImaginary[index] * yImaginary[index];
                double imaginary = xImaginary[index] * yReal[index] + xReal[index] * yImaginary[index];
                xReal[index] = real;
                xImaginary[index] = imaginary;
            }
            radixTwo(xReal, xImaginary, true);
        }

        private static void radixTwo(double[] real, double[] imaginary, boolean inverse) {
            int length = real.length;
            for (int index = 1, reversed = 0; index < length; index++) {
                int bit = length >>> 1;
                while ((reversed & bit) != 0) {
                    reversed ^= bit;
                    bit >>>= 1;
                }
                reversed ^= bit;
                if (index < reversed) {
                    double temporary = real[index];
                    real[index] = real[reversed];
                    real[reversed] = temporary;
                    temporary = imaginary[index];
                    imaginary[index] = imaginary[reversed];
                    imaginary[reversed] = temporary;
                }
            }
            for (int size = 2; size <= length; size <<= 1) {
                double angle = (inverse ? 2.0 : -2.0) * Math.PI / size;
                double rootReal = Math.cos(angle);
                double rootImaginary = Math.sin(angle);
                for (int start = 0; start < length; start += size) {
                    double factorReal = 1.0;
                    double factorImaginary = 0.0;
                    for (int offset = 0; offset < size / 2; offset++) {
                        int even = start + offset;
                        int odd = even + size / 2;
                        double oddReal = real[odd] * factorReal - imaginary[odd] * factorImaginary;
                        double oddImaginary = real[odd] * factorImaginary + imaginary[odd] * factorReal;
                        real[odd] = real[even] - oddReal;
                        imaginary[odd] = imaginary[even] - oddImaginary;
                        real[even] += oddReal;
                        imaginary[even] += oddImaginary;
                        double nextReal = factorReal * rootReal - factorImaginary * rootImaginary;
                        factorImaginary = factorReal * rootImaginary + factorImaginary * rootReal;
                        factorReal = nextReal;
                    }
                }
            }
            if (inverse) {
                for (int index = 0; index < length; index++) {
                    real[index] /= length;
                    imaginary[index] /= length;
                }
            }
        }
    }
}
