package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestBobyqa {
    public static void main(String[] args) {

        double Emin=0.05;
        double Emax=40;
        double step=0.05;
        List<String[]> paraVar = Arrays.asList(
                new String[]{"sigma"},
                new String[]{"Einfallswinkelalpha"},
                new String[]{"activeLayer"},
                new String[]{"Totschicht"},
                new String[]{"charzucontL"},
                new String[]{"charzucont"},
                new String[]{"Kontaktmaterialdicke"}

        );

        List<double[]> grenzen = Arrays.asList(
                new double[]{0.8, 1.0314},
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


        //double[] startwerte = {15, 3, 0, 1.2, 0.95, 10};
        double[] startwerte = {0.8, 15, 2, 0.1, 0.1, 1.1, 25};
        String para="Emin=0.05, Emax=40,step=0.05";
        //"sigma=0.8, raumwinkel=0.9, Einfallswinkelalpha=8, Einfallswinkelbeta=70"




        Kali1 kali = new Kali1(null,null);


        // NLLS-Aufruf mit BOBYQA:
        double[] optimierteParameter = kali.kalibrierungNLLS_bobyqa(
                paraVar,
                grenzen,
                gemesseneIntensitaet,
                probeliste,
                true,
                para,
                null // Startwerte
        );

        // Ausgabe
        System.out.println("Optimierte Parameter (BOBYQA):");
        for (int i = 0; i < optimierteParameter.length; i++) {
            System.out.println(paraVar.get(i)[0] + " = " + optimierteParameter[i]);
        }



        double[] geo = Kali1.berechneGeo(
                optimierteParameter,       // params
                paraVar,                   // para_var (List<String[]>)
                probeliste,                // proben (List<Probe>)
                true,                      // bedingung
                para // para
        );

        System.out.println("Optimierte Parameter (Geo):");
        for (int i = 0; i < geo.length; i++) {
            System.out.println(geo[i]);
        }
        System.out.println(Kali1.mittlereAbweichung(geo));



    }
}
