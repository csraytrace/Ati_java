package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FitParserPymca {

    // Tupel: Name + fitarea-Wert
    public static class FitResult {
        public final String sectionName;
        public final double fitarea;
        public FitResult(String sectionName, double fitarea) {
            this.sectionName = sectionName;
            this.fitarea = fitarea;
        }
        public String toString() { return sectionName + ": " + fitarea; }
    }

    // Liest alle [result.*]-Sektionen samt fitarea
    public static List<FitResult> extractFitAreas(String filename) throws IOException {
        String content = new String(java.nio.file.Files.readAllBytes(new File(filename).toPath()), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("\\[result\\.([^\\]]+)](.*?)(?=\\n\\[|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        List<FitResult> fitareas = new ArrayList<>();
        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();
            String sectionContent = matcher.group(2);

            Matcher fitMatcher = Pattern.compile("fitarea\\s*=\\s*([\\-\\d\\.eE]+)").matcher(sectionContent);
            if (fitMatcher.find()) {
                try {
                    double value = Double.parseDouble(fitMatcher.group(1));
                    fitareas.add(new FitResult(sectionName, value));
                } catch (NumberFormatException ignored) {}
            }
        }
        return fitareas;
    }

    // Hilfsmethode: Gibt Elementsymbol ("Fe" oder "O" etc.) aus dem Namen zurück
    public static String getElement(String name) {
        if (name.length() >= 2 && name.charAt(1) == ' ')
            return name.substring(0,1);
        else if (name.length() >= 2)
            return name.substring(0,2);
        else
            return name;
    }

    // Extrahiert aus einem Namen nur bestimmte Übergänge
    public static List<FitResult> filterTransitions(List<FitResult> list) {
        String[] allowed = {"KL2", "KL3", "L3M4", "L3M5"};
        List<FitResult> ret = new ArrayList<>();
        for (FitResult fr : list) {
            for (String übergang : allowed) {
                if (fr.sectionName.contains(übergang) && !fr.sectionName.contains("esc")) {
                    ret.add(fr);
                    break;
                }
            }
        }
        return ret;
    }

    // Gruppiert nach Elementen und sammelt die passenden Übergänge
    public static Map<String, List<FitResult>> groupByElement(List<FitResult> results) {
        Map<String, List<FitResult>> grouped = new LinkedHashMap<>();
        for (FitResult fr : results) {
            String ele = getElement(fr.sectionName);
            grouped.putIfAbsent(ele, new ArrayList<>());
            grouped.get(ele).add(fr);
        }
        // Filtere auf erlaubte Übergänge
        for (String key : grouped.keySet()) {
            grouped.put(key, filterTransitions(grouped.get(key)));
        }
        return grouped;
    }

    // Summiert für jede Gruppe die K- und L-Übergänge
    public static Map<String, int[]> getCountsPerElement(Map<String, List<FitResult>> grouped) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (String ele : grouped.keySet()) {
            List<FitResult> frs = grouped.get(ele);
            int sumK = 0, sumL = 0;
            for (FitResult fr : frs) {
                if (fr.sectionName.contains("KL2") || fr.sectionName.contains("KL3"))
                    sumK += Math.round(fr.fitarea);
                if (fr.sectionName.contains("L3M4") || fr.sectionName.contains("L3M5"))
                    sumL += Math.round(fr.fitarea);
            }
            counts.put(ele, new int[]{sumK, sumL});
        }
        return counts;
    }

    // Hauptfunktion (Demo)
    public static void main(String[] args) throws IOException {
        //String filename = "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Tracormessungen\\Messung2025\\pymca_dat\\1486.spe_1.1.1.1.fit";
        String filename = "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Tracormessungen\\Messung2025\\KLAUD\\1633A.spe_1.1.1.1.fit";


        List<FitResult> fitAreas = extractFitAreas(filename);

        // Gruppieren
        Map<String, List<FitResult>> grouped = groupByElement(fitAreas);

        // Summen ausgeben
        Map<String, int[]> resultCounts = getCountsPerElement(grouped);
        for (String ele : resultCounts.keySet()) {
            int[] vals = resultCounts.get(ele);
            System.out.printf("%s: K-Übergänge: %d, L-Übergänge: %d%n", ele, vals[0], vals[1]);
        }
    }
}
