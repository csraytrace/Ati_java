package org.example;

import org.mariuszgromada.math.mxparser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Stückweise definierte Funktion mit mXparser.
 * - Segmente können per Formel-String (z. B. "3*x^3 + ln(x)/sin(x)") oder als mXparser-Function
 *   hinzugefügt werden.
 * - Grenzen können inklusiv oder exklusiv gesetzt werden.
 * - Außerhalb aller Segmente wird defaultValue zurückgegeben (z. B. 0 oder 1).
 * - NaN/Inf (z. B. ln(x<=0), Division durch 0) -> defaultValue.
 *
 * Achtung: sin(), cos() usw. verwenden Radiant.
 */
public final class MathParser {


    static {
        License.iConfirmNonCommercialUse("Julia – Masterarbeit");
        // Für kommerzielle Nutzung stattdessen:
        // License.iConfirmCommercialUse("Firmenname / Projekt");
    }

    /** Kleines Epsilon zur robusten Grenzprüfung (floating point) */
    private final double eps;
    /** Rückgabewert außerhalb aller definierten Intervalle oder bei Rechenfehlern */
    private final double defaultValue;

    private static final class Segment {
        final double a, b;                 // Intervallgrenzen
        final boolean inclA, inclB;        // inklusiv?
        final Function f;                  // mXparser-Funktion: name(x) = <expr>

        Segment(double a, boolean inclA, double b, boolean inclB, Function f) {
            this.a = a; this.inclA = inclA; this.b = b; this.inclB = inclB; this.f = f;
        }

        boolean inside(double x, double eps) {
            boolean left  = inclA ? x >= a - eps : x > a + eps;
            boolean right = inclB ? x <= b + eps : x < b - eps;
            return left && right;
        }
    }

    private final List<Segment> segs = new ArrayList<>();
    private int autoNameCounter = 1;

    /**
     * @param defaultValue Wert außerhalb aller Intervalle (z. B. 0 oder 1)
     * @param eps          Toleranz bei Grenzvergleichen (z. B. 1e-12)
     */
    public MathParser(double defaultValue, double eps) {
        this.defaultValue = defaultValue;
        this.eps = eps <= 0 ? 1e-12 : eps;
    }

    /** Komfort: defaultValue=0, eps=1e-12 */
    public static MathParser withDefault(double defaultValue) {
        return new MathParser(defaultValue, 1e-12);
    }

    /* -------------------- Segmente hinzufügen -------------------- */

    /**
     * Segment mit Ausdruck-String hinzufügen, z. B. "3*x^3 + ln(x)/sin(x)".
     * Grenzen inkl./exkl. wählbar.
     */
    public MathParser addExprSegment(String expr,
                                                  double a, boolean inclA,
                                                  double b, boolean inclB) {
        String name = "seg" + (autoNameCounter++);
        Function f = new Function(name + "(x) = " + expr);
        if (!f.checkSyntax()) {
            throw new IllegalArgumentException("Ungültiger Ausdruck: " + f.getErrorMessage());
        }
        segs.add(new Segment(a, inclA, b, inclB, f));
        return this;
    }

    /**
     * Segment mit bereits definierter mXparser-Function (z. B. new Function("f(x)=...")).
     */
    public MathParser addFunctionSegment(Function f,
                                                      double a, boolean inclA,
                                                      double b, boolean inclB) {
        if (f == null || !f.checkSyntax()) {
            throw new IllegalArgumentException("Ungültige Function: " + (f == null ? "null" : f.getErrorMessage()));
        }
        segs.add(new Segment(a, inclA, b, inclB, f));
        return this;
    }

    /** Konstantes Segment: f(x) = value im Intervall */
    public MathParser addConstantSegment(double value,
                                                      double a, boolean inclA,
                                                      double b, boolean inclB) {
        return addExprSegment(Double.toString(value), a, inclA, b, inclB);
    }

    /* -------------------- Auswertung -------------------- */

    private boolean clampOutput = false;
    private double yMin = Double.NEGATIVE_INFINITY;
    private double yMax = Double.POSITIVE_INFINITY;

    /** Optional: Ausgabe auf [yMin, yMax] klemmen */
    public MathParser setOutputClamp(double yMin, double yMax) {
        if (!Double.isFinite(yMin) || !Double.isFinite(yMax) || yMin > yMax)
            throw new IllegalArgumentException("Ungültiger Wertebereich");
        this.clampOutput = true;
        this.yMin = yMin;
        this.yMax = yMax;
        return this;
    }

    /** Clamp wieder deaktivieren */
    public MathParser clearOutputClamp() {
        this.clampOutput = false;
        this.yMin = Double.NEGATIVE_INFINITY;
        this.yMax = Double.POSITIVE_INFINITY;
        return this;
    }



    /** Wert h(x) – stückweise, inkl. Fehlerschutz; außerhalb -> defaultValue */
    public double evaluate(double x) {
        for (Segment s : segs) {
            if (s.inside(x, eps)) {
                double y = s.f.calculate(x);
                if (Double.isNaN(y) || Double.isInfinite(y)) return defaultValue;

                // immer positiv
                y = Math.abs(y);


                // hier begrenzen
                if (clampOutput) {
                    if (y < yMin) y = yMin;
                    else if (y > yMax) y = yMax;
                }
                //if (y<1) y += 0.5*y;
                //else y -=0.5*y;
                return y;
            }
        }
        return defaultValue; // außerhalb: unverändert defaultValue
    }
}

