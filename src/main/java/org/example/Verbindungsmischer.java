package org.example;

import java.util.*;

public class Verbindungsmischer {

    /**
     * Addiert beliebig viele Verbindungen mit Faktoren/Gewichten.
     * Die Konzentrationen werden am Ende normiert.
     *
     * @param verbindungen Liste der Verbindungen
     * @param faktoren     Zuordnung, wie oft jede Verbindung gezählt werden soll
     * @return Neue Verbindung (gleicher Energie-Bereich und dateipfad wie die erste Verbindung!)
     */
    public static Verbindung addiereVerbindungen(List<Verbindung> verbindungen, List<Double> faktoren) {
        // Map: Symbol -> aufsummierte Menge
        Map<String, Double> gesamtkonzent = new HashMap<>();
        double Emin = verbindungen.get(0).getEmin();
        double Emax = verbindungen.get(0).getEmax();
        double step = verbindungen.get(0).getStep();
        String dateipfad = verbindungen.get(0).getDateipfad();
        double dichte = 0;
        double summeFaktoren = faktoren.stream().mapToDouble(Double::doubleValue).sum();

        for (int v = 0; v < verbindungen.size(); v++) {
            Verbindung verbindung = verbindungen.get(v);
            double faktor = faktoren.get(v);
            String[] symbole = verbindung.getSymbole();
            double[] konz = verbindung.getKonzentrationen();
            //System.out.println(("konz[v]"));
            //System.out.println((konz[v]));
            dichte += faktoren.get(v) * verbindung.getDichte();
            for (int i = 0; i < symbole.length; i++) {
                gesamtkonzent.put(symbole[i],
                        gesamtkonzent.getOrDefault(symbole[i], 0.0) + konz[i] * faktor);
            }
        }

        // Normieren
        double sum = gesamtkonzent.values().stream().mapToDouble(Double::doubleValue).sum();
        List<String> sortiert = new ArrayList<>(gesamtkonzent.keySet());
        //Collections.sort(sortiert); // alphabetisch;
        sortiert.sort(Comparator.comparingInt(s -> Elementsymbole.valueOf(s).ordinal()));  //nach Ordnungszahl

        String[] neueSymbole = new String[sortiert.size()];
        double[] neueKonz = new double[sortiert.size()];

        for (int i = 0; i < sortiert.size(); i++) {
            neueSymbole[i] = sortiert.get(i);
            neueKonz[i] = gesamtkonzent.get(sortiert.get(i)) / sum;
        }
        Verbindung Verb_neu = new Verbindung(neueSymbole, neueKonz, Emin, Emax, step, dateipfad, dichte/summeFaktoren);
        Verb_neu.zuAtomprozent();
        return Verb_neu;

        //return new Verbindung(neueSymbole, neueKonz, Emin, Emax, step, dateipfad, dichte/summeFaktoren);
    }
}
