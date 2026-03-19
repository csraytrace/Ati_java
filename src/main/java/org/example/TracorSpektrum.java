package org.example;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TracorSpektrum {

    private static final int SIZE_LONG  = 8424;
    private static final int SIZE_MID   = 4328;
    private static final int SIZE_SHORT = 2280;

    // -------------------------
    // Öffentliche Hilfsmethoden
    // -------------------------

    public static boolean hasNumericExtension(Path path) {
        if (path == null || path.getFileName() == null) return false;

        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;

        String ext = name.substring(dot + 1);
        for (int i = 0; i < ext.length(); i++) {
            if (!Character.isDigit(ext.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLikelyTracorFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;

        try {
            long size = Files.size(path);
            return size == SIZE_LONG || size == SIZE_MID || size == SIZE_SHORT;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isTracorCandidate(Path path) {
        return hasNumericExtension(path) && isLikelyTracorFile(path);
    }

    /**
     * Konvertiert ein Tracor-File in eine .spe-Datei im selben Ordner.
     * Zielname: originaler Dateiname + ".spe"
     */
    public static Path convertToSpeBesideSource(Path inputPath, boolean info, boolean overwrite) throws IOException {
        if (inputPath == null) {
            throw new IllegalArgumentException("inputPath ist null");
        }

        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Datei nicht gefunden: " + inputPath);
        }

        if (!isTracorCandidate(inputPath)) {
            throw new IllegalArgumentException("Kein erkanntes Tracor-File: " + inputPath);
        }

        // Erst Tracor-Datei lesen, damit wir den internen Namen kennen
        Result res = tracorDatenToSpe(inputPath.toString(), info, false, null);

        String rawName = res.name != null ? res.name.trim() : "";
        if (rawName.isEmpty()) {
            throw new IOException("Tracor-Datei enthält keinen gültigen Namen im Header.");
        }

        // Dateinamen etwas absichern
        String safeName = rawName.replaceAll("[\\\\/:*?\"<>|]", "_");

        Path outPath = inputPath.resolveSibling(safeName + ".SPE");

        if (Files.exists(outPath) && !overwrite) {
            return outPath;
        }

        writeSpeFile(
                outPath.toString(),
                res.name,
                res.measTimeSec,
                res.voltageKv,
                res.currentmA,
                res.filter,
                res.lv,
                res.ySpec,
                res.xValues
        );

        return outPath;
    }

    // -------------------------
    // Public API (bestehend)
    // -------------------------
    public static Result tracorDatenToSpe(String filePath, boolean info, boolean saveSpe, String outDirOrPrefix) throws IOException {
        byte[] content = readFile(filePath);

        int[] ySpec;
        double[] xValues;

        if (content.length == SIZE_LONG) {
            IntDoublePair pair = convertToInt(content, 184, 8376);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else if (content.length == SIZE_MID) {
            IntDoublePair pair = convertToInt(content, 184, 4280);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else if (content.length == SIZE_SHORT) {
            IntDoublePair pair = convertToInt(content, 184, 2232);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else {
            throw new IllegalArgumentException(
                    "Unerwartete Dateigröße: " + content.length + " bytes (erwartet: 8424, 4328, 2280)"
            );
        }

        int measTimeSec = convertToUShort(content, content.length - 30, content.length - 28).get(0);
        int voltageKv   = convertToUShort(content, content.length - 48, content.length - 46).get(0);
        int currentRaw  = convertToUShort(content, content.length - 46, content.length - 44).get(0);
        double currentmA = currentRaw * 1e-2;

        String name   = convertToCharFiltered(content, 22, 35);
        String filter = convertToCharFiltered(content, content.length - 44, content.length - 35);
        String lv     = convertToCharFiltered(content, content.length - 14, content.length - 8);

        if (info) {
            System.out.println("Messzeit: " + measTimeSec + " sec, Spannung: " + voltageKv + " kV, Strom: " + currentmA + " mA");
            System.out.println("Name: " + name + ", Filter: " + filter + ", L/V: " + lv);
        }

        if (saveSpe) {
            if (outDirOrPrefix == null) {
                throw new IllegalArgumentException("outDirOrPrefix darf bei saveSpe=true nicht null sein");
            }
            String outPath = outDirOrPrefix + name + ".spe";
            writeSpeFile(outPath, name, measTimeSec, voltageKv, currentmA, filter, lv, ySpec, xValues);
        }

        return new Result(ySpec, xValues, name, measTimeSec, voltageKv, currentmA, filter, lv);
    }

    public static class Result {
        public final int[] ySpec;
        public final double[] xValues;
        public final String name;
        public final int measTimeSec;
        public final int voltageKv;
        public final double currentmA;
        public final String filter;
        public final String lv;

        public Result(int[] ySpec, double[] xValues, String name,
                      int measTimeSec, int voltageKv, double currentmA,
                      String filter, String lv) {
            this.ySpec = ySpec;
            this.xValues = xValues;
            this.name = name;
            this.measTimeSec = measTimeSec;
            this.voltageKv = voltageKv;
            this.currentmA = currentmA;
            this.filter = filter;
            this.lv = lv;
        }
    }

    private static byte[] readFile(String filePath) throws IOException {
        try (InputStream in = new FileInputStream(filePath)) {
            return in.readAllBytes();
        }
    }

    private static IntDoublePair convertToInt(byte[] content, int from, int toExclusive) {
        int len = toExclusive - from;
        if (len % 4 != 0) {
            throw new IllegalArgumentException("Int-Bereich ist nicht durch 4 teilbar: " + len);
        }

        int n = len / 4;
        int[] ints = new int[n];
        double[] xs = new double[n];

        ByteBuffer bb = ByteBuffer.wrap(content, from, len).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < n; i++) {
            ints[i] = bb.getInt();
            xs[i] = i;
        }
        return new IntDoublePair(ints, xs);
    }

    private static class IntDoublePair {
        final int[] ints;
        final double[] xs;

        IntDoublePair(int[] ints, double[] xs) {
            this.ints = ints;
            this.xs = xs;
        }
    }

    private static List<Integer> convertToUShort(byte[] content, int from, int toExclusive) {
        int len = toExclusive - from;
        if (len % 2 != 0) {
            throw new IllegalArgumentException("UShort-Bereich ist nicht durch 2 teilbar: " + len);
        }

        int n = len / 2;
        List<Integer> out = new ArrayList<>(n);

        ByteBuffer bb = ByteBuffer.wrap(content, from, len).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            int unsigned = Short.toUnsignedInt(bb.getShort());
            out.add(unsigned);
        }
        return out;
    }

    private static String convertToCharFiltered(byte[] content, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            char c = (char) (content[i] & 0xFF);
            if (Character.isLetter(c) || Character.isWhitespace(c) || Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private static void writeSpeFile(String outPath,
                                     String name,
                                     int measTimeSec,
                                     int voltageKv,
                                     double currentmA,
                                     String filter,
                                     String lv,
                                     int[] ySpec,
                                     double[] xValues) throws IOException {

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.UTF_8))) {

            w.write("$SPEC_ID:\n");
            w.write(name);
            w.write("\n");

            w.write("$DATE_MEA:\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
            w.write(LocalDateTime.now().format(fmt));
            w.write("\n");

            w.write("$MEAS_TIM:\n");
            w.write(rjust(Integer.toString(measTimeSec), 8));
            w.write("\n");

            w.write("$MCA_CAL:\n");
            w.write("3\n");
            w.write("0.000000e+000 2.026000e-002 0.000000e+000\n");

            w.write("$Info:\n");
            w.write("Messzeit:" + measTimeSec + "sec, Spannung:" + voltageKv + "kV, Strom:" + currentmA + "mA\n");
            w.write("Name:" + name + ", Filter:" + filter + ", L/V:" + lv + "\n");

            w.write("$DATA:\n");
            w.write(rjust(Integer.toString(0), 10));
            w.write(rjust(Integer.toString(xValues.length - 1), 10));
            w.write("\n");

            for (int i = 0; i < xValues.length; i++) {
                w.write(rjust(Integer.toString(ySpec[i]), 8));
                if (i % 10 == 0) {
                    w.write("\n");
                }
            }
        }
    }

    private static String rjust(String s, int width) {
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }
}