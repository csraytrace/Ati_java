package org.example;

import java.util.Arrays;

import static org.example.XrfFixture.*;

public class NlHipparchus {


    public static void main(String[] args) {


        CalcIDark calcDark = newCalcIDark();
        double[] darkMatrix = DARK_MATRIX;
        double Z = Z_MITTEL;

        double[] optimum = calcDark.optimizeHIPPARCHUS(Z, darkMatrix);
        System.out.println("optimum optimum: " + Arrays.toString(optimum));
        calcDark.printOptimizedResult(optimum, darkMatrix, Z);




    }
}
