package org.example;

import java.util.Arrays;
import java.util.List;

public class probieren5 {


    public static void main(String[] args) {


        List<String> elementSymboleDark = Arrays.asList("Si","Cu","O");
        List<Integer> elementIntDark = Arrays.asList(1,12,55);

        double Emin = 0;
        double Emax = 30;
        double step = 0.01;

        Probe probeDark = new Probe(elementSymboleDark , "MCMASTER.TXT", Emin, Emax, step,elementIntDark );

        //probe.setzeUebergangAktivFuerElement(0, "K", "L2");
        //probe.setzeUebergangAktivFuerElement(0, "l3", "m1");
        //probe.setzeUebergangAktivFuerElement(0, "K", "L3");
        //probe.setzeUebergangAktivFuerElement(0, "K", "L1");
        probeDark.setzeUebergangAktivFuerElementKAlpha(0);
        probeDark.setzeUebergangAktivFuerElementKAlpha(1);
        //probe.setzeUebergangAktivFuerElementLAlpha(1);



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
                null
        );

        List<Double> darkMatrixList = Arrays.asList(1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 17;



        try {
            System.out.println("\n==== ALGLIB mindf: Testlauf ====");
            double[] optimumAlglib = calcDark.optimizeWithALGLIB_DF_Einfach(Z, darkMatrix);
            //System.out.println("Optimale Parameter (ALGLIB): " + Arrays.toString(optimumAlglib));

            // Ergebnis ausgeben (Z-mittel, Intensitäten etc.)
            calcDark.printOptimizedResultEinfach(optimumAlglib, darkMatrix, Z);



        } catch (Throwable t) {
            // Falls ALGLIB nicht auf dem Classpath ist oder mindf nicht verfügbar: freundlich degradieren
            System.err.println("ALGLIB-Test übersprungen: " + t.getMessage());
            // Optional: t.printStackTrace();
        }


    }





}
