package org.example;

import java.util.Arrays;
import java.util.List;

public class probieren6 {

    public static void main(String[] args)


    {


        List<String> elementSymboleDark = Arrays.asList("Si", "Cu", "O");
        List<Integer> elementIntDark = Arrays.asList(1, 12, 55);

        double Emin = 0;
        double Emax = 30;
        double step = 0.01;

        Probe probeDark = new Probe(elementSymboleDark, "MCMASTER.TXT", Emin, Emax, step, elementIntDark);


        probeDark.setzeUebergangAktivFuerElementKAlpha(0);
        probeDark.setzeUebergangAktivFuerElementKAlpha(1);


        Funktionen f = new FunktionenImpl();


        String ver1 = "2 HO + 0.5 HeO + 0.5 HeO";

        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");


        v1.multipliziereKonzentrationen(0.5);


        double[] konz = v1.getKonzentrationen();
        String[] symbol = v1.getSymbole();
        for (int i = 0; i < konz.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol[i], konz[i]);
        }



        // 4. Erzeuge CalcI-Instanz
        CalcIDark calcDark = new CalcIDark(
                "MCMASTER.TXT",  // dateipfad
                probeDark,
                "widerschwinger", // Röhrentyp (oder "lovescott")
                "Rh",             // Röhrenmaterial
                20, 70,           // Einfallswinkel alpha, beta
                0,                // Fensterwinkel
                1,                // sigma
                1,                // charzucontL
                "Be",             // Fenstermaterial Röhre
                125,              // Fensterdicke Röhre (µm)
                1,                // Raumwinkel
                0.01,             // Röhrenstrom (A)
                Emin, Emax,            // Emin, Emax
                step,             // step
                30,               // Messzeit
                1,                // charzucont
                "Be",             // Fenstermaterial Detektor
                7.62,             // Fensterdicke Detektor (µm)
                0,                // phi Detektor
                "Au",             // Kontaktmaterial
                50,               // Kontaktmaterialdicke (nm)
                1,                // Bedeckungsfaktor
                45,               // palpha Grad
                45,               // pbeta Grad
                "Si",             // Detektormaterial
                0.05,             // Totschicht (µm)
                3,                // activeLayer (mm)
                null,            // Filter-Liste
                null,
                v1
        );

        List<Double> darkMatrixList = Arrays.asList(1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 17;

        int[] index = calcDark.getIndexBind();


        System.out.println("Indices: " + java.util.Arrays.toString(index));





    }
    }
