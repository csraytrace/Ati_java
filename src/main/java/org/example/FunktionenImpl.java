package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunktionenImpl implements Funktionen {
    @Override
    public Verbindung parseVerbindung(String input, double Emin, double Emax, double Step, String dateipfad) {
        input = input.trim();

        if (input.contains("+")) {
            // Mischung erkannt
            return parseMischung(input, Emin, Emax, Step, dateipfad);
        }

        // Optional: nur Zahl als Symbol akzeptieren, falls gewünscht
        if (input.matches("\\d{1,3}")) {
            String[] symbole = new String[]{ input };
            double[] konz   = new double[]{ 1.0 };
            return new Verbindung(symbole, konz, Emin, Emax, Step, dateipfad, 0);
        }

        // Universelles Pattern: [Faktor]? [Formel] [Dichte]?
        Pattern splitPattern = Pattern.compile("^\\s*(\\d*\\.?\\d+)?\\s*([A-Za-z][A-Za-z0-9]*)\\s*(\\d*\\.?\\d*)\\s*$");
        Matcher m = splitPattern.matcher(input);
        if (!m.matches()) {
            throw new IllegalArgumentException("Eingabe nicht erkannt: " + input);
        }

        // Gruppe 1: optionaler Faktor (meistens ignorierbar)
        String formel = m.group(2);
        String dichteStr = m.group(3);
        double dichte = (dichteStr == null || dichteStr.isEmpty()) ? 0.0 : Double.parseDouble(dichteStr);

        // Elemente extrahieren
        Map<Elementsymbole, Double> map = new HashMap<>();
        Pattern p = Pattern.compile("([A-Z][a-z]?)(\\d*\\.?\\d*)");
        Matcher m2 = p.matcher(formel);

        try {
            while (m2.find()) {
                String symbol = m2.group(1);
                double anzahl = m2.group(2).isEmpty() ? 1.0 : Double.parseDouble(m2.group(2));
                Elementsymbole element;
                try {
                    element = Elementsymbole.valueOf(symbol);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unbekanntes Element: " + symbol);
                }
                map.put(element, map.getOrDefault(element, 0.0) + anzahl);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Fehler beim Parsen der Formel: " + ex.getMessage(), ex);
        }

        List<Elementsymbole> sorted = new ArrayList<>(map.keySet());
        sorted.sort(Comparator.comparingInt(Enum::ordinal));

        String[] symbole = new String[sorted.size()];
        double[] konz = new double[sorted.size()];
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        for (int i = 0; i < sorted.size(); i++) {
            symbole[i] = sorted.get(i).name();
            konz[i] = map.get(sorted.get(i)) / sum;
        }

        return new Verbindung(symbole, konz, Emin, Emax, Step, dateipfad, dichte);
    }


    /*public Verbindung parseVerbindung(String input, double Emin, double Emax, double Step, String dateipfad) {

        if (input.contains("+")) {
            // Mischung erkannt
            return parseMischung(input, Emin, Emax, Step, dateipfad);
        }

        String formelPart = input.trim().split("\\s+")[0];
        if (formelPart.matches("(?i)[A-Za-z]{1,2}")    // 1–2 Buchstaben, case-insensitive
                || formelPart.matches("\\d{1,3}")             // 1–3 Ziffern
        ) {
            String[] symbole = new String[]{ formelPart };
            double[] konz   = new double[]{ 1.0 };
            return new Verbindung(symbole, konz, Emin, Emax, Step, dateipfad, 0);
        }
        String formel;
        double dichte = 0;
        input = input.trim();
        Pattern splitPattern = Pattern.compile("^([A-Za-z0-9\\.]+)\\s*(\\d*\\.?\\d*)$");
        Matcher splitMatcher = splitPattern.matcher(input);
        if (splitMatcher.matches()) {
            formel = splitMatcher.group(1);
            String wertStr = splitMatcher.group(2);
            if (wertStr != null && !wertStr.isEmpty()) {
                dichte = Double.parseDouble(wertStr);
            }
        } else {
            throw new IllegalArgumentException("Eingabe nicht erkannt: " + input);
        }

        Map<Elementsymbole, Double> map = new HashMap<>();
        Pattern p = Pattern.compile("([A-Z][a-z]?)(\\d*\\.?\\d*)");
        Matcher m = p.matcher(formel);

        try {
            while (m.find()) {
                String symbol = m.group(1);
                double anzahl = m.group(2).isEmpty() ? 1.0 : Double.parseDouble(m.group(2));
                Elementsymbole element;
                try {
                    element = Elementsymbole.valueOf(symbol);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unbekanntes Element: " + symbol);
                }
                map.put(element, map.getOrDefault(element, 0.0) + anzahl);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Fehler beim Parsen der Formel: " + ex.getMessage(), ex);
        }

        List<Elementsymbole> sorted = new ArrayList<>(map.keySet());
        sorted.sort(Comparator.comparingInt(Enum::ordinal));

        String[] symbole = new String[sorted.size()];
        double[] konz = new double[sorted.size()];
        double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
        for (int i = 0; i < sorted.size(); i++) {
            symbole[i] = sorted.get(i).name();
            konz[i] = map.get(sorted.get(i)) / sum;
        }

        return new Verbindung(symbole, konz, Emin, Emax, Step, dateipfad, dichte);
    }*/

    public Verbindung parseMischung(String input, double Emin, double Emax, double Step, String dateipfad) {
        // 1) Splitte an '+'
        String[] teile = input.split("\\+");
        List<Verbindung> verbindungen = new ArrayList<>();
        List<Double> faktoren     = new ArrayList<>();

        // 2) Pattern für jeden Term: [Stückzahl] [Formel] [Dichte?]
        //Pattern termP = Pattern.compile    ("^\\s*(\\d*\\.?\\d+)\\s+([A-Za-z0-9]+)\\s*(\\d*\\.?\\d*)\\s*$");
        Pattern termP = Pattern.compile("^\\s*(\\d*\\.?\\d+)\\s+([A-Za-z][a-zA-Z0-9]*)\\s*(\\d*\\.?\\d*)\\s*$");
        for (String teil : teile) {
            Matcher m = termP.matcher(teil.trim());
            if (!m.matches()) {
                System.out.println("Matching failed für: '" + teil.trim() + "'");
                throw new IllegalArgumentException("Ungültiger Misch-Term: " + teil);
            }
            double faktor = Double.parseDouble(m.group(1));
            String formel = m.group(2);
            double dichte = m.group(3).isEmpty() ? 0.0 : Double.parseDouble(m.group(3));

            // 3) Einzel-Verbindung erzeugen, übergebe Dichte als letzte Zahl
            Verbindung v = parseVerbindung(formel + (dichte>0? " "+dichte : ""),
                    Emin, Emax, Step, dateipfad);
            verbindungen.add(v);
            faktoren.add(faktor);
        }

        // 4) Jetzt alles mischen
        return Verbindungsmischer.addiereVerbindungen(verbindungen, faktoren);
    }

    public double[] Filter_array(List<Verbindung> Verbindungen) {
        // Finde eine typische Energielänge (z.B. vom Standardmaterial)
        int n = 0;
        if (!Verbindungen.isEmpty()) {
            n = Verbindungen.get(0).getEnergieArray().length;
        } else {
            // Wenn wirklich gar nichts da ist, gib ein leeres Array zurück
            return new double[0];
        }

        if (Verbindungen.isEmpty()) {
            double[] ones = new double[n];
            Arrays.fill(ones, 1.0);
            return ones;
        }

        List<double[]> filterListen = new ArrayList<>();
        for (Verbindung v : Verbindungen) {
            double[] filter = v.erzeuge_Filter_liste(v.getFensterDickeCm());
            filterListen.add(filter);
        }
        return multipliziereArrays(filterListen);
    }


    // Hilfsfunktion
    public double[] multipliziereArrays(List<double[]> arrays) {
        if (arrays == null || arrays.isEmpty()) return new double[0];
        int n = arrays.get(0).length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double prod = 1.0;
            for (double[] arr : arrays) {
                prod *= arr[i];
            }
            result[i] = prod;
        }
        return result;
    }
}
