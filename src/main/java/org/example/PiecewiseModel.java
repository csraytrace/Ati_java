package org.example;

import java.util.*;

final class PiecewiseModel {
    private final List<PwSegment> segs = new ArrayList<>();
    private double outsideValue = 1.0;

    public PiecewiseModel setOutsideValue(double v) { this.outsideValue = v; return this; }
    public List<PwSegment> segments() { return segs; }

    /** Konsistenz: sortieren, Überschneidungen prüfen, letztes Segment rechts inklusiv */
    public void normalize(double eps) {
        segs.sort(Comparator.comparingDouble(s -> s.a));
        for (int i = 0; i < segs.size() - 1; i++) {
            PwSegment s = segs.get(i), t = segs.get(i+1);
            if (s.b > t.a + eps)
                throw new IllegalStateException("Überlappende Segmente: " + s.id + " / " + t.id);
            s.inclB = false;                 // true/false … true/true
            t.inclA = true;
        }
        if (!segs.isEmpty()) segs.get(segs.size()-1).inclB = true;
    }

    /** In Verbindung exportieren (Formelstrings setzen) */
    public void applyToFilter(Verbindung v, String multiplyWith) {
        v.clearModulation(outsideValue);
        for (PwSegment s : segs) {
            if (!s.enabled) continue;
            String expr = s.toExpressionString(multiplyWith);
            v.addModulationSegment(expr, s.a, s.inclA, s.b, s.inclB);
        }
    }



    public static PiecewiseModel fromPolyline(double[] xs, double[] ys,
                                              Double clampMin, Double clampMax) {
        if (xs.length != ys.length || xs.length < 2)
            throw new IllegalArgumentException("xs/ys gleiche Länge >= 2");

        int n = xs.length;

        // sortieren nach x
        Integer[] ord = new Integer[n];
        for (int i = 0; i < n; i++) ord[i] = i;
        Arrays.sort(ord, Comparator.comparingDouble(i -> xs[i]));

        List<Double> X = new ArrayList<>(), Y = new ArrayList<>();
        double eps = 1e-12;
        for (int k : ord) {
            if (!X.isEmpty() && Math.abs(xs[k] - X.get(X.size() - 1)) <= eps) {
                // Duplikat-x: letztes y ersetzen
                Y.set(Y.size() - 1, ys[k]);
            } else {
                X.add(xs[k]);
                Y.add(ys[k]);
            }
        }
        if (X.size() < 2) throw new IllegalArgumentException("Zu wenige unterschiedliche x");

        PiecewiseModel model = new PiecewiseModel().setOutsideValue(1.0);
        for (int i = 0; i < X.size() - 1; i++) {
            double x1 = X.get(i),   y1 = Y.get(i);
            double x2 = X.get(i + 1), y2 = Y.get(i + 1);
            double m = (y2 - y1) / (x2 - x1);
            double c = y1 - m * x1;

            PwSegment s = new PwSegment();
            s.type = SegmentType.LINEAR;
            s.a = x1; s.b = x2;
            s.inclA = true; s.inclB = (i == X.size() - 2);
            s.m = m; s.c = c;

            if (clampMin != null && clampMax != null) {
                s.clamp = true; s.yMin = clampMin; s.yMax = clampMax;
            }

            model.segments().add(s);
        }

        model.normalize(1e-12);



        for (int i = 0; i < model.segments().size(); i++) {
            PwSegment seg = model.segments().get(i);
            String L = seg.inclA ? "[" : "(";
            String R = seg.inclB ? "]" : ")";
            String clamp = seg.clamp
                    ? String.format(Locale.US, "  clamp=[%.9g, %.9g]", seg.yMin, seg.yMax)
                    : "";
            switch (seg.type) {
                case LINEAR:
                    System.out.printf(Locale.US,
                            "Seg %02d: %s%.9g, %.9g%s  type=LINEAR  m=%.9g  c=%.9g%s%n",
                            i + 1, L, seg.a, seg.b, R, seg.m, seg.c, clamp);
                    break;
                case CONSTANT:
                    System.out.printf(Locale.US,
                            "Seg %02d: %s%.9g, %.9g%s  type=CONST   value=%.9g%s%n",
                            i + 1, L, seg.a, seg.b, R, seg.value, clamp);
                    break;
                case EXPR:
                    System.out.printf(Locale.US,
                            "Seg %02d: %s%.9g, %.9g%s  type=EXPR    expr=%s%s%n",
                            i + 1, L, seg.a, seg.b, R, seg.expr, clamp);
                    break;
            }
        }













        return model;
    }
}