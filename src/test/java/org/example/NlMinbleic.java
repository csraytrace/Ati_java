package org.example;

import static org.example.XrfFixture.*;

public class NlMinbleic {




    public static void main(String[] args) {


        CalcIDark calcDark = newCalcIDark();
        double[] darkMatrix = DARK_MATRIX;
        double Z = Z_MITTEL;


        try {
            System.out.println("\n==== ALGLIB mindf: Testlauf ====");
            double[] optimumAlglib = calcDark.optimizeWithALGLIB_MINBLEIC_Einfach(Z, darkMatrix);
            //System.out.println("Optimale Parameter (ALGLIB): " + Arrays.toString(optimumAlglib));

            // Ergebnis ausgeben (Z-mittel, Intensitäten etc.)
            calcDark.printOptimizedResultEinfach(optimumAlglib, darkMatrix, Z);


        } catch (Throwable t) {
            // Falls ALGLIB nicht auf dem Classpath ist oder mindf nicht verfügbar: freundlich degradieren
            System.err.println("ALGLIB-Test übersprungen: " + t.getMessage());
            // Optional: t.printStackTrace();
        }
    }
}
