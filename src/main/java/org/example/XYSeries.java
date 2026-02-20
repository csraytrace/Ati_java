package org.example;

import java.util.Arrays;
import java.util.Objects;

public final class XYSeries {
    private final double[] x;
    private final double[] y;
    private final String title; // optional

    private XYSeries(double[] x, double[] y, String title) {
        if (x == null || y == null) throw new IllegalArgumentException("x/y dürfen nicht null sein");
        if (x.length != y.length) throw new IllegalArgumentException("x und y müssen gleich lang sein");
        if (x.length == 0) throw new IllegalArgumentException("Serie darf nicht leer sein");

        // defensive copy + numeric validation
        this.x = Arrays.copyOf(x, x.length);
        this.y = Arrays.copyOf(y, y.length);
        this.title = title == null ? "" : title;

        validateFinite(this.x, "x");
        validateFinite(this.y, "y");
    }

    public static XYSeries of(double[] x, double[] y) {
        return new XYSeries(x, y, "");
    }

    public static XYSeries of(double[] x, double[] y, String title) {
        return new XYSeries(x, y, title);
    }

    public int size() {
        return x.length;
    }

    public double x(int i) {
        return x[i];
    }

    public double y(int i) {
        return y[i];
    }

    public double[] xArrayCopy() {
        return Arrays.copyOf(x, x.length);
    }

    public double[] yArrayCopy() {
        return Arrays.copyOf(y, y.length);
    }

    public String title() {
        return title;
    }

    public XYSeries withTitle(String newTitle) {
        Objects.requireNonNull(newTitle, "title");
        return new XYSeries(this.x, this.y, newTitle);
    }

    private static void validateFinite(double[] arr, String name) {
        for (int i = 0; i < arr.length; i++) {
            double v = arr[i];
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException(name + "[" + i + "] ist NaN/Infinity: " + v);
            }
        }
    }
}

