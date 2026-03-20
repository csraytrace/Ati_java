package org.example;

import java.util.HashMap;
import java.util.Map;

public class IncoherentScatteringCalculator {

    public static class SParams {
        final int z;
        final double a;
        final double b;
        final double c;
        final double d;

        public SParams(int z, double a, double b, double c, double d) {
            this.z = z;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    private static final Map<Integer, SParams> S_PARAMS = new HashMap<>();

    static {
        S_PARAMS.put(2,  new SParams(2,  7.24e-01, -2.15e-01, 9.10e0,  1.56e1));
        S_PARAMS.put(3,  new SParams(3,  2.61e1,    6.88e0,   2.63e1,  8.81e-1));
        S_PARAMS.put(4,  new SParams(4,  1.70e1,    1.12e3,   4.09e1,  1.04e2));
        S_PARAMS.put(5,  new SParams(5,  7.30e0,    2.73e2,   2.27e1,  3.44e1));
        S_PARAMS.put(6,  new SParams(6,  4.34e0,    9.31e1,   1.47e1,  1.46e1));
        S_PARAMS.put(7,  new SParams(7,  4.51e0,    4.10e1,   1.11e1,  7.32e0));
        S_PARAMS.put(8,  new SParams(8,  3.24e0,    1.94e1,   8.27e0,  4.01e0));
        S_PARAMS.put(9,  new SParams(9,  2.78e0,    1.00e1,   6.60e0,  2.31e0));
        S_PARAMS.put(10, new SParams(10, 3.19e0,    5.65e0,   5.77e0,  1.38e0));
        S_PARAMS.put(11, new SParams(11, 1.63e1,    4.52e1,   1.32e1,  5.46e0));
        S_PARAMS.put(12, new SParams(12, 1.16e2,    3.23e3,   7.08e1,  7.72e1));
        S_PARAMS.put(13, new SParams(13, 1.08e2,    2.74e3,   6.72e1,  5.99e1));
        S_PARAMS.put(14, new SParams(14, 8.07e1,    1.51e3,   5.21e1,  3.70e1));
        S_PARAMS.put(15, new SParams(15, 5.59e1,    7.03e2,   3.74e1,  2.09e1));
        S_PARAMS.put(16, new SParams(16, 4.37e1,    4.16e2,   3.01e1,  1.34e1));
        S_PARAMS.put(17, new SParams(17, 3.46e1,    2.53e2,   2.45e1,  8.85e0));
        S_PARAMS.put(18, new SParams(18, 2.71e1,    1.50e2,   1.97e1,  5.88e0));
        S_PARAMS.put(19, new SParams(19, 3.05e1,    1.94e2,   2.19e1,  6.56e0));
        S_PARAMS.put(20, new SParams(20, 3.69e1,    2.90e2,   2.60e1,  8.16e0));
        S_PARAMS.put(21, new SParams(21, 3.32e1,    2.33e2,   2.35e1,  6.86e0));
        S_PARAMS.put(22, new SParams(22, 2.95e1,    1.82e2,   2.10e1,  5.71e0));
        S_PARAMS.put(23, new SParams(23, 2.62e1,    1.43e2,   1.87e1,  4.82e0));
        S_PARAMS.put(24, new SParams(24, 1.89e1,    7.25e1,   1.38e1,  3.04e0));
        S_PARAMS.put(25, new SParams(25, 2.13e1,    9.48e1,   1.52e1,  3.62e0));
        S_PARAMS.put(26, new SParams(26, 1.93e1,    7.75e1,   1.38e1,  3.11e0));
        S_PARAMS.put(27, new SParams(27, 1.76e1,    6.47e1,   1.26e1,  2.71e0));
        S_PARAMS.put(28, new SParams(28, 1.63e1,    5.59e1,   1.17e1,  2.43e0));
        S_PARAMS.put(29, new SParams(29, 1.29e1,    3.38e1,   9.34e0,  1.72e0));
        S_PARAMS.put(30, new SParams(30, 1.46e1,    4.55e1,   1.03e1,  2.08e0));
    }

    private static double sAlt(int z, double a, double b, double c, double d, double q) {
        double q2 = q * q;
        double q4 = q2 * q2;

        double numerator = 1.0 + a * q2 + b * q4;
        double denominator = 1.0 + c * q2 + d * q4;

        return z * (1.0 - numerator / (denominator * denominator));
    }

    public static double S(int z, double q) {
        if (z > 30) {
            throw new IllegalArgumentException("Nur bis Z <= 30 implementiert.");
        }
        if (q < 0) {
            throw new IllegalArgumentException("q darf nicht negativ sein.");
        }

        SParams p = S_PARAMS.get(z);
        if (p == null) {
            throw new IllegalArgumentException("Keine S-Parameter für Z = " + z + " gefunden.");
        }

        return sAlt(z, p.a, p.b, p.c, p.d, q);
    }

    public static void main(String[] args) {
        int z = 26;
        double q = 0.5;

        double result = S(z, q);
        System.out.println("S(" + z + ", " + q + ") = " + result);
        System.out.println(S(6,1));
        System.out.println(S(8,1));
        System.out.println(S(10,1));
    }
}
