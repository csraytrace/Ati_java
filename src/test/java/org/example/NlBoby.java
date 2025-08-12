package org.example;

import java.util.Arrays;
import java.util.List;

import static org.example.XrfFixture.*;

public class NlBoby {
    public static void main(String[] args) {

        CalcIDark calcDark = newCalcIDark();
        double[] darkMatrix = DARK_MATRIX;
        double Z = Z_MITTEL;


        double[] optimum = calcDark.optimizeWithBOBYQAEinfach(Z, darkMatrix);
        System.out.println("optimum optimum: " + Arrays.toString(optimum));
        calcDark.printOptimizedResultEinfach(optimum, darkMatrix, Z);
    }
}
