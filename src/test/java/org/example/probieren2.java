package org.example;

import java.util.Arrays;
import java.util.List;

public class probieren2 {

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



        double[] relKonzDark = calcDark.startwerte(Z,darkMatrix);

        System.out.println("Startkonzentrationen in %:");
        for (int i = 0; i < relKonzDark.length; i++) {
            System.out.printf("Element %d: %.4f %%\n", i, relKonzDark[i]);
        }



        //double[] optimum = calcDark.optimizeWithBOBYQA( Z,darkMatrix);
        double[] optimum = {35.9572,29.6599, 34.3829 };   //python
        System.out.println("Optimale Parameter: " + Arrays.toString(optimum));
        calcDark.printOptimizedResult(optimum,darkMatrix,Z);



        double[] res1 = {35.9572,29.6599, 34.3829 };
        double [] res_be1 = calcDark.berechnenResiduum(res1, darkMatrix,Z);

        for (int i = 0; i < res_be1.length; i++) {
            System.out.printf("Res %d: %.2f %%\n", i, res_be1[i]);
        }

        //calcDark.printOptimizedResult(res1,darkMatrix,Z);

        //double[] alg = {3.169383350293745, -87.01327919332516, -100.86898770051452};
        double[] alg = {0.7314573261495299, 0.3169405474094008, 0.3674085315493202};
        calcDark.printOptimizedResult(alg,darkMatrix,Z);

        double[] alg_angepasst = calcDark.applyZAnpassen(alg,darkMatrix,17);
        System.out.println("Optimale Parameter alg_angepasst: " + Arrays.toString(alg_angepasst));

        double[] res2 = {0.7314573261495299, 0.3169405474094008, 0.3674085315493202};

        double [] res_be2 = calcDark.berechnenResiduum(res2, darkMatrix,Z);

        for (int i = 0; i < res_be2.length; i++) {
            System.out.printf("ResAlglib %d: %.2f %%\n", i, res_be2[i]);
        }











    }
}
