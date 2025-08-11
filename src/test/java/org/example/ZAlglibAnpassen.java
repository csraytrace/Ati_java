package org.example;


import java.util.Arrays;
import java.util.List;
public class ZAlglibAnpassen {

        public static void main(String[] args) {


            // Beispielwerte
            double[] konzLow  = {0.7314573261495299}; // Konzentrationen für niedrige Z
            int[]    zLow     = { 8};     // z.B. C, O
            double[] konzHigh = {0.3169405474094008, 0.3674085315493202}; // Konzentrationen für hohe Z
            int[]    zHigh    = {14, 29};   // z.B. Si, Fe
            double   zGewuenscht = 17;      // Zielwert für gewichtetes Z

            double[] result = CalcIDark.ZAnpassen(konzLow, zLow, konzHigh, zHigh, zGewuenscht);

            // Skaliere die Konzentrationen
            double[] newKonzLow  = new double[konzLow.length];
            double[] newKonzHigh = new double[konzHigh.length];
            for (int i = 0; i < konzLow.length; i++)  newKonzLow[i]  = konzLow[i]  * result[0];
            for (int i = 0; i < konzHigh.length; i++) newKonzHigh[i] = konzHigh[i] * result[1];

            // Kombiniere und normiere die neuen Konzentrationen
            double[] allKonz = new double[newKonzLow.length + newKonzHigh.length];
            System.arraycopy(newKonzLow, 0, allKonz, 0, newKonzLow.length);
            System.arraycopy(newKonzHigh, 0, allKonz, newKonzLow.length, newKonzHigh.length);

            int[] allZ = new int[zLow.length + zHigh.length];
            System.arraycopy(zLow, 0, allZ, 0, zLow.length);
            System.arraycopy(zHigh, 0, allZ, zLow.length, zHigh.length);

            double sumKonz = 0.0;
            for (double d : allKonz) sumKonz += d;
            for (int i = 0; i < allKonz.length; i++) allKonz[i] /= sumKonz;

            // Berechne das gewichtete Z
            double zm = 0.0;
            for (int i = 0; i < allKonz.length; i++) zm += allKonz[i] * allZ[i];

            // Output
            System.out.println("Anpassungsfaktoren:");
            System.out.printf("Low:  %.4f\n", result[0]);
            System.out.printf("High: %.4f\n", result[1]);

            System.out.println("Neue normierte Konzentrationen:");
            for (int i = 0; i < allKonz.length; i++) {
                System.out.printf("  Z=%2d: %.6f\n", allZ[i], allKonz[i]);
            }

            System.out.printf("Berechnetes Z_m: %.4f\n", zm);

        }
    }

