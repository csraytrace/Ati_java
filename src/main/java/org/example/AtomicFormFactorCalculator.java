package org.example;

import java.util.HashMap;
import java.util.Map;

public class AtomicFormFactorCalculator {

    // =========================
    // Parameterklassen
    // =========================

    public static class ParamsZ1to7 {
        final int z;
        final double a;
        final double b1;
        final double c;
        final double q1;
        final double b2;
        final double q2;
        final double b3;
        final double q3;
        final double b4;
        final double q4;

        public ParamsZ1to7(int z, double a, double b1, double c,
                           double q1, double b2, double q2,
                           double b3, double q3, double b4, double q4) {
            this.z = z;
            this.a = a;
            this.b1 = b1;
            this.c = c;
            this.q1 = q1;
            this.b2 = b2;
            this.q2 = q2;
            this.b3 = b3;
            this.q3 = q3;
            this.b4 = b4;
            this.q4 = q4;
        }
    }

    public static class ParamsZ8to50 {
        final int z;
        final double a;
        final double b1;
        final double c;
        final double q1;
        final double b2;
        final double q2;
        final double b3;
        final double q3;

        public ParamsZ8to50(int z, double a, double b1, double c,
                            double q1, double b2, double q2,
                            double b3, double q3) {
            this.z = z;
            this.a = a;
            this.b1 = b1;
            this.c = c;
            this.q1 = q1;
            this.b2 = b2;
            this.q2 = q2;
            this.b3 = b3;
            this.q3 = q3;
        }
    }

    // =========================
    // Parameterdaten
    // =========================

    private static final Map<Integer, ParamsZ1to7> PARAMS_LOW_Z = new HashMap<>();
    private static final Map<Integer, ParamsZ8to50> PARAMS_HIGH_Z = new HashMap<>();

