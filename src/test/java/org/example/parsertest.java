package org.example;

import org.mariuszgromada.math.mxparser.License;
import org.mariuszgromada.math.mxparser.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class parsertest {

    static PwSegment constOne(double a, double b) {
        PwSegment s = new PwSegment();
        s.type = SegmentType.CONSTANT;
        s.value = 1.0;
        s.a = a; s.b = b; s.inclA = true; s.inclB = true;
        return s;
    }


    public static void main(String[] args) {
        // Beispiel 1
        MathParser h = MathParser.withDefault(0.0)
                .addExprSegment("3*x^3 + ln(x)/sin(x)", 3.0, true, 3.04, false)  // [3, 3.04)
                .addExprSegment("sqrt(x) * exp(0.2*x)", 3.04, true, 6.0,  true); // [3.04, 6]

        System.out.println(h.evaluate(2.9));   // 0.0
        System.out.println(h.evaluate(3.04));  // Wert aus erstem Segment
        System.out.println(h.evaluate(5.0));   // Wert aus zweitem Segment
        System.out.println(h.evaluate(7.0));   // 0.0
        System.out.println("\n");   // 0.0

        // Beispiel 2
        MathParser energie = new MathParser(1.0, 1e-12)
                .addExprSegment("1 / (1 + 2)", 1.0, true, 40.0, true);

        System.out.println(energie.evaluate(0.5));  // 1.0 (außerhalb)
        System.out.println(energie.evaluate(20));   // ~0.5
        System.out.println(energie.evaluate(50));
        System.out.println("\n");   // 0.0// 1.0 (außerhalb)

        // Beispiel 3
        Function fx = new Function("f(x) = x^2 + 2*x + 1");
        Function g = new Function("g(x) = ln(x) / (1 + x)");
        MathParser pw = MathParser.withDefault(0.0)
                .addFunctionSegment(fx,  0.0, true, 2.0,  false)  // [0,2)
                .addFunctionSegment(g,  2.0, true, 10.0, true);  // [2,10]

        System.out.println(pw.evaluate(1.5));  // f(1.5)
        System.out.println(pw.evaluate(3.0));  // g(3.0)
        System.out.println(pw.evaluate(12));   // 0.0 (außerhalb)



        List<String> elementSymboleDark = Arrays.asList( "C", "N", "O", "Si", "Ti");
        List<Integer> elementIntDark = Arrays.asList(  0, 0, 0, 8785,98652);


        double Emin = 0;
        double Emax = 40;
        double step = 0.01;

        Probe probeDark = new Probe(elementSymboleDark, "MCMASTER.TXT", Emin, Emax, step, elementIntDark);


        probeDark.setzeUebergangAktivFuerElementKAlpha(0);
        probeDark.setzeUebergangAktivFuerElementKAlpha(1);
        probeDark.setzeUebergangAktivFuerElementKAlpha(2);
        probeDark.setzeUebergangAktivFuerElementKAlpha(3);
        probeDark.setzeUebergangAktivFuerElementKAlpha(4);
        //probe.setzeUebergangAktivFuerElementLAlpha(1);



        Funktionen f = new FunktionenImpl();


        String ver1 = "1 C38H76N2O2";

        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");


        v1.multipliziereKonzentrationen(1/5.67);

        List<Verbindung> filter = new ArrayList<>();
        // Beispiel: Verbindung mit Dichte 0.1 und Symbol "Be"
        Verbindung beFilter = new Verbindung(
                new String[]{"Be"},
                new double[]{1.0},
                Emin, Emax, step,    // Emin, Emax, step
                "MCMASTER.TXT",
                0.1             // Dichte
        );
        beFilter.setFensterDickeCm(0.4);
        filter.add(beFilter);

        for (Verbindung fil : filter) {
            // "Identität" vorher ist nicht nötig – das hier reicht:
            fil.clearModulation(1.0); // außerhalb der Intervalle = 1.0

            // dein Segment: [3, 3.04) -> 3*x^3 + ln(x)/sin(x)
            fil.addModulationSegment("1", Emin, true, Emax, true);

            // optional weitere Bereiche anhängen:
            // f.addModulationSegment("sqrt(x) * exp(0.2*x)", 3.04, true, 6.0, true);
        }




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
                filter,
                v1
        );

        List<Double> darkMatrixList = Arrays.asList(0.,0.,1.);
        double[] darkMatrix = darkMatrixList.stream().mapToDouble(Double::doubleValue).toArray();
        double Z = 12.1;

        double[] optimum = calcDark.optimizeHIPPARCHUS(Z, darkMatrix);
        System.out.println("optimum optimum: " + Arrays.toString(optimum));
        calcDark.printOptimizedResult(optimum, darkMatrix, Z);



        PwSegment konst = constOne(2,5);
        System.out.println("optimum optimum: " + konst.toExpressionString("3"));











    }


    }

