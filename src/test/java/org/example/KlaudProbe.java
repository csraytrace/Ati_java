package org.example;

import java.util.Arrays;
import java.util.List;

public class KlaudProbe {



    public static void main(String[] args) {


        List<String> elementSymboleDark = Arrays.asList("H", "C", "N", "O", "Si", "Ti");
        List<Integer> elementIntDark = Arrays.asList( 0, 0, 0, 0, 8785,98652);


        double Emin = 0;
        double Emax = 40;
        double step = 0.05;

        Probe probeDark = new Probe(elementSymboleDark, "MCMASTER.TXT", Emin, Emax, step, elementIntDark);


        probeDark.setzeUebergangAktivFuerElementKAlpha(0);
        probeDark.setzeUebergangAktivFuerElementKAlpha(1);
        probeDark.setzeUebergangAktivFuerElementKAlpha(2);
        probeDark.setzeUebergangAktivFuerElementKAlpha(3);
        probeDark.setzeUebergangAktivFuerElementKAlpha(4);
        probeDark.setzeUebergangAktivFuerElementKAlpha(5);
        //probe.setzeUebergangAktivFuerElementLAlpha(1);



        Funktionen f = new FunktionenImpl();


        String ver1 = "1 C38H76N2O2";

        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");


        v1.multipliziereKonzentrationen(1/5.67);




//sigma=8.30349567e-01
        //binder=[[4.67,1],["1C38H76N2O2"]]

        // 4. Erzeuge CalcI-Instanz
        CalcIDark calcDark = new CalcIDark(
                "MCMASTER.TXT",  // dateipfad
                probeDark,
                "widerschwinger", // Röhrentyp (oder "lovescott")
                "Rh",             // Röhrenmaterial
                20, 70,           // Einfallswinkel alpha, beta
                0,                // Fensterwinkel
                8.30349567e-01,                // sigma
                0.3,                // charzucontL
                "Be",             // Fenstermaterial Röhre
                125,              // Fensterdicke Röhre (µm)
                1,                // Raumwinkel
                0.01,             // Röhrenstrom (A)
                Emin, Emax,            // Emin, Emax
                step,             // step
                500,               // Messzeit
                9.46173852e-01,                // charzucont
                "Be",             // Fenstermaterial Detektor
                7.62,             // Fensterdicke Detektor (µm)
                0,                // phi Detektor
                "Au",             // Kontaktmaterial
                2.99882970e+01,               // Kontaktmaterialdicke (nm)
                1,                // Bedeckungsfaktor
                45,               // palpha Grad
                45,               // pbeta Grad
                "Si",             // Detektormaterial
                8.56937060e-11,             // Totschicht (µm)
                3,                // activeLayer (mm)
                null,            // Filter-Liste
                null,
                v1
        );

        List<Double> darkMatrixList = Arrays.asList(0.,0.,0.,1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 12.1;

        double[] optimum = calcDark.optimizeWithHIPPARCHUS_MINLM_Einfach(Z, darkMatrix);
        System.out.println("optimum optimum: " + Arrays.toString(optimum));
        calcDark.printOptimizedResultEinfach(optimum, darkMatrix, Z);




    }
}