    static {
        // Z = 1..7
        PARAMS_LOW_Z.put(1, new ParamsZ1to7(1, 3.566, -1.143, -2.243, 0.20, 6.102, 0.6, 4.442, 0.90, 3.921, 15));
        PARAMS_LOW_Z.put(2, new ParamsZ1to7(2, 3.264, 0.019, -1.773, 0.30, 3.695, 0.7, 2.905, 1.25, 3.736, 8));
        PARAMS_LOW_Z.put(3, new ParamsZ1to7(3, 3.010, 2.581, -3.231, 0.25, 1.647, 0.5, 2.177, 1.50, 3.591, 8));
        PARAMS_LOW_Z.put(4, new ParamsZ1to7(4, 6.343, 0.470, -2.279, 0.20, 1.412, 0.5, 1.559, 2.50, 3.606, 8));
        PARAMS_LOW_Z.put(5, new ParamsZ1to7(5, 6.625, 1.007, -2.285, 0.25, 1.593, 0.5, 1.248, 2.50, 3.435, 8));
        PARAMS_LOW_Z.put(6, new ParamsZ1to7(6, 7.366, 0.745, -3.209, 0.25, 2.395, 0.5, 1.026, 2.50, 3.258, 8));
        PARAMS_LOW_Z.put(7, new ParamsZ1to7(7, 8.657, 0.222, -3.815, 0.25, 2.787, 0.5, 0.878, 2.50, 3.003, 8));

        // Z = 8..50
        PARAMS_HIGH_Z.put(8,  new ParamsZ8to50(8,  -2.038, 17.634, 2.887, 0.50, 1.339, 0.839, 0.718, 7.0));
        PARAMS_HIGH_Z.put(9,  new ParamsZ8to50(9,  -30.997, 5.569, 4.307, 0.25, 2.800, 0.656, 0.659, 7.0));
        PARAMS_HIGH_Z.put(10, new ParamsZ8to50(10, -8.137, 7.102, 3.058, 0.13, 2.419, 0.768, 0.605, 7.0));
        PARAMS_HIGH_Z.put(11, new ParamsZ8to50(11, -1.108, 29.778, 1.972, 0.40, 2.157, 0.902, 0.555, 7.0));
        PARAMS_HIGH_Z.put(12, new ParamsZ8to50(12, -0.599, 51.200, 1.836, 0.20, 1.884, 1.024, 0.512, 7.0));
        PARAMS_HIGH_Z.put(13, new ParamsZ8to50(13, -0.618, 61.112, 1.926, 0.25, 1.717, 1.132, 0.476, 7.0));
        PARAMS_HIGH_Z.put(14, new ParamsZ8to50(14, -0.929, 45.786, 2.093, 0.25, 1.538, 1.285, 0.442, 7.0));
        PARAMS_HIGH_Z.put(15, new ParamsZ8to50(15, -0.998, 45.579, 2.055, 0.30, 1.425, 1.403, 0.413, 7.0));
        PARAMS_HIGH_Z.put(16, new ParamsZ8to50(16, -1.457, 33.964, 2.154, 0.30, 1.321, 1.581, 0.373, 10.0));
        PARAMS_HIGH_Z.put(17, new ParamsZ8to50(17, -2.082, 25.555, 2.245, 0.30, 1.268, 1.674, 0.354, 10.0));
        PARAMS_HIGH_Z.put(18, new ParamsZ8to50(18, -2.779, 19.784, 2.290, 0.40, 1.084, 1.889, 0.338, 10.0));
        PARAMS_HIGH_Z.put(19, new ParamsZ8to50(19, -1.500, 29.714, 2.008, 0.40, 1.105, 1.922, 0.319, 10.0));
        PARAMS_HIGH_Z.put(20, new ParamsZ8to50(20, -0.966, 50.494, 1.901, 0.50, 0.963, 2.178, 0.300, 10.0));
        PARAMS_HIGH_Z.put(21, new ParamsZ8to50(21, -1.083, 50.773, 1.873, 0.50, 0.952, 2.248, 0.287, 10.0));
        PARAMS_HIGH_Z.put(22, new ParamsZ8to50(22, -1.300, 39.583, 1.852, 0.60, 0.838, 2.463, 0.276, 10.0));
        PARAMS_HIGH_Z.put(23, new ParamsZ8to50(23, -1.477, 37.779, 1.816, 0.60, 0.842, 2.501, 0.265, 10.0));
        PARAMS_HIGH_Z.put(24, new ParamsZ8to50(24, -2.451, 22.944, 1.862, 0.60, 0.819, 2.636, 0.252, 100.0));
        PARAMS_HIGH_Z.put(25, new ParamsZ8to50(25, -1.962, 28.845, 1.756, 0.70, 0.753, 2.792, 0.243, 10.0));
        PARAMS_HIGH_Z.put(26, new ParamsZ8to50(26, -1.577, 44.670, 1.651, 0.80, 0.679, 3.147, 0.226, 10.0));
        PARAMS_HIGH_Z.put(27, new ParamsZ8to50(27, -2.522, 22.906, 1.697, 0.80, 0.659, 3.244, 0.219, 10.0));
        PARAMS_HIGH_Z.put(28, new ParamsZ8to50(28, -3.013, 19.358, 1.692, 0.80, 0.656, 3.309, 0.212, 10.0));
        PARAMS_HIGH_Z.put(29, new ParamsZ8to50(29, -4.405, 14.344, 1.728, 0.80, 0.651, 3.370, 0.205, 10.0));
        PARAMS_HIGH_Z.put(30, new ParamsZ8to50(30, -3.694, 16.277, 1.634, 0.90, 0.603, 3.533, 0.203, 10.0));
        PARAMS_HIGH_Z.put(31, new ParamsZ8to50(31, -3.245, 17.357, 1.580, 0.90, 0.612, 3.541, 0.200, 10.0));
        PARAMS_HIGH_Z.put(32, new ParamsZ8to50(32, -2.415, 24.695, 1.491, 1.00, 0.576, 3.754, 0.190, 10.0));
        PARAMS_HIGH_Z.put(33, new ParamsZ8to50(33, -2.189, 29.184, 1.457, 1.00, 0.579, 3.791, 0.187, 10.0));
        PARAMS_HIGH_Z.put(34, new ParamsZ8to50(34, -1.357, 65.926, 1.344, 1.25, 0.491, 4.339, 0.175, 15.0));
        PARAMS_HIGH_Z.put(35, new ParamsZ8to50(35, -1.391, 66.774, 1.329, 1.25, 0.490, 4.436, 0.169, 15.0));
        PARAMS_HIGH_Z.put(36, new ParamsZ8to50(36, -1.432, 68.070, 1.320, 1.25, 0.486, 4.543, 0.164, 15.0));
        PARAMS_HIGH_Z.put(37, new ParamsZ8to50(37, -1.387, 49.729, 1.333, 1.25, 0.467, 4.706, 0.159, 15.0));
        PARAMS_HIGH_Z.put(38, new ParamsZ8to50(38, -1.012, 78.049, 1.306, 1.25, 0.470, 4.779, 0.154, 15.0));
        PARAMS_HIGH_Z.put(39, new ParamsZ8to50(39, -0.947, 92.565, 1.294, 1.25, 0.468, 4.831, 0.151, 15.0));
        PARAMS_HIGH_Z.put(40, new ParamsZ8to50(40, -0.960, 96.320, 1.285, 1.25, 0.466, 4.895, 0.148, 15.0));
        PARAMS_HIGH_Z.put(41, new ParamsZ8to50(41, -0.184, 297.380, 1.241, 1.25, 0.478, 4.876, 0.145, 15.0));
        PARAMS_HIGH_Z.put(42, new ParamsZ8to50(42, 1.157, -0.320, 1.275, 2.00, 0.375, 5.653, 0.136, 15.0));
        PARAMS_HIGH_Z.put(43, new ParamsZ8to50(43, 1.157, -0.320, 1.275, 2.00, 0.363, 5.815, 0.133, 15.0));
        PARAMS_HIGH_Z.put(44, new ParamsZ8to50(44, 1.157, -0.319, 1.265, 2.00, 0.358, 6.006, 0.117, 15.0));
        PARAMS_HIGH_Z.put(45, new ParamsZ8to50(45, 1.141, -0.488, 1.269, 2.00, 0.411, 5.624, 0.127, 15.0));
        PARAMS_HIGH_Z.put(46, new ParamsZ8to50(46, 1.141, -0.488, 1.269, 2.00, 0.400, 5.734, 0.124, 15.0));
        PARAMS_HIGH_Z.put(47, new ParamsZ8to50(47, 1.141, -0.488, 1.269, 2.00, 0.379, 6.119, 0.117, 15.0));
        PARAMS_HIGH_Z.put(48, new ParamsZ8to50(48, 1.141, -0.488, 1.269, 2.00, 0.710, 6.219, 0.114, 15.0));
        PARAMS_HIGH_Z.put(49, new ParamsZ8to50(49, -2.015, 59.177, 1.308, 0.80, 0.782, 2.226, 0.326, 7.0));
        PARAMS_HIGH_Z.put(50, new ParamsZ8to50(50, -1.957, 61.985, 1.302, 0.80, 0.770, 2.265, 0.324, 7.0));
    }

