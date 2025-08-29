package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class TestLagrangeInterpolation {



    public static void main(String[] args) {

        double Emin=0.05;
        double Emax=45;
        double step=0.05;
        List<String[]> paraVar = Arrays.asList(
                //new String[]{"sigma"},
                new String[]{"Einfallswinkelalpha"},
                new String[]{"activeLayer"},
                new String[]{"Totschicht"},
                new String[]{"charzucontL"},
                new String[]{"charzucont"},
                new String[]{"Kontaktmaterialdicke"}

        );

        List<double[]> grenzen = Arrays.asList(
                new double[]{15,25},
                new double[]{2, 4},
                new double[]{0.0, 0.2},
                new double[]{0.1, 1.2},
                new double[]{0.8, 1.1},
                new double[]{10, 40}
        );

// Deine Liste als Tripel (Intensität, Symbol, Übergang)
        List<Object[]> elementPaare = Arrays.asList(
                new Object[]{140000, "Ag", 0},
                new Object[]{66746,  "SN", 0},
                new Object[]{163000, "TI", 0},
                new Object[]{111267, "CD", 0},
                new Object[]{433090, "CU", 0},
                new Object[]{196370, "V", 0},
                new Object[]{544606, "ZR", 0},
                new Object[]{464025, "ZN", 0},
                new Object[]{498315, "GE", 0},
                new Object[]{10844,  "AL", 0},
                new Object[]{20692,  "SI", 0},
                new Object[]{106785, "BI", 1},
                new Object[]{13396,  "CD", 1},
                new Object[]{17196,  "SN", 1},
                new Object[]{109041, "PB", 1},
                new Object[]{82738,  "TA", 1},
                new Object[]{13240,  "Ag", 1}
        );

        List<Probe> probeliste = new ArrayList<>();
        double[] gemesseneIntensitaet = new double[elementPaare.size()];
        int index = 0;
        for (Object[] eintrag : elementPaare) {
            int intensity = (int) eintrag[0];
            String symbol = (String) eintrag[1];
            int uebergang = (int) eintrag[2];

            List<String> elementSymbole = Arrays.asList(symbol);
            List<Integer> elementInt = Arrays.asList(intensity);

            Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", Emin, Emax, step, elementInt);
            gemesseneIntensitaet[index]=intensity;

            // Übergang aktivieren:
            if (uebergang == 0) {
                probe.setzeUebergangAktivFuerElementKAlpha(0);
            } else if (uebergang == 1) {
                probe.setzeUebergangAktivFuerElementLAlpha(0);
            }
            probeliste.add(probe);
            index ++;
        }



        //"sigma=0.8, raumwinkel=0.9, Einfallswinkelalpha=8, Einfallswinkelbeta=70"


        List<Verbindung> filter = new ArrayList<>();
        // Beispiel: Verbindung mit Dichte 0.1 und Symbol "Be"
        Verbindung beFilter = new Verbindung(
                new String[]{"Be"},
                new double[]{1.0},
                Emin, Emax, step,    // Emin, Emax, step
                "MCMASTER.TXT",
                0.1             // Dichte
        );
        beFilter.setFensterDickeCm(0);
        filter.add(beFilter);


        KalibrierungFunktion kal = new KalibrierungFunktion(filter,filter,3.8537950863499206E-5);



        //double[] startwerte = {15, 3, 0, 1.2, 0.95, 10};
        double[] startwerte = {18.267463928051775, 2.8623463615208564, 0, 0.1, 1.0770533966938147, 10.035482713128378};
        String para="Emin=0.05, Emax=45,step=0.05";

        boolean bedingung = true;// beliebig
        String funktion = "a*x^2 + b*x + c + d * exp(x*e)";                // Modulationsfunktion über Energie x
        Map<String, Double> funktionsParameter = Map.of(
                "a", 0.0,    // 0 ⇒ nur Offset b
                "b", 0.0,
                "c",1.,
                "d",0.,
                "e",0.
                //"a", -0.0036822973031124333,    // 0 ⇒ nur Offset b
                // "b", 0.13329511108092246,
                // "c",0.33629628051289584
        );
        double x1 = 0.05;    // Modulationsfenster (Schranken)
        double x2 = Emax;

        double[] residuen = kal.berechneResiduen(
                startwerte, paraVar, probeliste,
                bedingung, para,
                funktion, funktionsParameter,
                x1, x2);

        System.out.println("Residuen: " + Arrays.stream(residuen)
                .mapToObj(v -> String.format(Locale.US, "%.6g", v))
                .collect(Collectors.joining(", ")));

        double[] geo = kal.berechneGeo(
                startwerte, paraVar, probeliste,
                bedingung, para,
                funktion, funktionsParameter,
                x1, x2);
        System.out.println("Geo: " + Arrays.stream(geo)
                .mapToObj(v -> String.format(Locale.US, "%.6g", v))
                .collect(Collectors.joining(", ")));

        double[] moxwerte = kal.berechneUEnergie(
                startwerte, paraVar, probeliste,
                bedingung, para,
                funktion, funktionsParameter,
                x1, x2);


        System.out.println("moxwerte: " + Arrays.stream(moxwerte)
                .mapToObj(v -> String.format(Locale.US, "%.16g", v))
                .collect(Collectors.joining(", ")));


        double x0 = 3.8537950863499206E-5;
        String Moxfkt = KalibrierungFunktion.buildLagrangeExprShifted(moxwerte,geo,x0);
        System.out.println(Moxfkt);



        System.out.println("x0/Geo: " + Arrays.stream(geo)
                .mapToObj(v -> String.format(Locale.US, "%.6g", x0/v))
                .collect(Collectors.joining(", ")));

        double min = Arrays.stream(geo).map(v -> x0 / v).filter(Double::isFinite).min().orElse(Double.NaN);
        double max = Arrays.stream(geo).map(v -> x0 / v).filter(Double::isFinite).max().orElse(Double.NaN);
        System.out.println(min);
        System.out.println(max);

        String MoxfktClamped = String.format(
                java.util.Locale.US,
                "min(max(abs((%s)), %.16g), %.16g)",
                Moxfkt, min, max
        );
        System.out.println(MoxfktClamped);


        double[] ywerte = Arrays.stream(geo)
                .map(v -> x0 / v)
                .filter(Double::isFinite)
                .toArray();

        //moxwerte xWerte


        PiecewiseModel gelb = PiecewiseModel.fromPolyline(moxwerte, ywerte, null, null);

        for (Verbindung v : filter) {
            gelb.applyToFilter(v, null); // oder null, wenn du nicht multiplizieren willst
        }







        //for (Verbindung v : filter) {
         //   v.clearModulation(1.0);// außerhalb = 1
        //    v.addModulationSegment(MoxfktClamped, x1, true, 25, true); // Formel gilt auf [x1,x2]
        //}



        List<Double> geoNeuListe = new ArrayList<>();


        for (Probe sample : probeliste) {


            CalcI calc = new CalcI(
                    "MCMASTER.TXT",  // dateipfad
                    sample,
                    "widerschwinger", // Röhrentyp (oder "lovescott")
                    "Rh",             // Röhrenmaterial
                    18.267463928051775, 71.733,           // Einfallswinkel alpha, beta
                    0,                // Fensterwinkel
                    1,                // sigma
                    0.1,                // charzucontL
                    "Be",             // Fenstermaterial Röhre
                    125,              // Fensterdicke Röhre (µm)
                    1,                // Raumwinkel
                    0.01,             // Röhrenstrom (A)
                    Emin, Emax,            // Emin, Emax
                    step,             // step
                    30,               // Messzeit
                    1.0771,                // charzucont
                    "Be",             // Fenstermaterial Detektor
                    7.62,             // Fensterdicke Detektor (µm)
                    0,                // phi Detektor
                    "Au",             // Kontaktmaterial
                    10.035,               // Kontaktmaterialdicke (nm)
                    1,                // Bedeckungsfaktor
                    45,               // palpha Grad
                    45,               // pbeta Grad
                    "Si",             // Detektormaterial
                    0.0,             // Totschicht (µm)
                    2.8623,                // activeLayer (mm)
                    filter,            // Filter-Liste
                    null
            );
            ;
            System.out.println(sample.getElemente().get(0).getSymbol());


            PreparedValues pv = calc.werteVorbereitenAlle();
            double[] origKonz = calc.konzentration;  // oder aus Probe holen
            double[] relKonz = calc.berechneRelKonzentrationen(calc, pv, 10000);

            double[] berechInt =  calc.berechneSummenintensitaetMitKonz(relKonz);
            double[] geoneu = calc.geometriefaktor(origKonz,berechInt);

            if (geoneu != null && geoneu.length > 0 && Double.isFinite(geoneu[0])) {
                geoNeuListe.add(geoneu[0]);

                for (int i = 0; i < geoneu.length; i++) {
                    System.out.println(geoneu[i]);
                }

            }




        }

        double[] geoNeuArray = geoNeuListe.stream().mapToDouble(Double::doubleValue).toArray();

// optional: ausgeben
        System.out.println("geoNeu[0] pro Probe: " +
                java.util.Arrays.toString(geoNeuArray));

        System.out.println(kal.mittlereAbweichung(geoNeuArray));




    }
}
