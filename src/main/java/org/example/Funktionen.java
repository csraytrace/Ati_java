package org.example;

import java.util.List;
public interface Funktionen {
    /**
     * Parst eine chemische Summenformel (z.B. "CaCO3"), ergänzt fehlende Atomzahlen mit 1,
     * prüft gegen Enum Elementsymbole, sortiert nach Ordnungszahl und normiert die Anteile auf 1.
     *
     * @param formel Die Summenformel, z.B. "CaCO3"
     * @return Verbindung mit sortierten Symbolen und normierten Konzentrationen
     * @throws IllegalArgumentException falls unbekanntes Element enthalten ist
     */
    Verbindung parseVerbindung(String formel, double Emin, double Emax, double Step, String dateipfad);
    Verbindung parseMischung(String eingabe, double Emin, double Emax, double step, String dateipfad);
    double[] Filter_array(List<Verbindung> Verbindungen);
    double[] multipliziereArrays(List<double[]> arrays);

}
