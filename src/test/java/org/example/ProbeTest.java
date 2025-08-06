package org.example;

import java.util.List;

public class ProbeTest {
    public static void main(String[] args) {
        // Beispiel-Elemente für Test
        List<Object> symbole = List.of("C", "O", "Al", "Si", "Fe"); // Z: 6, 8, 13, 14, 26
        List<Double> ints = List.of(0.5, 0.2, 0.7, 1.0, 0.9);

        // Test-Probe erstellen
        Probe p = new Probe(symbole, "McMaster.txt", 1.0, 10.0, 0.1, ints);

        // Gefilterte Probe mit Z > 12
        Probe gefiltert = p.filterByMinZ(2);

        // Ausgabe prüfen:
        System.out.println("Gefilterte Elemente (Z > 12):");
        for (Element e : gefiltert.getElemente()) {
            System.out.println("Symbol: " + e.getSymbol() + ", Z: " + e.getAtomicNumber());
        }
        System.out.println("Gefilterte Intensitäten:");
        for (double d : gefiltert.getIntensitäten()) {
            System.out.println(d);
        }
    }
}

