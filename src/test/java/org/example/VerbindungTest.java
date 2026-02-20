package org.example;

import java.io.IOException;
import java.util.*;

public class VerbindungTest {

    public static void main(String[] args) throws IOException {





        Funktionen f = new FunktionenImpl();
        double Emin = 0;
        double Emax = 30;
        double step = 0.01;
        //String ver1 = "2 HO + 0.5 HeO + 0.5 HeO";
        String ver1 = "1.04 C38H76N2O2 + 1 CeO2 + 0.84 ZnO + 1.06 Cr2O3 + 0.98 TiO2 + 0.6 Al2O3";
        //String ver2 = "0.844 C10H8O4 + 0.156 TiO2";
        String ver2 = "15.57 TiO2 + 84.43 C10H8O4";

        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");
        //System.out.println(v2.getWeight() / v1.getWeight() );
        double[] konz = v1.getKonzentrationen();
        String[] symbol = v1.getSymbole();
        for (int i = 0; i < konz.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol[i], konz[i]);
        }
        System.out.println(v1.getDichte());
        System.out.println(v1.getWeight());
        System.out.println(v1.Z_gemittelt());

        Verbindung v2 = f.parseVerbindung(ver2,Emin, Emax, step, "McMaster.txt");
        v2.zuAtomprozent();
        double[] konz2 = v2.getKonzentrationen();
        String[] symbol2 = v2.getSymbole();
        for (int i = 0; i < konz2.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol2[i], konz2[i]);
        }

        System.out.println(v2.getDichte());
        System.out.println(v2.getWeight());
        System.out.println(v2.Z_gemittelt());


        v1.zuAtomprozent();
        konz = v1.getKonzentrationen();
        //v1.zuMassenprozent();

        //v1.multipliziereKonzentrationen(0.1);


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
