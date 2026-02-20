package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class XYSeriesIO {

    private XYSeriesIO() {}

    // -----------------------------
    // SPE Loader
    // -----------------------------
    public static XYSeries fromSpe(Path spePath) throws IOException {
        // x = 1..N wie gewünscht
        return fromSpe(spePath, true);
    }

    public static XYSeries fromSpe(Path spePath, boolean xStartsAtOne) throws IOException {
        List<Double> yList = new ArrayList<>();
        Integer dataStart = null;
        Integer dataEnd = null;

        boolean inDataBlock = false;
        boolean readRangeLine = false;

        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(spePath.toFile()), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("$DATA")) {
                    inDataBlock = true;
                    readRangeLine = true;
                    continue;
                }

                if (inDataBlock) {
                    // SPE Sections end with next $SECTION
                    if (line.startsWith("$")) break;

                    if (readRangeLine) {
                        // Format in deinem Writer:
                        // start end (rechtsbündig, aber whitespace-getrennt)
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length < 2) {
                            throw new IOException("Ungültige $DATA Range-Line in " + spePath + ": " + line);
                        }
                        dataStart = parseIntStrict(parts[0], "DATA start");
                        dataEnd = parseIntStrict(parts[1], "DATA end");
                        if (dataStart < 0 || dataEnd < dataStart) {
                            throw new IOException("Ungültiger $DATA Bereich: " + dataStart + " .. " + dataEnd);
                        }
                        readRangeLine = false;
                        continue;
                    }

                    // Danach: viele integers, whitespace-separated, ggf. mehrere pro Zeile
                    String[] nums = line.split("\\s+");
                    for (String n : nums) {
                        if (n.isEmpty()) continue;
                        yList.add((double) parseIntStrict(n, "count"));
                    }
                }
            }
        }

        if (dataStart == null || dataEnd == null) {
            throw new IOException("Keine $DATA Section gefunden oder Range-Line fehlt: " + spePath);
        }

        int expected = (dataEnd - dataStart) + 1;
        if (yList.size() < expected) {
            throw new IOException("Zu wenige Datenpunkte in $DATA: erwartet " + expected + ", gefunden " + yList.size());
        }
        if (yList.size() > expected) {
            // Manche SPE enthalten mehr, wir schneiden strikt auf expected (robust)
            yList = yList.subList(0, expected);
        }

        double[] y = new double[expected];
        double[] x = new double[expected];
        for (int i = 0; i < expected; i++) {
            y[i] = yList.get(i);
            x[i] = xStartsAtOne ? (i + 1) : i;
        }

        String title = spePath.getFileName().toString();
        return XYSeries.of(x, y, title);
    }

    // -----------------------------
    // CSV Loader
    // -----------------------------
    public static XYSeries fromCsv(Path csvPath) throws IOException {
        return fromCsv(csvPath, true);
    }

    /**
     * @param allowHeader Wenn true: erste Zeile darf Header sein ("x,y" / "x;y" etc.) und wird dann übersprungen.
     */
    public static XYSeries fromCsv(Path csvPath, boolean allowHeader) throws IOException {
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(csvPath.toFile()), StandardCharsets.UTF_8))) {

            String line;
            boolean firstDataLineChecked = false;

            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Kommentare erlauben
                if (line.startsWith("#") || line.startsWith("//")) continue;

                // Separator tolerant: ; oder , oder whitespace
                String[] parts = splitCsvLine(line);
                if (parts.length < 2) continue;

                if (allowHeader && !firstDataLineChecked) {
                    firstDataLineChecked = true;
                    // Wenn erste "Daten"-Zeile nicht numerisch ist, Header überspringen
                    if (!looksNumeric(parts[0]) || !looksNumeric(parts[1])) {
                        continue;
                    }
                }

                double x = parseDoubleStrict(parts[0], "x");
                double y = parseDoubleStrict(parts[1], "y");

                xs.add(x);
                ys.add(y);
            }
        }

        if (xs.isEmpty()) {
            throw new IOException("CSV enthält keine Daten: " + csvPath);
        }
        if (xs.size() != ys.size()) {
            throw new IOException("CSV inkonsistent: x-Anzahl != y-Anzahl");
        }

        double[] xArr = new double[xs.size()];
        double[] yArr = new double[ys.size()];
        for (int i = 0; i < xs.size(); i++) {
            xArr[i] = xs.get(i);
            yArr[i] = ys.get(i);
        }

        String title = csvPath.getFileName().toString();
        return XYSeries.of(xArr, yArr, title);
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private static String[] splitCsvLine(String line) {
        // Erst ;, dann ,; fallback whitespace
        if (line.contains(";")) return trimAll(line.split(";"));
        if (line.contains(",")) return trimAll(line.split(","));
        return trimAll(line.split("\\s+"));
    }

    private static String[] trimAll(String[] arr) {
        for (int i = 0; i < arr.length; i++) arr[i] = arr[i].trim();
        return arr;
    }

    private static int parseIntStrict(String s, String label) throws IOException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Ungültige Integer-Zahl für " + label + ": '" + s + "'", e);
        }
    }

    private static double parseDoubleStrict(String s, String label) throws IOException {
        try {
            double v = Double.parseDouble(s.trim().replace(',', '.')); // falls jemand Dezimal-Komma nutzt
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IOException("Ungültiger Double-Wert für " + label + ": '" + s + "'");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IOException("Ungültige Double-Zahl für " + label + ": '" + s + "'", e);
        }
    }

    private static boolean looksNumeric(String s) {
        // sehr simple Heuristik
        String t = s.trim();
        if (t.isEmpty()) return false;
        // erlaubt: - 0-9 . , e E
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(c == '-' || c == '+' || (c >= '0' && c <= '9') || c == '.' || c == ',' || c == 'e' || c == 'E')) {
                return false;
            }
        }
        return true;
    }
}

