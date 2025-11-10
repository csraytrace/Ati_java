package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class test {
    public static void main(String[] args) {
        // Pfad zur Datei und Element wählen
        String dateipfad = "McMaster.txt";
        String element = "Fe";

        // Element erzeugen
        Element meinElement = new Element(dateipfad, element, 1, 1, 1);

        // Ergebnis holen
        Daten erg = meinElement.getErgebnis();
        /*

        // Alle Werte ausgeben
        System.out.println("Symbol: " + erg.getSymbol());
        System.out.println("Atomic Number: " + erg.getAtomicNumber());
        System.out.println("Atomic Weight: " + erg.getAtomicWeight());
        System.out.println("Density: " + erg.getDensity());
        System.out.println("cm2g: " + erg.getCm2g());
        System.out.println("Kanten: " + erg.getKanten());
        System.out.println("Übergänge: " + erg.getUbergange());
        System.out.println("Jumps: " + java.util.Arrays.toString(erg.getJumps()));
        System.out.println("McMaster: " + erg.getMcMaster());
        System.out.println("Omega: " + erg.getOmega());
        System.out.println("Costa: " + erg.getCosta());

        Funktionen f = new FunktionenImpl();

        Verbindung v = f.parseVerbindung("CaCO3 0.4324", 0.25, 0.25, 0.5, "McMaster.txt");


        List<List<Kante>> kantenListe = v.erzeugeKantenListe();

        for (int i = 0; i < kantenListe.size(); i++) {
            List<Kante> kanten = kantenListe.get(i);
            System.out.println("Element: " + v.getSymbole()[i]);
            for (Kante kante : kanten) {
                System.out.println(" - " + kante);

            }
        }



         */
        double Emin = 0;
        double Emax = 35;
        double step = 5;

        Funktionen f = new FunktionenImpl();

        Verbindung v1 = f.parseVerbindung("Cu1",Emin, Emax, step, "McMaster.txt");
        v1.setFensterDickeCm(0.001);
        List<Verbindung> fil = new ArrayList<>();
        fil.add(v1);

        WiderschwingerRöhre roehre = new WiderschwingerRöhre(
                /* Einfallswinkel alpha */    20,
                /* Einfallswinkel beta */     70,
                /* Fensterwinkel */           0,
                /* charzucont */              1,
                /* charzucont_L */            1,
                /* Fensterdicke (µm) */       125,
                /* Raumwinkel (sr) */         1,
                /* Röhrenstrom (A) */         0.01,
                /* Emin (keV) */              Emin,    // 0 → wird intern auf step gesetzt
                /* Emax (keV) */              Emax,
                /* sigmaConst */              1,
                /* step (keV) */              step,
                /* messZeit (s) */            30,
                /* roehrenMaterial */         "Rh",
                /* fensterMaterial */         "Be",
                /* folderPath */              "MCMASTER.TXT",
                /* Filter */                  fil// new ArrayList<>()


        );


        System.out.println("Energie      Tau");

        double[] energieArray = v1.getEnergieArray();
        double[] filt = v1.erzeuge_Filter_liste();

        //double[] energieArray = roehre.();
        //double[] filt = roehre.computeContinuousSpectrum();

        System.out.println("Energie      Tau");
        for (int i = 0; i < energieArray.length; i++) {
            System.out.printf("%8.3f  %12.6f\n", energieArray[i], filt[i]);
        }
        System.out.println(v1.getDichte()+", ");

        String eingabeName = "Air";
        String verbindungsString = MaterialNamenMapper.mapName(eingabeName);
// verbindungsString ist jetzt: "78 N 0.001225 + 20.94 O 0.001225 + 0.93 Ar 0.001225"
        System.out.println(verbindungsString);

        Verbindung v = f.parseVerbindung(verbindungsString, Emin, Emax, step, dateipfad);
        System.out.println(v.getSymbole()[1]);
        v.setFensterDickeCm(1);



        double[] filt1 =v.erzeuge_Filter_liste();
        System.out.println("Energie      Tau");
        for (int i = 0; i < energieArray.length; i++) {
            System.out.printf("%8.3f  %12.6f\n", energieArray[i], filt1[i]);
        }
        System.out.println(v1.getDichte()+", ");





        // 1. Filter-Liste erzeugen (kann auch leer sein)
        List<Verbindung> filter = new ArrayList<>();
        // Beispiel: Verbindung mit Dichte 0.1 und Symbol "Be"
        Verbindung beFilter = new Verbindung(
                new String[]{"Be"},
                new double[]{1.0},
                0, 40, 0.01,    // Emin, Emax, step
                "MCMASTER.TXT",
                0.1             // Dichte
        );
        //beFilter.setFensterDickeCm(0.4);
        filter.add(beFilter);

        // 2. Dummy-Probe bauen (hier mit 1 Element "Ag" – passe an!)
        //List<String> elementSymbole = Arrays.asList("Ag");
        //List<String> elementSymbole = Arrays.asList("Si","al");
        //List<Integer> elementInt = Arrays.asList(2,1);


        List<String> elementSymbole = Arrays.asList("Fe","Cu");
        List<Integer> elementInt = Arrays.asList(18000,23000);

        Emin = 0;
        Emax = 40;
        step = 0.01;

        Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", Emin, Emax, step,elementInt);

        //probe.setzeUebergangAktivFuerElement(0, "K", "L2");
        //probe.setzeUebergangAktivFuerElement(0, "l3", "m1");
        //probe.setzeUebergangAktivFuerElement(0, "K", "L3");
        //probe.setzeUebergangAktivFuerElement(0, "K", "L1");
        probe.setzeUebergangAktivFuerElementKAlpha(0);
        probe.setzeUebergangAktivFuerElementKAlpha(1);
        //probe.setzeUebergangAktivFuerElementLAlpha(1);



        // 4. Erzeuge CalcI-Instanz
        CalcI calc = new CalcI(
                "MCMASTER.TXT",  // dateipfad
                probe,
                "widerschwinger", // Röhrentyp (oder "lovescott")
                "Rh",             // Röhrenmaterial
                20, 70,           // Einfallswinkel alpha, beta
                0,                // Fensterwinkel
                0.830349567,                // sigma
                1,                // charzucontL
                "Be",             // Fenstermaterial Röhre
                125,              // Fensterdicke Röhre (µm)
                1,                // Raumwinkel
                0.01,             // Röhrenstrom (A)
                Emin, Emax,            // Emin, Emax
                step,             // step
                30,               // Messzeit
                0.946173852,                // charzucont
                "Be",             // Fenstermaterial Detektor
                7.62,             // Fensterdicke Detektor (µm)
                0,                // phi Detektor
                "Au",             // Kontaktmaterial
                29.988297,               // Kontaktmaterialdicke (nm)
                1,                // Bedeckungsfaktor
                45,               // palpha Grad
                45,               // pbeta Grad
                "Si",             // Detektormaterial
                0.00,             // Totschicht (µm)
                3,                // activeLayer (mm)
                filter,            // Filter-Liste
                filter
        );

        PreparedValues pv = calc.werteVorbereitenAlle();

        Übergang[][] ret = calc.primaerintensitaetBerechnen(pv);

        // Ausgabe mit Überschriften
        System.out.println("=== Primärintensitäten aller Übergänge ===");
        for (int i = 0; i < ret.length; i++) {
            System.out.println("Element " + i + ":");
            for (int j = 0; j < ret[i].length; j++) {
                Übergang u = ret[i][j];
                if (u == null || u.getEnergy() == 0.0) continue;  // Platzhalter überspringen
                System.out.printf(Locale.US,
                        "  %d.%d: %s → %s  |  Energie: %8.4f keV  |  Intensität: %10.4e|  Aktiv: %s\n",
                        i, j,
                        u.getSchale_von().name(),
                        u.getSchale_zu().name(),
                        u.getEnergy(),
                        u.getRate() ,
                        u.isAktiv() ? "Ja" : "Nein"// Angenommen, du speicherst hier die berechnete Intensität!
                );
            }
        }
        System.out.println("=== Ende ===");


        Übergang[][] sekundRet = calc.sekundaerintensitaetBerechnen(pv);

        System.out.println("=== Sekundärintensitäten aller Übergänge ===");
        for (int i = 0; i < sekundRet.length; i++) {
            System.out.println("Element " + i + ":");
            for (int j = 0; j < sekundRet[i].length; j++) {
                Übergang u = sekundRet[i][j];
                if (u == null || u.getEnergy() == 0.0) continue;  // Platzhalter überspringen
                System.out.printf(Locale.US,
                        "  %d.%d: %s → %s  |  Energie: %8.4f keV  |  Intensität: %10.4e|  Aktiv: %s\n",
                        i, j,
                        u.getSchale_von().name(),
                        u.getSchale_zu().name(),
                        u.getEnergy(),
                        u.getRate() , // Hier: berechnete Sekundärintensität!
                        u.isAktiv() ? "Ja" : "Nein"
                );
            }
        }
        System.out.println("=== Ende ===");

        double[] origKonz = calc.konzentration;  // oder aus Probe holen
        double[] relKonz = calc.berechneRelKonzentrationen(calc, pv, 10000);

// Ausgabe als Tabelle
        System.out.println("Relative Konzentrationen in %:");
        for (int i = 0; i < relKonz.length; i++) {
            System.out.printf("Element %d: %.2f %%\n", i, relKonz[i]);
        }



        double[] berechInt =  calc.berechneSummenintensitaetMitKonz(relKonz);
        double[] geo = calc.geometriefaktor(origKonz,berechInt);

        for (int i = 0; i < relKonz.length; i++) {
            System.out.println(berechInt[i]);;
        }





        double[] relKonzDünn = calc.berechneRelKonzentrationenDünnschicht(calc, pv, 10000);

// Ausgabe als Tabelle
        System.out.println("Relative Konzentrationen in %:");
        for (int i = 0; i < relKonzDünn.length; i++) {
            System.out.printf("Element_Dünnschicht %d: %.2f %%\n", i, relKonzDünn[i]);
        }







        List<String> elementSymboleDark = Arrays.asList("Si","al","O");
        List<Integer> elementIntDark = Arrays.asList(2,1,55);

        Emin = 0;
        Emax = 30;
        step = 0.01;

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



        double[] relKonzDark = calcDark.startwerte(1,darkMatrix);

        System.out.println("Relative Konzentrationen in %:");
        for (int i = 0; i < relKonzDark.length; i++) {
            System.out.printf("Element %d: %.2f %%\n", i, relKonzDark[i]);
        }







        //System.out.println(v1.erzeuge_Filter(20));



    }
    private static String deep2D(double[][] arr) {
        return Arrays.deepToString(arr);
    }

    private static String deep3D(double[][][] arr) {
        return Arrays.deepToString(arr);
    }

}