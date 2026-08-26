package random.nist;

/** Numerical functions required by the first NIST ports. */
final class NistMath {
    private static final double[] LANCZOS = {
            676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012,
            9.9843695780195716e-6, 1.5056327351493116e-7
    };
    private NistMath() {
    }

    /* Numerical Recipes approximation; maximum fractional error is approximately 1.2e-7. */
    static double erfc(double value) {
        double absolute = Math.abs(value);
        double t = 1.0 / (1.0 + 0.5 * absolute);
        double polynomial = 0.17087277;
        polynomial = -0.82215223 + t * polynomial;
        polynomial = 1.48851587 + t * polynomial;
        polynomial = -1.13520398 + t * polynomial;
        polynomial = 0.27886807 + t * polynomial;
        polynomial = -0.18628806 + t * polynomial;
        polynomial = 0.09678418 + t * polynomial;
        polynomial = 0.37409196 + t * polynomial;
        polynomial = 1.00002368 + t * polynomial;
        double result = t * Math.exp(-absolute * absolute - 1.26551223 + t * polynomial);
        return value >= 0.0 ? result : 2.0 - result;
    }

    static double normal(double value) {
        return 0.5 * erfc(-value / Math.sqrt(2.0));
    }

    static double regularizedGammaQ(double a, double x) {
        if (a <= 0.0 || x < 0.0 || Double.isNaN(a) || Double.isNaN(x)) {
            throw new IllegalArgumentException("Incomplete gamma requires a > 0 and x >= 0.");
        }
        if (x == 0.0) {
            return 1.0;
        }
        if (Double.isInfinite(x)) {
            return 0.0;
        }
        return x < a + 1.0 ? 1.0 - gammaSeries(a, x) : gammaContinuedFraction(a, x);
    }

    private static double gammaSeries(double a, double x) {
        double sum = 1.0 / a;
        double term = sum;
        double ap = a;
        for (int iteration = 1; iteration <= 100_000; iteration++) {
            ap += 1.0;
            term *= x / ap;
            sum += term;
            if (Math.abs(term) <= Math.abs(sum) * 1e-15) {
                return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
            }
        }
        throw new ArithmeticException("Incomplete gamma series did not converge.");
    }

    private static double gammaContinuedFraction(double a, double x) {
        double b = x + 1.0 - a;
        double c = 1.0 / 1e-300;
        double d = 1.0 / Math.max(Math.abs(b), 1e-300) * Math.copySign(1.0, b);
        double h = d;
        for (int iteration = 1; iteration <= 100_000; iteration++) {
            double an = -iteration * (iteration - a);
            b += 2.0;
            d = an * d + b;
            if (Math.abs(d) < 1e-300) {
                d = 1e-300;
            }
            c = b + an / c;
            if (Math.abs(c) < 1e-300) {
                c = 1e-300;
            }
            d = 1.0 / d;
            double delta = d * c;
            h *= delta;
            if (Math.abs(delta - 1.0) <= 1e-15) {
                return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
            }
        }
        throw new ArithmeticException("Incomplete gamma continued fraction did not converge.");
    }

    static double logGamma(double value) {
        if (value < 0.5) {
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * value)) - logGamma(1.0 - value);
        }
        double shifted = value - 1.0;
        double sum = 0.99999999999980993;
        for (int index = 0; index < LANCZOS.length; index++) {
            sum += LANCZOS[index] / (shifted + index + 1.0);
        }
        double t = shifted + LANCZOS.length - 0.5;
        return 0.5 * Math.log(2.0 * Math.PI) + (shifted + 0.5) * Math.log(t) - t + Math.log(sum);
    }
}