    // =========================
    // Hilfsfunktionen
    // =========================

    private static double f1(double a, double b1, double q, int z, double c) {
        return a * Math.exp(-b1 * q) + (z - a) * Math.exp(-c * q);
    }

    private static double f2(double b2, double q1, double a, double b1, double q, int z, double c) {
        return f1(a, b1, q1, z, c) * Math.exp(b2 * (q1 - q));
    }

    private static double f3(double b2, double q1, double a, double b1, double q, int z, double c,
                             double b3, double q2) {
        return f2(b2, q1, a, b1, q2, z, c) * Math.exp(b3 * (q2 - q));
    }

    private static double f4(double b2, double q1, double a, double b1, double q, int z, double c,
                             double b3, double q2, double q3, double b4) {
        return f3(b2, q1, a, b1, q3, z, c, b3, q2) * Math.pow(q / q3, -b4);
    }

    // =========================
    // Hauptfunktion
    // =========================

    public static double F(int z, double q) {
        if (z < 1 || z > 50) {
            throw new IllegalArgumentException("Nur für 1 <= Z <= 50 implementiert.");
        }
        if (q < 0) {
            throw new IllegalArgumentException("q darf nicht negativ sein.");
        }

        if (z <= 7) {
            ParamsZ1to7 p = PARAMS_LOW_Z.get(z);
            if (p == null) {
                throw new IllegalArgumentException("Keine Parameter für Z = " + z + " gefunden.");
            }

            if (q <= p.q1) {
                return f1(p.a, p.b1, q, z, p.c);
            } else if (q <= p.q2) {
                return f2(p.b2, p.q1, p.a, p.b1, q, z, p.c);
            } else if (q <= p.q3) {
                return f3(p.b2, p.q1, p.a, p.b1, q, z, p.c, p.b3, p.q2);
            } else if (q <= p.q4) {
                return f4(p.b2, p.q1, p.a, p.b1, q, z, p.c, p.b3, p.q2, p.q3, p.b4);
            } else {
                throw new IllegalArgumentException("q > q4 für Z = " + z);
            }
        } else {
            ParamsZ8to50 p = PARAMS_HIGH_Z.get(z);
            if (p == null) {
                throw new IllegalArgumentException("Keine Parameter für Z = " + z + " gefunden.");
            }

            if (q <= p.q1) {
                return f1(p.a, p.b1, q, z, p.c);
            } else if (q <= p.q2) {
                return f2(p.b2, p.q1, p.a, p.b1, q, z, p.c);
            } else if (q <= p.q3) {
                return f3(p.b2, p.q1, p.a, p.b1, q, z, p.c, p.b3, p.q2);
            } else {
                throw new IllegalArgumentException("q > q3 für Z = " + z);
            }
        }
    }

    // =========================
    // Test
    // =========================

    public static void main(String[] args) {
        int z = 5;
        double q = 0.4;

        double result = F(z, q);
        System.out.println("F(" + z + ", " + q + ") = " + result);
        System.out.println(F(6,1));
        System.out.println(F(8,1));
        System.out.println(F(10,1));
    }
}
