package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WiderschwingerRöhre extends RöhreBasis {

    public WiderschwingerRöhre(
            double Einfallswinkelalpha,
            double Einfallswinkelbeta,
            double Fensterwinkel,
            double charzucont,
            double charzucontL,
            double fensterDickeUmikron,
            double raumWinkel,
            double roehrenStrom,
            double Emin,
            double Emax,
            double sigmaConst,
            double step,
            double messZeit,
            String roehrenMaterial,
            String fensterMaterial,
            String folderPath,
            List<Verbindung> Filter
    ) {
        super(
                roehrenMaterial,
                Einfallswinkelalpha,
                Einfallswinkelbeta,
                Fensterwinkel,
                charzucont,
                charzucontL,
                fensterMaterial,
                fensterDickeUmikron,
                raumWinkel,
                roehrenStrom,
                Emin,
                Emax,
                sigmaConst,
                step,
                messZeit,
                folderPath,
                Filter
        );
        prepareData();
    }

    @Override
    protected double getSigma(double E) {
        double Z = roehrenMaterial.Z_gemittelt();
        double U = Emax / E;
        double exp = 1.0314 - 0.0032 * Z + 0.0047 * Emax;
        return 1.36e9 * Z * Math.pow(U - 1, exp);
    }


    protected  List<Double> RzuS_j(){
        double[][] coefficients = {
                {5.580848699E-3, 2.709177328E-4, -5.531081141E-6, 5.95579625E-8, -3.210316856E-10},
                {3.401533559E-2, -1.601761397E-4, 2.473523226E-6, -3.020861042E-8, 0.0},
                {9.916651666E-2, -4.615018255E-4, -4.332933627E-7, 0.0, 0.0},
                {1.030099792E-1, -3.113053618E-4, 0.0, 0.0, 0.0},
                {3.630169747E-2, 0.0, 0.0, 0.0, 0.0}
        };
        List<Double> listeappends = new ArrayList<>();
        List<List<Double>> energieListe =  getEnergienVonKanten();
        List<Double> Z_liste = roehrenMaterial.getZ_List();

        for (int i = 0; i < energieListe.size(); i++) {
            List<Double> energieProElement = energieListe.get(i);
            for (int j = 0; j < energieProElement.size(); j++) {
                double S_energie = energieProElement.get(j);
                double U = Emax / S_energie;
                double J = 0.0135 * Z_liste.get(i);
                double lnU = Math.log(U);
                // Kanten sind immer K1, L1, L2, L3
                double z, b;
                if (j == 0) {
                    z = 2;
                    b = 0.35;
                } else {
                    z = 8;
                    b = 0.25;
                }
                double klammer = 1 + 16.05 * Math.sqrt(J / S_energie) *
                        (Math.sqrt(U) * lnU + 2 * (1 - Math.sqrt(U))) / (U * lnU + 1 - U);

                double intensitaetsfaktor = z * b / Z_liste.get(i) * (U * lnU + 1 - U) * klammer;

                double R = 1;
                for (int y = 0; y < 5; y++) {
                    for (int x = 0; x <= y; x++) {
                        R += coefficients[x][y - x] * Math.pow(1 / U - 1, x + 1) * Math.pow(Z_liste.get(i), y - x + 1);
                    }
                }
                double listappend = 0;

                if (S_energie > 0 && Emax > S_energie) {
                    listappend = intensitaetsfaktor * R;
                }

                listeappends.add(listappend);
            }
        }
        return listeappends;}


    public static void main(String[] args) {
        // Erzeuge eine Widerschwinger-Röhre mit denselben Parametern wie dein Python-Default


        double step = 5;
        WiderschwingerRöhre roehre = new WiderschwingerRöhre(
                /* Einfallswinkel alpha */    20,
                /* Einfallswinkel beta */     70,
                /* Fensterwinkel */           0,
                /* charzucont */              1,
                /* charzucont_L */            1,
                /* Fensterdicke (µm) */       125,
                /* Raumwinkel (sr) */         1,
                /* Röhrenstrom (A) */         0.01,
                /* Emin (keV) */              0,    // 0 → wird intern auf step gesetzt
                /* Emax (keV) */              35,
                /* sigmaConst */              1,
                /* step (keV) */              step,
                /* messZeit (s) */            30,
                /* roehrenMaterial */         "Rh",
                /* fensterMaterial */         "Be",
                /* folderPath */              "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Atiquant\\BGQXRFPN\\BGQXRFPN\\MCMASTER.TXT",
                /* Filter */                   new ArrayList<>()


        );



        LoveScottRöhre roehre_scott = new LoveScottRöhre(
                /* Einfallswinkel alpha */    20,
                /* Einfallswinkel beta */     70,
                /* Fensterwinkel */           0,
                /* charzucont */              1,
                /* charzucont_L */            1,
                /* Fensterdicke (µm) */       125,
                /* Raumwinkel (sr) */         1,
                /* Röhrenstrom (A) */         0.01,
                /* Emin (keV) */              0,    // 0 → wird intern auf step gesetzt
                /* Emax (keV) */              35,
                /* sigmaConst */              1,
                /* step (keV) */              step,
                /* messZeit (s) */            30,
                /* roehrenMaterial */         "Rh",
                /* fensterMaterial */         "Be",
                /* folderPath */              "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Atiquant\\BGQXRFPN\\BGQXRFPN\\MCMASTER.TXT",
                /* Filter */                   new ArrayList<>()


        );

        // kontinuierliches Spektrum berechnen
        //double[] spectrum = roehre.computeContinuousSpectrum();
        //double[] energies = roehre.roehrenMaterial.getEnergieArray();

        // gib die ersten 10 Werte aus
        System.out.println("Energie (keV) | Rate");
        //for (int i = 0; i < Math.min(100, spectrum.length); i++) {
           // System.out.printf("%8.3f      | %12.6e%n", energies[i], spectrum[i]);
       // }

        roehre.getEnergienVonKanten();
        List<List<Double>> energien = roehre.getEnergienVonKanten();
        String[] symbole = roehre.roehrenMaterial.getSymbole();
        String[] schalen = {"K", "L1", "L2", "L3"};
        roehre.RzuS_j();
        System.out.println(roehre.RzuS_j());
        System.out.println(roehre.roehrenMaterial.erzeugeÜbergängeListe());
        System.out.println(roehre.roehrenMaterial.erzeugeOmegaListe());

        List<List<String[]>> omegaListe = roehre.roehrenMaterial.erzeugeOmegaListe();

        for (int i = 0; i < omegaListe.size(); i++) {
            System.out.println("Element " + i + ":");
            List<String[]> listeProElement = omegaListe.get(i);
            for (int j = 0; j < listeProElement.size(); j++) {
                String[] arr = listeProElement.get(j);
                // Gib das Array als String aus:
                System.out.println("  " + Arrays.toString(arr));
            }
        }
        System.out.println(roehre.computeCharacteristicSpectrum());
        System.out.println("char");
        System.out.println(roehre.getCountRateChar(19.8));
        System.out.println(roehre.getGesamtspektrum());


        double[] spectrum = roehre.getGesamtspektrum();
        double[] energies = roehre.roehrenMaterial.getEnergieArray();
        double[] spectrum_scott = roehre_scott.getGesamtspektrum();

        // gib die ersten 10 Werte aus
        System.out.println("Energie (keV) | Rate");
        for (int i = 0; i < Math.min(50000, spectrum.length); i++) {
         System.out.printf("%8.3f      | %12.6e%n", energies[i], spectrum[i]);
         }

        System.out.println("Energie (keV) | Rate");
        for (int i = 0; i < Math.min(50000, spectrum.length); i++) {
            System.out.printf("%8.3f      | %12.6e%n", energies[i], spectrum_scott[i]);
        }

        //for (int i = 0; i < energien.size(); i++) {
           /// System.out.println("Element: " + symbole[i]);
           // List<Double> energieProElement = energien.get(i);
           // for (int j = 0; j < energieProElement.size(); j++) {
            //    System.out.println(" - " + schalen[j] + ": " + energieProElement.get(j));
          //  }
       // }
    }

}
