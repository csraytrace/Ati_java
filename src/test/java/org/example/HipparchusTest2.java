package org.example;

import java.util.Arrays;
import java.util.List;

import static org.example.XrfFixture.DARK_MATRIX;
import static org.example.XrfFixture.Z_MITTEL;

public class HipparchusTest2 {


    public static void main(String[] args) {


        List<String> elementSymboleDark = Arrays.asList("Si", "Cu", "Ca", "K", "Ni", "O");
        List<Integer> elementIntDark = Arrays.asList(1, 12, 11, 14, 16, 55);

        double Emin = 0;
        double Emax = 30;
        double step = 0.01;

        Probe probeDark = new Probe(elementSymboleDark, "MCMASTER.TXT", Emin, Emax, step, elementIntDark);


        probeDark.setzeUebergangAktivFuerElementKAlpha(0);
        probeDark.setzeUebergangAktivFuerElementKAlpha(1);
        probeDark.setzeUebergangAktivFuerElementKAlpha(2);
        probeDark.setzeUebergangAktivFuerElementKAlpha(3);
        probeDark.setzeUebergangAktivFuerElementKAlpha(4);
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
                null
        );

        List<Double> darkMatrixList = Arrays.asList(1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 17;

        double[] optimum = calcDark.optimizeWithHIPPARCHUS_MINLM_Einfach(Z, darkMatrix);
        System.out.println("optimum optimum: " + Arrays.toString(optimum));
        calcDark.printOptimizedResultEinfach(optimum, darkMatrix, Z);




    }
}