package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Filtertest {
    public static void main(String[] args) {


        double Emin = 0;
        double Emax = 35;
        double step = 0.01;

        Funktionen f = new FunktionenImpl();

        Verbindung v1 = f.parseVerbindung("Pd",Emin, Emax, step, "McMaster.txt");
        v1.setFensterDickeCm(0.005);
        List<Verbindung> fil = new ArrayList<>();
        fil.add(v1);



        String eingabeName = "Air";
        String verbindungsString = MaterialNamenMapper.mapName(eingabeName);
// verbindungsString ist jetzt: "78 N 0.001225 + 20.94 O 0.001225 + 0.93 Ar 0.001225"
        System.out.println(verbindungsString);



        List<String> elementSymbole = Arrays.asList("Pb");
        List<Integer> elementInt = Arrays.asList(1);

        Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", Emin, Emax, step,elementInt);


        //probe.setzeUebergangAktivFuerElementKAlpha(0);
        probe.setzeUebergangAktivFuerElementLAlpha(0);

        //probe.setzeUebergangAktivFuerElementLAlpha(1);
        //probe.setzeUebergangAktivFuerElementKAlpha(1);


/*
        // 4. Erzeuge CalcI-Instanz
        CalcI calc = new CalcI(
                "MCMASTER.TXT",  // dateipfad
                probe,
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
                fil,            // Filter-Liste
                null
        );

*/

        CalcI calc = new CalcI(
                "MCMASTER.TXT",  // dateipfad
                probe,
                "widerschwinger", // Röhrentyp (oder "lovescott")
                "Rh",             // Röhrenmaterial
                20.325, 69.675,           // Einfallswinkel alpha, beta
                0,                // Fensterwinkel
                0.8,                // sigma
                0.1,                // charzucontL
                "Be",             // Fenstermaterial Röhre
                125,              // Fensterdicke Röhre (µm)
                1,                // Raumwinkel
                0.01,             // Röhrenstrom (A)
                Emin, Emax,            // Emin, Emax
                step,             // step
                30,               // Messzeit
                0.816,                // charzucont
                "Be",             // Fenstermaterial Detektor
                7.62,             // Fensterdicke Detektor (µm)
                0,                // phi Detektor
                "Au",             // Kontaktmaterial
                24.838,               // Kontaktmaterialdicke (nm)
                1,                // Bedeckungsfaktor
                45,               // palpha Grad
                45,               // pbeta Grad
                "Si",             // Detektormaterial
                0.0,             // Totschicht (µm)
                2.513,                // activeLayer (mm)
                null,            // Filter-Liste
                null
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
        System.out.println("intensiät: "+calc.berechneSummenintensitaetMitKonz(relKonz)[0]);

        double[] geo = calc.geometriefaktor(origKonz,berechInt);

        for (int i = 0; i < relKonz.length; i++) {
            System.out.println(berechInt[i]);;
        }




        CalcI calcFil = new CalcI(
                "MCMASTER.TXT",  // dateipfad
                probe,
                "widerschwinger", // Röhrentyp (oder "lovescott")
                "Rh",             // Röhrenmaterial
                20.325, 69.675,           // Einfallswinkel alpha, beta
                0,                // Fensterwinkel
                0.8,                // sigma
                0.1,                // charzucontL
                "Be",             // Fenstermaterial Röhre
                125,              // Fensterdicke Röhre (µm)
                1,                // Raumwinkel
                0.01,             // Röhrenstrom (A)
                Emin, Emax,            // Emin, Emax
                step,             // step
                30,               // Messzeit
                0.816,                // charzucont
                "Be",             // Fenstermaterial Detektor
                7.62,             // Fensterdicke Detektor (µm)
                0,                // phi Detektor
                "Au",             // Kontaktmaterial
                24.838,               // Kontaktmaterialdicke (nm)
                1,                // Bedeckungsfaktor
                45,               // palpha Grad
                45,               // pbeta Grad
                "Si",             // Detektormaterial
                0.0,             // Totschicht (µm)
                2.513,                // activeLayer (mm)
                fil,            // Filter-Liste
                null
        );


        double[] berechIntFil =  calc.berechneSummenintensitaetMitKonz(relKonz);
        System.out.println("Dif: "+calc.berechneSummenintensitaetMitKonz(relKonz)[0]/calcFil.berechneSummenintensitaetMitKonz(relKonz)[0]/3);




    }


}