package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsrParser {

    public static class FitResult {
        public final String sectionName; // "Fe K", "Si L"
        public final double fitarea;
        public FitResult(String sectionName, double fitarea) {
            this.sectionName = sectionName;
            this.fitarea = fitarea;
        }
        @Override public String toString() { return sectionName + ": " + fitarea; }
    }

    /** Hauptfunktion: liest Peaks aus .ASR */
    public static List<FitResult> extractPeaks(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return List.of();

        // 1) Position der "peaks"-Marke suchen (tolerant)
        int startIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l == null) continue;
            String low = l.toLowerCase(Locale.ROOT);
            if (low.matches(".*\\bpeaks\\b.*")) { // irgendwo in der Zeile
                startIdx = i + 1; // Daten ab nächster Zeile
                break;
            }
        }

        // 2) Regex für Datenzeilen:
        //    Z   Serie(1/2)   Energie   Intensität   (…optional Rest…)
        //    Dezimal: Punkt ODER Komma, optional Exponent
        String dbl = "([+-]?(?:\\d+(?:[\\.,]\\d*)?|[\\.,]\\d+)(?:[eE][+-]?\\d+)?)";
        Pattern row = Pattern.compile("^\\s*(\\d+)\\s+([12])\\s+" + dbl + "\\s+" + dbl + "(?:\\s+.*)?$");

        List<FitResult> out = new ArrayList<>();

        // 3) Primär ab 'peaks' parsen, sonst Fallback: gesamtes File scannen
        if (startIdx >= 0) {
            parseBlock(lines.listIterator(startIdx), row, out);
        } else {
            parseBlock(lines.listIterator(0), row, out);
        }

        if (out.isEmpty()) {
            System.err.println("[ASR] Warnung: 0 Peaks geparst – passt das Format der Datei?");
            // Debughilfe: die ersten 30 Zeilen ausgeben
            for (int i = 0; i < Math.min(30, lines.size()); i++) {
                System.err.println(String.format("[ASR] %03d: %s", i + 1, lines.get(i)));
            }
        }
        return out;
    }

    private static void parseBlock(ListIterator<String> it, Pattern row, List<FitResult> out) {
        while (it.hasNext()) {
            String raw = it.next();
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;

            Matcher m = row.matcher(line);
            if (!m.matches()) continue;

            int z = safeParseInt(m.group(1), -1);
            int serie = safeParseInt(m.group(2), -1);
            if (z < 1 || serie < 1) continue;

            // m.group(3) = Energie (ignoriert)
            double intensity = safeParseDouble(m.group(4));
            if (!Double.isFinite(intensity)) continue;

            String ele = symbolFromZ(z);
            String KL  = (serie == 1) ? "K" : "L";
            out.add(new FitResult(ele + " " + KL, intensity));
        }
    }

    public static List<FitResult> extractPeaks(String filename) throws IOException {
        return extractPeaks(Path.of(filename));
    }

    public static Map<String, List<FitResult>> groupByElement(List<FitResult> list) {
        Map<String, List<FitResult>> grouped = new LinkedHashMap<>();
        for (FitResult fr : list) {
            String[] parts = fr.sectionName.split("\\s+");
            String ele = parts.length > 0 ? parts[0] : fr.sectionName;
            grouped.computeIfAbsent(ele, k -> new ArrayList<>()).add(fr);
        }
        return grouped;
    }

    public static Map<String, int[]> getCountsPerElement(Map<String, List<FitResult>> grouped) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Map.Entry<String, List<FitResult>> e : grouped.entrySet()) {
            int sumK = 0, sumL = 0;
            for (FitResult fr : e.getValue()) {
                if (fr.sectionName.endsWith(" K")) sumK += Math.round(fr.fitarea);
                else if (fr.sectionName.endsWith(" L")) sumL += Math.round(fr.fitarea);
            }
            counts.put(e.getKey(), new int[]{sumK, sumL});
        }
        return counts;
    }

    // ---------- Helpers ----------

    private static String symbolFromZ(int z) {
        if (z >= 1 && z <= Elementsymbole.values().length) {
            return Elementsymbole.values()[z - 1].name();
        }
        return "Z" + z; // falls dein Enum nicht bis dahin geht
    }

    private static int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static double safeParseDouble(String s) {
        if (s == null) return Double.NaN;
        s = s.trim().replace(',', '.');
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
    }

    // --- Mini-Demo ---
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Bitte ASR-Datei als Argument übergeben.");
            return;
        }
        Path p = Path.of(args[0]);
        if (!Files.exists(p)) {
            System.err.println("Datei nicht gefunden: " + p);
            return;
        }

        var peaks   = extractPeaks(p);
        System.out.println("Peaks gelesen: " + peaks.size());
        peaks.stream().limit(5).forEach(x -> System.out.println("  " + x));

        var grouped = groupByElement(peaks);
        var counts  = getCountsPerElement(grouped);
        counts.forEach((ele, v) -> System.out.printf("%s: K=%d, L=%d%n", ele, v[0], v[1]));
    }
}
