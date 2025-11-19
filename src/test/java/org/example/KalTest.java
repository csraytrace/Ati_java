package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KalTest {
    public static void main(String[] args) throws IOException {
        List<String[]> paraVar = Arrays.asList(
                new String[]{"sigma"},
                new String[]{"Einfallswinkelalpha"},
                new String[]{"activeLayer"},
                new String[]{"Totschicht"},
                new String[]{"charzucont_L"},
                new String[]{"charzucont"},
                //new String[]{"Emax"},
                new String[]{"Kontaktmaterialdicke"}

        );

        List<double[]> grenzen = Arrays.asList(
                new double[]{0.8, 1.0314},
                new double[]{15, 25},
                new double[]{2, 4},
                new double[]{0.0, 0.2},
                new double[]{0.1, 1.2},
                new double[]{0.8, 1.1},
                //new double[]{35, 45},
                new double[]{10, 40}
        );
        //int [] stepAnzahl = new int[]{3, 2,5,3,2};
        int[] stepAnzahl = new int[]{3, 3, 3, 3, 3, 3, 3};

// Deine Liste als Tripel (Intensität, Symbol, Übergang)
        List<Object[]> elementPaare = Arrays.asList(
                new Object[]{140000, "Ag", 0},
                new Object[]{66746, "SN", 0},
                new Object[]{163000, "TI", 0},
                new Object[]{111267, "CD", 0},
                new Object[]{433090, "CU", 0},
                new Object[]{196370, "V", 0},
                new Object[]{544606, "ZR", 0},
                new Object[]{464025, "ZN", 0},
                new Object[]{498315, "GE", 0},
                new Object[]{10844, "AL", 0},
                new Object[]{20692, "SI", 0},
                new Object[]{106785, "BI", 1},
                new Object[]{13396, "CD", 1},
                new Object[]{17196, "SN", 1},
                new Object[]{109041, "PB", 1},
                new Object[]{82738, "TA", 1},
                new Object[]{13240, "Ag", 1}
        );

        List<Probe> probeliste = new ArrayList<>();
        for (Object[] eintrag : elementPaare) {
            int intensity = (int) eintrag[0];
            String symbol = (String) eintrag[1];
            int uebergang = (int) eintrag[2];

            List<String> elementSymbole = Arrays.asList(symbol);
            List<Integer> elementInt = Arrays.asList(intensity);

            Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", 0, 40, 0.05, elementInt);
            // Übergang aktivieren:
            if (uebergang == 0) {
                probe.setzeUebergangAktivFuerElementKAlpha(0);
            } else if (uebergang == 1) {
                probe.setzeUebergangAktivFuerElementLAlpha(0);
            }
            probeliste.add(probe);
        }

        Kalibrierung kali = new Kalibrierung(null,null);


        kali.kalibrierung(
                paraVar,
                grenzen,
                stepAnzahl,
                probeliste,
                true,
                "kalibrierung_test_output.txt",
                ""
        );
    }
}