package org.example;

import java.io.IOException;
import java.util.*;

public class VerbindungTest {

    public static void main(String[] args) throws IOException {





        Funktionen f = new FunktionenImpl();
        double Emin = 0;
        double Emax = 30;
        double step = 0.01;
        String ver1 = "2 HO + 0.5 HeO + 0.5 HeO";

        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");
        //System.out.println(v2.getWeight() / v1.getWeight() );
        double[] konz = v1.getKonzentrationen();
        String[] symbol = v1.getSymbole();
        for (int i = 0; i < konz.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol[i], konz[i]);
        }

        v1.multipliziereKonzentrationen(0.1);


        for (int i = 0; i < konz.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol[i], konz[i]);
        }


/*
        double[] verteilung = { 0.0, 0.20, 0.10 };               // [Probe, Binder1, Binder2] → intern normiert
        List<String> binderFormeln = List.of("HO", "HeO");
        int Zdark = 12;

        Binder binder = new Binder(verteilung, binderFormeln, Zdark,
                "MCMASTER.TXT", 0.0, 30.0, 0.01);

// 1) Validierung ist bereits im Konstruktor erfolgt (wirft Exception falls Verstoß)

// 2) Gemeinsame Liste (Z → Anteil)
        Map<Integer, Double> agg = binder.aggregiereBinderNachZ(); // z.B. {16:0.14, 20:0.12, 8:0.04, ...}
        System.out.println("Aggregierter Binder (Z -> Anteil / Prozent):");
        agg.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("Z=%d : %.6f  (%.4f%%)%n",
                        e.getKey(), e.getValue(), e.getValue() * 100.0));

*/



    }
}
