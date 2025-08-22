package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class filter {

    public static void main(String[] args) throws IOException {


        double Emin = 0;
        double Emax = 30;
        double step = 0.01;
        String dateipfad = "McMaster.txt";


        Funktionen f = new FunktionenImpl();


        Verbindung v1 = f.parseVerbindung("Cu1",Emin, Emax, step, "McMaster.txt");
        v1.setFensterDickeCm(0.01);
        List<Verbindung> fil = new ArrayList<>();
        fil.add(v1);



        double[] energieArray = v1.getEnergieArray();
        double[] filt = v1.erzeuge_Filter_liste();



        //System.out.println("Energie      Tau");
       // for (int i = 0; i < energieArray.length; i++) {
        //    System.out.printf("%8.3f  %12.6f\n", energieArray[i], filt[i]);
        //}
        System.out.println(v1.getDichte() + ", ");

        String eingabeName = "Air";
        String verbindungsString = MaterialNamenMapper.mapName(eingabeName);

        System.out.println(verbindungsString);

        Verbindung v = f.parseVerbindung(verbindungsString, Emin, Emax, step, dateipfad);
        System.out.println(v.getSymbole()[1]);
        v.setFensterDickeCm(1);



        // 1. Filter-Liste erzeugen (kann auch leer sein)
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

        // 2. Dummy-Probe bauen (hier mit 1 Element "Ag" – passe an!)
        //List<String> elementSymbole = Arrays.asList("Ag");
        List<String> elementSymbole = Arrays.asList("Si", "al");
        List<Integer> elementInt = Arrays.asList(2, 1);



        Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", Emin, Emax, step, elementInt);

        probe.setzeUebergangAktivFuerElementKAlpha(0);
        probe.setzeUebergangAktivFuerElementKAlpha(1);



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
                null,            // Filter-Liste
                filter
        );

        PreparedValues pv = calc.werteVorbereitenAlle();

        Übergang[][] ret = calc.primaerintensitaetBerechnen(pv);


        double[] origKonz = calc.konzentration;  // oder aus Probe holen
        double[] relKonz = calc.berechneRelKonzentrationen(calc, pv, 1000);

// Ausgabe als Tabelle
        System.out.println("Relative Konzentrationen in %:");
        for (int i = 0; i < relKonz.length; i++) {
            System.out.printf("Element %d: %.2f %%\n", i, relKonz[i]);
        }


        double[] berechInt = calc.berechneSummenintensitaetMitKonz(relKonz);
        double[] geo = calc.geometriefaktor(origKonz, berechInt);

        for (int i = 0; i < relKonz.length; i++) {
            System.out.println(berechInt[i]);
            ;
        }

    }
}