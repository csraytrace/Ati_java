package org.example;

import java.util.Arrays;
import java.util.List;

public class restest {
    public static void main(String[] args) {


        List<String> elementSymboleDark = Arrays.asList("O","Si","Cu");
        List<Integer> elementIntDark = Arrays.asList(1,12,55);

        double Emin = 0;
        double Emax = 30;
        double step = 0.01;

        Probe probeDark = new Probe(elementSymboleDark , "MCMASTER.TXT", Emin, Emax, step,elementIntDark );


        probeDark.setzeUebergangAktivFuerElementKAlpha(1);
        probeDark.setzeUebergangAktivFuerElementKAlpha(2);




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
                null
        );

        List<Double> darkMatrixList = Arrays.asList(1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 17;


        //double[] res1 = {0.359572,0.296599, 0.343829 };
        //double[] res1 = {0.3,0.2, 0.3 };
        double[] res1 = {18.53361483, 54.05293923, 27.41344593 };
        double[] res2 = {30,29, 34 };
        double[] res3 = {2,5, 8};


        double[] res1_einfach = {18.53361483, 27.41344593 };
        double[] res2_einfach = {30,29};
        double[] res3_einfach = {2,5};
        double [] res_be1 = calcDark.berechnenResiduum(res1, darkMatrix,Z);

        for (int i = 0; i < res_be1.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be1[i]);
        }

        double [] res_be2 = calcDark.berechnenResiduum(res2, darkMatrix,Z);

        for (int i = 0; i < res_be1.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be2[i]);
        }

        double [] res_be3 = calcDark.berechnenResiduum(res3, darkMatrix,Z);

        for (int i = 0; i < res_be1.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be3[i]);
        }




        double [] res_be1einfach = calcDark.testberechnenResiduumEinfach(res1_einfach, darkMatrix,Z,1,54.05293923);

        for (int i = 0; i < res_be1einfach.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be1einfach[i]);
        }

        double [] res_be2einfach = calcDark.testberechnenResiduumEinfach(res2_einfach, darkMatrix,Z,2,34);

        for (int i = 0; i < res_be2einfach.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be2einfach[i]);
        }

        double [] res_be3einfach = calcDark.testberechnenResiduumEinfach(res3_einfach, darkMatrix,Z,2,8);

        for (int i = 0; i < res_be3einfach.length; i++) {
            System.out.printf("Res %d: %.2f \n", i, res_be3einfach[i]);
        }




    }




}
