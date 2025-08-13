package org.example;

import alglib.*;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Locale;

import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;



import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.SimpleVectorValueChecker;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresBuilder;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.util.Pair;
import org.hipparchus.optim.nonlinear.vector.leastsquares.*;



import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;

public class CalcIDark {

    private CalcI calcDark;
    private CalcI calcFiltered;
    private PreparedValues Dark;
    private PreparedValues Filtered;
    private final int darkI = 12;
    private int indexMaxKonz = 1;
    private double maxValKonz = 1;

    public CalcIDark(
            String dateipfad,
            Probe probe,
            String röhrenTyp,
            String röhrenmaterial,
            double einfallswinkelalpha,
            double einfallswinkelbeta,
            double fensterwinkel,
            double sigma,
            double charzucontL,
            String fenstermaterialRöhre,
            double fensterdickeRöhre,
            double raumwinkel,
            double röhrenstrom,
            double emin,
            double emax,
            double step,
            double messzeit,
            double charzucont,
            String fenstermaterialDet,
            double fensterdickeDet,
            double phiDet,
            String kontaktmaterial,
            double kontaktmaterialdicke,
            double bedeckungsfaktor,
            double palphaGrad,
            double pbetaGrad,
            String detektormaterial,
            double totschicht,
            double activeLayer,
            List<Verbindung> filter_röhre,
            List<Verbindung> filter_det

    ) {
        this.calcDark = new CalcI(dateipfad, probe, röhrenTyp, röhrenmaterial,
                einfallswinkelalpha, einfallswinkelbeta, fensterwinkel, sigma,
                charzucontL, fenstermaterialRöhre, fensterdickeRöhre, raumwinkel, röhrenstrom,
                emin, emax, step, messzeit, charzucont, fenstermaterialDet, fensterdickeDet,
                phiDet, kontaktmaterial, kontaktmaterialdicke, bedeckungsfaktor, palphaGrad,
                pbetaGrad, detektormaterial, totschicht, activeLayer, filter_röhre, filter_det);

        Probe probeFiltered = probe.filterByMinZ(darkI);
        this.calcFiltered = new CalcI(dateipfad, probeFiltered, röhrenTyp, röhrenmaterial,
                einfallswinkelalpha, einfallswinkelbeta, fensterwinkel, sigma,
                charzucontL, fenstermaterialRöhre, fensterdickeRöhre, raumwinkel, röhrenstrom,
                emin, emax, step, messzeit, charzucont, fenstermaterialDet, fensterdickeDet,
                phiDet, kontaktmaterial, kontaktmaterialdicke, bedeckungsfaktor, palphaGrad,
                pbetaGrad, detektormaterial, totschicht, activeLayer, filter_röhre, filter_det);


        Dark = calcDark.werteVorbereitenAlle();
        Filtered = calcFiltered.werteVorbereitenAlle();
    }




    private double[] lowKonBe(double lowKon, double[] verteilung) {
        double[] norVert = normiereDaten(verteilung); // eigene Hilfsmethode
        double[] result = new double[norVert.length];
        for (int i = 0; i < norVert.length; i++) {
            result[i] = lowKon * norVert[i];
        }
        return result;
    }

    private double[] normiereDaten(double[] daten) {
        double sum = 0.0;
        for (double d : daten) sum += d;
        double[] out = new double[daten.length];
        for (int i = 0; i < daten.length; i++) out[i] = daten[i] / sum;
        return out;
    }


    public static double[] ZAnpassen(
            double[] konzLow, int[] zLow, double[] konzHigh, int[] zHigh, double zGewuenscht) {

        double konzSumLow = 0.0;
        for (double k : konzLow) konzSumLow += k;
        double konzSumHigh = 0.0;
        for (double k : konzHigh) konzSumHigh += k;

        double zMittelLow = 0.0;
        for (int i = 0; i < konzLow.length; i++) zMittelLow += konzLow[i] * zLow[i];
        zMittelLow /= konzSumLow;

        double zMittelHigh = 0.0;
        for (int i = 0; i < konzHigh.length; i++) zMittelHigh += konzHigh[i] * zHigh[i];
        zMittelHigh /= konzSumHigh;

        //double konzGesamt = konzSumLow + konzSumHigh;
        //double konzSumLowNorm = konzSumLow / konzGesamt;
        //double konzSumHighNorm = konzSumHigh / konzGesamt;

        double konzNeuLow = ((zGewuenscht - zMittelHigh) / (zMittelLow - zMittelHigh));
        double konzNeuHigh = 1 - konzNeuLow;

        if (zGewuenscht < zMittelLow || zGewuenscht > zMittelHigh) {
            //System.out.println(zMittelLow + " " + zMittelHigh + " " + zGewuenscht + " Wert muss dazwischen liegen!");
            System.err.println("WARNUNG: (Z = "+ zGewuenscht+") nicht in den Grenzen: " + zMittelLow + "<" + "Z" + "<" +  zMittelHigh);
            // throw new IllegalArgumentException("Z_gemittel kann nicht erreicht werden!");
            return new double[]{
                    Math.abs(konzNeuLow / konzSumLow),
                    Math.abs(konzNeuHigh / konzSumHigh)
            };
        }

        return new double[]{konzNeuLow / konzSumLow, konzNeuHigh / konzSumHigh};
        //return new double[]{konzNeuLow / konzSumLowNorm, konzNeuHigh / konzSumHighNorm};  #falls die originalen Konzentrationenn nicht normiert werden sollen
    }



    public double[] startwerte(double Z_mittelwert, double [] low_verteilung) {

        double [] gemInt = calcFiltered.konzentration;
        double[] relKonz = CalcI.berechneRelKonzentrationen(calcFiltered, Filtered);
        double[] geo = CalcI.geometriefaktor(gemInt,calcFiltered.berechneSummenintensitaetMitKonz(relKonz));
        double sum = 0;
        for (double value : geo) {
            sum += value;
        }
        double average_geo = sum / geo.length;
        double [] d = calcFiltered.berechneSummenintensitaetMitKonz(relKonz,average_geo);
        //System.out.println(d[0]+"  "+d[1]);


        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        double[] Startkonzentration = new double[z_split.valuesHigh.length + 1];
        for (int i = 0; i < relKonz.length; i++) {
            Startkonzentration[i + 1] = relKonz[i];
        }

        double[] x_y = ZAnpassen( low_verteilung, z_split.valuesLow, relKonz, z_split.valuesHigh, Z_mittelwert);

        double x = x_y[0];
        double y = x_y[1];

        for (int i = 1; i < Startkonzentration.length; i++) {
            Startkonzentration[i] *= y;
        }
        Startkonzentration[0] = x;

        double [] konz_low_start = lowKonBe(Startkonzentration[0],low_verteilung);
        double [] konz_high_start = lowKonBe(1 - Startkonzentration[0],relKonz);

        int gesamtLen = z_split.indicesLow.length + z_split.indicesHigh.length;
        double[] gesamtKonz = new double[gesamtLen];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low_start[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high_start[i];
        }

        //double [] berechneteIntensitaetBasis = calcFiltered.berechneSummenintensitaetMitKonz(relKonz,average_geo);
        double [] berechneteIntensitaetBasis = calcFiltered.berechneSummenintensitaetMitKonz(konz_high_start,average_geo);
        double [] berechneteIntensitaetDark = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz,average_geo);

        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            int idx = z_split.indicesHigh[i];
            Startkonzentration[1 + i] *= berechneteIntensitaetBasis[i] / berechneteIntensitaetDark[idx];
        }
        double[] startKonzAb = Arrays.copyOfRange(Startkonzentration, 1, Startkonzentration.length);

        x_y = ZAnpassen( low_verteilung, z_split.valuesLow, startKonzAb, z_split.valuesHigh, Z_mittelwert);
        x = x_y[0];
        y = x_y[1];
        for (int i = 1; i < Startkonzentration.length; i++) {
            Startkonzentration[i] *= y;
        }
        Startkonzentration[0] = x;
        int indexMax = -1;
        double maxVal = Double.NEGATIVE_INFINITY;

        for (int i = 1; i < Startkonzentration.length; i++) { // bei 1 starten, um Index 0 zu ignorieren
            if (Startkonzentration[i] > maxVal) {
                maxVal = Startkonzentration[i];
                indexMax = i;
            }
        }
        indexMaxKonz = indexMax;
        maxValKonz = maxVal;


        return Startkonzentration;

    }

    public double [] berechnenResiduum(double [] params, double [] low_verteilung, double Z_mittelwert)
    {

        double [] konzentration = params.clone();
        //konzentration=normiereDaten(konzentration);

        //for (int i = 0; i < konzentration.length; i++) {
        //    System.out.printf("Res %d: %.2f %%\n", i, konzentration[i]);
        //}


        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        //double[] high_verteilung = Arrays.copyOfRange(params, 1, params.length);

        double [] konz_low_start = lowKonBe(konzentration[0],low_verteilung);
        double[] konz_high_start = Arrays.copyOfRange(params, 1, params.length);
        //double [] konz_high_start = lowKonBe(1 - konzentration[0],high_verteilung);

        int gesamtLen = z_split.indicesLow.length + z_split.indicesHigh.length;

        //System.out.println(konzentration[0]);

        //System.out.printf("konzentration[0]: %.2f %%\n", konzentration[0]);
        //System.out.printf("konzentration[0]: %.2f %%\n", 1-konzentration[0]);

        //for (int i = 0; i < konz_low_start.length; i++) {
         //   System.out.printf("konz_low_start %d: %.2f %%\n", i, konz_low_start[i]);
        //}

        //for (int i = 0; i < konz_high_start.length; i++) {
         //   System.out.printf("konz_high_start %d: %.2f %%\n", i, konz_high_start[i]);
        //}



        //Effekt von Binder auf konz_low_start

        double[] x_y = ZAnpassen(konz_low_start, z_split.valuesLow, konz_high_start, z_split.valuesHigh, Z_mittelwert);
        //System.out.println(x_y[0]+"  "+x_y[1]);

        for (int i = 0; i < konz_low_start.length; i++) {
            konz_low_start[i] *= x_y[0];
        }
        //for (int i = 0; i < konz_low_start.length; i++) {
         //  System.out.printf("konz_low_start %d: %.2f %%\n", i, konz_low_start[i]);
        //}

        for (int i = 0; i < konz_high_start.length; i++) {
            konz_high_start[i] *= x_y[1];
        }

        //for (int i = 0; i < konz_high_start.length; i++) {
        //   System.out.printf("konz_high_start %d: %.2f %%\n", i, konz_high_start[i]);
        //}



        double[] gesamtKonz = new double[gesamtLen];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low_start[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high_start[i];
        }


        //for (int i = 0; i < gesamtKonz.length; i++) {
        //    System.out.printf("gesamtKonz %d: %.2f %%\n", i, gesamtKonz[i]);
        //}


        double [] berechneteIntensitaetDark = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz);

        double[] IbHigh = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            IbHigh[i] = berechneteIntensitaetDark[z_split.indicesHigh[i]];
        }

        double geo = geoIbIg(IbHigh);
        double [] berechneteIntensitaetDark1 = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz,geo);
        double [] gemInt=calcFiltered.konzentration;

        double[] diff = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            int idx = z_split.indicesHigh[i];
            //System.out.println("idx"+idx+"gemint"+gemInt[i]);
            diff[i] = berechneteIntensitaetDark1[idx] - gemInt[i];

            //System.out.printf("Index %d: gemessen = %.6f, berechnet = %.6f, Differenz = %.6f%n",
             //       idx, gemInt[i], berechneteIntensitaetDark1[idx], diff[i]);
        }
        return diff;
    }




    public double [] berechnenResiduumEinfach(double [] params, double [] low_verteilung, double Z_mittelwert)
    {

        double [] konzentration = params.clone();


        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);


        double [] konz_low_start = lowKonBe(konzentration[0],low_verteilung);
        double[] konz_high_start = Arrays.copyOfRange(params, 1, params.length);
        konz_high_start = insertAt(konz_high_start, indexMaxKonz - 1, maxValKonz);


        int gesamtLen = z_split.indicesLow.length + z_split.indicesHigh.length;


        double[] x_y = ZAnpassen(konz_low_start, z_split.valuesLow, konz_high_start, z_split.valuesHigh, Z_mittelwert);


        for (int i = 0; i < konz_low_start.length; i++) {
            konz_low_start[i] *= x_y[0];
        }


        for (int i = 0; i < konz_high_start.length; i++) {
            konz_high_start[i] *= x_y[1];
        }


        double[] gesamtKonz = new double[gesamtLen];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low_start[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high_start[i];
        }

        //for (int i = 0; i < gesamtKonz.length; i++) {
        //    System.out.printf("gesamtKonz %d: %.2f %%\n", i, gesamtKonz[i]);
        //}
        //System.out.println("maxValKonz"+maxValKonz);


        double [] berechneteIntensitaetDark = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz);

        double[] IbHigh = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            IbHigh[i] = berechneteIntensitaetDark[z_split.indicesHigh[i]];
        }

        double geo = geoIbIg(IbHigh);
        double [] berechneteIntensitaetDark1 = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz,geo);
        double [] gemInt=calcFiltered.konzentration;

        double[] diff = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            int idx = z_split.indicesHigh[i];
            //System.out.println("idx"+idx+"gemint"+gemInt[i]);
            diff[i] = berechneteIntensitaetDark1[idx] - gemInt[i];

            //System.out.printf("Index %d: gemessen = %.6f, berechnet = %.6f, Differenz = %.6f%n",
             //      idx, gemInt[i], berechneteIntensitaetDark1[idx], diff[i]);
        }
        return diff;
    }


    public static double[] insertAt(double[] array, int index, double value) {
        if (index < 0 || index > array.length) {
            throw new IllegalArgumentException("Index außerhalb des Bereichs: " + index);
        }
        double[] result = new double[array.length + 1];
        for (int i = 0, j = 0; i < result.length; i++) {
            if (i == index) {
                result[i] = value;
            } else {
                result[i] = array[j++];
            }
        }
        return result;
    }

    public double [] testberechnenResiduumEinfach(double [] params, double [] low_verteilung, double Z_mittelwert, int index, double konz)
    {
        maxValKonz=konz;
        indexMaxKonz=index;

        return berechnenResiduumEinfach(params, low_verteilung, Z_mittelwert);
    }








    // Mini-Klasse als Rückgabecontainer:
    public static class SplitResult {
        public final int[] indicesLow;
        public final int[] valuesLow;
        public final int[] indicesHigh;
        public final int[] valuesHigh;

        public SplitResult(int[] indicesLow, int[] valuesLow, int[] indicesHigh, int[] valuesHigh) {
            this.indicesLow = indicesLow;
            this.valuesLow = valuesLow;
            this.indicesHigh = indicesHigh;
            this.valuesHigh = valuesHigh;
        }
    }

    // Die Utility-Funktion:
    public static SplitResult splitByZ(int[] arr, int Z) {
        int countLow = 0, countHigh = 0;
        for (int z : arr) {
            if (z < Z) countLow++;
            else countHigh++;
        }
        int[] indicesLow = new int[countLow];
        int[] valuesLow = new int[countLow];
        int[] indicesHigh = new int[countHigh];
        int[] valuesHigh = new int[countHigh];
        int li = 0, hi = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < Z) {
                indicesLow[li] = i;
                valuesLow[li++] = arr[i];
            } else {
                indicesHigh[hi] = i;
                valuesHigh[hi++] = arr[i];
            }
        }
        return new SplitResult(indicesLow, valuesLow, indicesHigh, valuesHigh);
    }



    public double geoIbIg(double[] Ib) {
        double[] gemInt = calcFiltered.konzentration; // oder this.konzentration, je nach Kontext
        double sum = 0.0;
        for (int i = 0; i < gemInt.length; i++) {
            sum += gemInt[i] / Ib[i];
        }
        return sum / gemInt.length;
    }

    public static double[] zeros(int len) {
        double[] arr = new double[len];
        Arrays.fill(arr, 0.0);
        return arr;
    }

    public static double[] ones(int len) {
        double[] arr = new double[len];
        Arrays.fill(arr, 1.0);
        return arr;
    }


    public double[] optimizeWithBOBYQA(
            double zMittelwert,
            double[] lowVerteilung

    ) {
        double [] start = startwerte(zMittelwert,lowVerteilung);
        int dim = start.length;
        int numberOfInterpolationPoints = 2 * dim + 1; // Empfehlung: 2*N+1

        // Bounds automatisch: 0.0 bis 1.0
        double[] lower = new double[dim];
        double[] upper = new double[dim];
        Arrays.fill(lower, 0.0);
        Arrays.fill(upper, 1.0);

        // Ziel: Summe der quadrierten Residuen minimieren
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(numberOfInterpolationPoints);

        ObjectiveFunction objective = new ObjectiveFunction(params -> {
            double[] resid = this.berechnenResiduum(params, lowVerteilung, zMittelwert);
            double sumSq = 0.0;
            for (double v : resid) sumSq += v * v;
            return sumSq;
        });

        PointValuePair result = optimizer.optimize(
                objective,
                GoalType.MINIMIZE,
                new InitialGuess(start),
                new MaxEval(2000),
                new SimpleBounds(lower, upper)
        );

        return result.getPoint(); // optimierte Parameter
    }



    public double[] optimizeWithBOBYQAEinfach(
            double zMittelwert,
            double[] lowVerteilung

    ) {
        double [] start1 = startwerte(zMittelwert,lowVerteilung);
        double[] start = new double[start1.length - 1];
        for (int i = 0, j = 0; i < start1.length; i++) {
            if (i != indexMaxKonz) {
                start[j++] = start1[i];
            }
        }
        int dim = start.length;
        int numberOfInterpolationPoints = 2 * dim + 1; // Empfehlung: 2*N+1

        // Bounds automatisch: 0.0 bis 1.0
        double[] lower = new double[dim];
        double[] upper = new double[dim];
        Arrays.fill(lower, 0.0);
        Arrays.fill(upper, 2.0);

        // Ziel: Summe der quadrierten Residuen minimieren
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(numberOfInterpolationPoints);

        ObjectiveFunction objective = new ObjectiveFunction(params -> {
            double[] resid = this.berechnenResiduumEinfach(params, lowVerteilung, zMittelwert);
            double sumSq = 0.0;
            for (double v : resid) sumSq += v * v;
            return sumSq;
        });

        PointValuePair result = optimizer.optimize(
                objective,
                GoalType.MINIMIZE,
                new InitialGuess(start),
                new MaxEval(2000),
                new SimpleBounds(lower, upper)
        );

        return result.getPoint(); // optimierte Parameter
    }


    public double [] ergebnisEinfach(double [] konz)
    {double [] ret = insertAt(konz,indexMaxKonz,maxValKonz);


        return normiereDaten(ret);
    }







    public void printOptimizedResult(
            double[] optimizedParams,
            double[] lowVerteilung,
            double zMittelwert
    ) {
        // 1. Z-Split berechnen
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);
        //optimizedParams=KonzDark(zMittelwert,lowVerteilung);
        //optimizedParams=normiereDaten(optimizedParams);

        // 2. Konzentrationsvektor erzeugen
        double[] konz_low = lowKonBe(optimizedParams[0], lowVerteilung);
        double[] konz_high = Arrays.copyOfRange(optimizedParams, 1, optimizedParams.length);
        //double[] konz_high = lowKonBe(1 - optimizedParams[0], Arrays.copyOfRange(optimizedParams, 1, optimizedParams.length));

        double[] gesamtKonz = new double[z_split.indicesLow.length + z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high[i];
        }

        // 3. Mittlere Ordnungszahl berechnen
        double ordnungszahl_summe = 0.0;
        double konz_summe = 0.0;
        for (int i = 0; i < gesamtKonz.length; i++) {
            ordnungszahl_summe += gesamtKonz[i] * zNumbersArr[i];
            konz_summe += gesamtKonz[i];
        }
        double z_mittel_opt = ordnungszahl_summe / konz_summe;

        // 4. Berechnete Intensitäten bestimmen
        double[] berechneteIntensitaet = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz);

        double[] IbHigh = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            IbHigh[i] = berechneteIntensitaet[z_split.indicesHigh[i]];
        }

        double geo = geoIbIg(IbHigh);
        double[] berechneteIntensitaet1 = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz,geo);
        //double[] arr = {29.6599, 34.3829, 35.9572};
        //double[] berechneteIntensitaet1 = calcDark.berechneSummenintensitaetMitKonz(arr,3.56e-09);




        // 5. Ausgabe
        System.out.println("===== BOBYQA-Ergebnis-Report =====");
        System.out.println("konz_summe"+konz_summe);
        System.out.println("Optimierte Konzentrationen: " + Arrays.toString(gesamtKonz));
        System.out.printf("Optimierte mittlere Ordnungszahl: %.4f%n", z_mittel_opt);
        System.out.println("Berechnete Intensitäten: " + Arrays.toString(berechneteIntensitaet1));
        System.out.println("==================================");
    }




    public void printOptimizedResultEinfach(
            double[] optimizedParams,
            double[] lowVerteilung,
            double zMittelwert
    ) {

        double [] optimizedParamsGanz = ergebnisEinfach(optimizedParams);
        // 1. Z-Split berechnen
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);
        //optimizedParams=KonzDark(zMittelwert,lowVerteilung);
        //optimizedParams=normiereDaten(optimizedParams);

        double[] alg_angepasst = applyZAnpassen(optimizedParamsGanz,lowVerteilung,zMittelwert);

        // 2. Konzentrationsvektor erzeugen
        double[] konz_low = lowKonBe(alg_angepasst[0], lowVerteilung);
        double[] konz_high = Arrays.copyOfRange(alg_angepasst, 1, alg_angepasst.length);
        //double[] konz_high = lowKonBe(1 - optimizedParams[0], Arrays.copyOfRange(optimizedParams, 1, optimizedParams.length));

        double[] gesamtKonz = new double[z_split.indicesLow.length + z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high[i];
        }



        // 3. Mittlere Ordnungszahl berechnen
        double ordnungszahl_summe = 0.0;
        double konz_summe = 0.0;
        for (int i = 0; i < gesamtKonz.length; i++) {
            ordnungszahl_summe += gesamtKonz[i] * zNumbersArr[i];
            konz_summe += gesamtKonz[i];
        }
        double z_mittel_opt = ordnungszahl_summe / konz_summe;

        // 4. Berechnete Intensitäten bestimmen
        double[] berechneteIntensitaet = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz);

        double[] IbHigh = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            IbHigh[i] = berechneteIntensitaet[z_split.indicesHigh[i]];
        }

        double geo = geoIbIg(IbHigh);
        double[] berechneteIntensitaet1 = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz,geo);
        //double[] arr = {29.6599, 34.3829, 35.9572};
        //double[] berechneteIntensitaet1 = calcDark.berechneSummenintensitaetMitKonz(arr,3.56e-09);


        String out = Arrays.stream(gesamtKonz)
                .map(v -> v * 100.0)
                .mapToObj(v -> String.format(Locale.GERMANY, "%.4f", v)) // GERMANY = Komma
                .collect(Collectors.joining(", ", "[", "]"));



        // 5. Ausgabe
        System.out.println("===== Ergebnis-Report =====");
        System.out.println("konz_summe"+konz_summe);
        System.out.println("Optimierte Konzentrationen: " + out + " %");
        System.out.printf("Optimierte mittlere Ordnungszahl, wird hier zwangsweise immer erreicht: %.4f%n", z_mittel_opt);
        System.out.println("Berechnete Intensitäten: " + Arrays.toString(berechneteIntensitaet1));
        System.out.println("==================================");
    }









    public double[] applyZAnpassen(double[] optimaleKonz, double[] lowVerteilung, double zMittelwert) {
        // Ordnungszahlen holen & splitten
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        // Low(-gesamt) aus optimaleKonz[0] aufspannen, High als Einzelwerte ab Index 1
        double[] konz_low  = lowKonBe(optimaleKonz[0], lowVerteilung);
        double[] konz_high = Arrays.copyOfRange(optimaleKonz, 1, optimaleKonz.length);

        // Sicherheitscheck: Längen müssen zum Split passen
        if (konz_low.length != z_split.indicesLow.length) {
            throw new IllegalArgumentException("konz_low.len != indicesLow.len");
        }
        if (konz_high.length != z_split.indicesHigh.length) {
            throw new IllegalArgumentException("konz_high.len != indicesHigh.len");
        }

        // Z-Anpassung (liefert zwei Faktoren: für Low und High)
        double[] factors = ZAnpassen(konz_low, z_split.valuesLow, konz_high, z_split.valuesHigh, zMittelwert);

        // Faktoren anwenden
        for (int i = 0; i < konz_low.length; i++)  konz_low[i]  *= factors[0];
        for (int i = 0; i < konz_high.length; i++) konz_high[i] *= factors[1];

        // Low-Gesamtsumme bilden
        double sumLow = 0.0;
        for (double v : konz_low) sumLow += v;

        // Rückgabeformat: [SummeLow, High...]
        double[] adjusted = new double[1 + konz_high.length];
        adjusted[0] = sumLow;
        System.arraycopy(konz_high, 0, adjusted, 1, konz_high.length);

        return adjusted;
    }




    public double[] optimizeWithALGLIB_DF(double zMittelwert, double[] lowVerteilung) {
        double[] start = startwerte(zMittelwert, lowVerteilung);
        double epsf = 1e-8;      // Abbruchkriterium auf f
        // int maxits = 5000;    // optional, falls deine ALGLIB-Bindung das anbietet
        double[] bndl = new double[start.length];
        double[] bndu = new double[start.length];
        Arrays.fill(bndl, 0);  // untere Grenzen
        Arrays.fill(bndu,  1);  // obere Grenzen


        try {
            alglib.mindfstate state = alglib.mindfcreate(start.length, start);
            alglib.mindfsetcondf(state, epsf);
            // alglib.mindfsetmaxits(state, maxits); // optional
            alglib.mindfsetbc(state, bndl, bndu);

            // <<< WICHTIG: Instanzmethode, kein static, keine Bounds >>>
            Object ctx = new Object[]{ lowVerteilung, zMittelwert };
            alglib.mindfoptimize(state, this::alglibObjective, null, ctx);

            alglib.mindfresults_results results = alglib.mindfresults(state);
// Zielwert aus den optimierten Parametern neu berechnen
            double[] residFinal = this.berechnenResiduum(results.x, lowVerteilung, zMittelwert);
            double sumSqFinal = 0.0;
            for (double r : residFinal) sumSqFinal += r * r;

            System.out.printf("[ALGLIB mindf] terminationtype=%d, f=%.6e%n",
                    results.rep.terminationtype, sumSqFinal);

            return results.x;
        } catch (alglib.exception e) {
            System.err.println("ALGLIB-Fehler: " + e.getMessage());
            return start; // Fallback
        }
    }

    /** ALGLIB-Callback: Instanzmethode, schreibt f in fi[0], keine Bounds/Clamps */
    private void alglibObjective(double[] params, double[] fi, Object obj) {
        Object[] ctx = (Object[]) obj;
        double[] lowVerteilung = (double[]) ctx[0];
        double zMittelwert = (Double) ctx[1];

        double[] resid = this.berechnenResiduum(params, lowVerteilung, zMittelwert);
        double sumSq = 0.0;
        for (double r : resid) sumSq += r * r;
        fi[0] = sumSq;
    }


    public double[] optimizeWithALGLIB_DF_Einfach(double zMittelwert, double[] lowVerteilung) {
        double [] start1 = startwerte(zMittelwert,lowVerteilung);
        double[] start = new double[start1.length - 1];
        for (int i = 0, j = 0; i < start1.length; i++) {
            if (i != indexMaxKonz) {
                start[j++] = start1[i];
            }
        }






        double epsf = 1e-8;      // Abbruchkriterium auf f
        // int maxits = 5000;    // optional, falls deine ALGLIB-Bindung das anbietet
        double[] bndl = new double[start.length];
        double[] bndu = new double[start.length];
        Arrays.fill(bndl, 0);  // untere Grenzen
        Arrays.fill(bndu,  2);  // obere Grenzen


        try {
            alglib.mindfstate state = alglib.mindfcreate(start.length, start);
            alglib.mindfsetcondf(state, epsf);
            // alglib.mindfsetmaxits(state, maxits); // optional
            alglib.mindfsetbc(state, bndl, bndu);

            // <<< WICHTIG: Instanzmethode, kein static, keine Bounds >>>
            Object ctx = new Object[]{ lowVerteilung, zMittelwert };
            alglib.mindfoptimize(state, this::alglibObjectiveEinfach, null, ctx);


            alglib.mindfresults_results results = alglib.mindfresults(state);
// Zielwert aus den optimierten Parametern neu berechnen
            double[] residFinal = this.berechnenResiduumEinfach(results.x, lowVerteilung, zMittelwert);
            double sumSqFinal = 0.0;
            for (double r : residFinal) sumSqFinal += r * r;

            System.out.printf("[ALGLIB mindf] terminationtype=%d, f=%.6e%n",
                    results.rep.terminationtype, sumSqFinal);

            return results.x;
        } catch (alglib.exception e) {
            System.err.println("ALGLIB-Fehler: " + e.getMessage());
            return start; // Fallback
        }
    }

    private void alglibObjectiveEinfach(double[] params, double[] fi, Object obj) {
        Object[] ctx = (Object[]) obj;
        double[] lowVerteilung = (double[]) ctx[0];
        double zMittelwert = (Double) ctx[1];

        double[] resid = this.berechnenResiduumEinfach(params, lowVerteilung, zMittelwert);
        double sumSq = 0.0;
        for (double r : resid) sumSq += r * r;
        fi[0] = sumSq;
    }





    public double[] optimizeWithALGLIB_MINBC_Einfach(double zMittelwert, double[] lowVerteilung) {
        // Startvektor auf "Einfach"-Form reduzieren (Binder + High ohne Maximum)
        double[] start1 = startwerte(zMittelwert, lowVerteilung);
        double[] start = new double[start1.length - 1];
        for (int i = 0, j = 0; i < start1.length; i++) {
            if (i != indexMaxKonz) {
                start[j++] = start1[i];
            }
        }

        // Bounds wie in DF-Variante
        double[] bndl = new double[start.length];
        double[] bndu = new double[start.length];
        java.util.Arrays.fill(bndl, 0.0);
        java.util.Arrays.fill(bndu, 2.0);

        // Toleranzen / Numerik
        double diffStep = 1e-6; // Schrittweite für numerische Ableitungen
        double epsg = 1e-8;
        double epsf = 0.0;
        double epsx = 1e-8;
        int maxits = 0; // 0 = automatisch

        try {
            // Zustand anlegen (funktionsbasierte Variante, Gradient wird numerisch approximiert)
            alglib.minbcstate state = alglib.minbccreatef(start, diffStep);

            // Grenzen & Abbruchkriterien setzen
            alglib.minbcsetbc(state, bndl, bndu);
            alglib.minbcsetcond(state, epsg, epsf, epsx, maxits);

            // Kontext übergeben (lowVerteilung, zMittelwert)
            Object ctx = new Object[]{ lowVerteilung, zMittelwert };

            // Optimierung starten – Objective gibt einen double zurück
            alglib.minbcoptimize(state, (params, obj) -> {
                Object[] c = (Object[]) obj;
                double[] lowV = (double[]) c[0];
                double z = (Double) c[1];

                double[] resid = this.berechnenResiduumEinfach(params, lowV, z);
                double sumSq = 0.0;
                for (double r : resid) sumSq += r * r;
                return sumSq;
            }, null, ctx);

            // Ergebnisse
            alglib.minbcresults_results results = alglib.minbcresults(state);

            double[] residFinal = this.berechnenResiduumEinfach(results.x, lowVerteilung, zMittelwert);
            double f = 0.0;
            for (double r : residFinal) f += r * r;

            System.out.printf("[ALGLIB minbc] terminationtype=%d, f=%.6e%n",
                    results.rep.terminationtype, f);

            return results.x; // Reduzierter Parametervektor (Einfach)

        } catch (alglib.exception e) {
            System.err.println("ALGLIB-Fehler (minbc): " + e.getMessage());
            return start; // Fallback
        }
    }



    public double[] optimizeWithALGLIB_MINBLEIC_Einfach(double zMittelwert, double[] lowVerteilung) {
        // Startvektor auf "Einfach"-Form reduzieren (Binder + High ohne Maximum)
        double[] start1 = startwerte(zMittelwert, lowVerteilung);
        double[] start = new double[start1.length - 1];
        for (int i = 0, j = 0; i < start1.length; i++) {
            if (i != indexMaxKonz) {
                start[j++] = start1[i];
            }
        }

        // Box-Bounds wie in DF/BC
        double[] bndl = new double[start.length];
        double[] bndu = new double[start.length];
        java.util.Arrays.fill(bndl, 0.0);
        java.util.Arrays.fill(bndu, 2.0);

        // Toleranzen
        double diffStep = 1e-6; // Schrittweite f. numerische Ableitungen
        double epsg = 1e-8;
        double epsf = 0.0;
        double epsx = 1e-8;
        int maxits = 0; // 0 = auto

        try {
            // Zustand (Funktion + numerischer Grad)
            alglib.minbleicstate state = alglib.minbleiccreatef(start, diffStep);

            // Box-Constraints
            alglib.minbleicsetbc(state, bndl, bndu);

            // (Optional) Lineare Constraints hier setzen, z.B. Summe <= 1.0:
            // double[][] c = new double[1][start.length];
            // java.util.Arrays.fill(c[0], 1.0);
            // double[] ct = new double[]{ 1.0 };
            // alglib.minbleicsetlc(state, c, ct, 0, 1); // 0 Gleichungen, 1 Ungleichung

            // Abbruchkriterien
            alglib.minbleicsetcond(state, epsg, epsf, epsx, maxits);

            // Kontext
            Object ctx = new Object[]{ lowVerteilung, zMittelwert };

            // Optimierung
            alglib.minbleicoptimize(state, (params, obj) -> {
                Object[] c = (Object[]) obj;
                double[] lowV = (double[]) c[0];
                double z = (Double) c[1];

                double[] resid = this.berechnenResiduumEinfach(params, lowV, z);
                double sumSq = 0.0;
                for (double r : resid) sumSq += r * r;
                return sumSq;
            }, null, ctx);

            // Ergebnisse
            alglib.minbleicresults_results results = alglib.minbleicresults(state);

            double[] residFinal = this.berechnenResiduumEinfach(results.x, lowVerteilung, zMittelwert);
            double f = 0.0;
            for (double r : residFinal) f += r * r;

            System.out.printf("[ALGLIB minbleic] terminationtype=%d, f=%.6e%n",
                    results.rep.terminationtype, f);

            return results.x; // reduzierter Vektor (Einfach)

        } catch (alglib.exception e) {
            System.err.println("ALGLIB-Fehler (minbleic): " + e.getMessage());
            return start; // Fallback
        }
    }


    // Hipparchus NLLS (Levenberg–Marquardt) – "Einfach"-Parametrisierung
    public double[] optimizeWithHIPPARCHUS_MINLM_Einfach(double zMittelwert, double[] lowVerteilung) {
        // Startvektor auf "Einfach" reduzieren
        double[] start1 = startwerte(zMittelwert, lowVerteilung);
        double[] start  = new double[start1.length - 1];
        for (int i = 0, j = 0; i < start1.length; i++) {
            if (i != indexMaxKonz) start[j++] = start1[i];
        }

        // Residuen-Länge = #High-Elemente (wie in berechnenResiduumEinfach)
        java.util.List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult zSplit = splitByZ(zArr, darkI);
        final int m = zSplit.indicesHigh.length;
        final int n = start.length;
        //System.out.println("m"+m+"n"+n);

        // Modell: gibt Residuen und numerischen Jacobi zurück

        MultivariateJacobianFunction model = point -> {
            double[] p = point.toArray();

            // Optional: leicht clampen, um NaN/Inf zu vermeiden (Hipparchus-LM hat keine Bounds)
            for (int j = 0; j < p.length; j++) {
                if (!Double.isFinite(p[j])) p[j] = 1.0;
                // weicher Korridor wie in DF/BC/bleic
                if (p[j] < 1e-12) p[j] = 1e-12;
                if (p[j] > 2.0)   p[j] = 2.0;
            }

            double[] resid = this.berechnenResiduumEinfach(p, lowVerteilung, zMittelwert);
            // Safety: keine NaNs/Inf an den Optimierer liefern
            for (int i = 0; i < resid.length; i++) {
                if (!Double.isFinite(resid[i])) resid[i] = 1e150;
            }


            /*

            final double h = 1e-6; // Schrittweite für Zentraldifferenzen
            // Numerischer Jacobi (Zentraldifferenzen)
            double[][] J = new double[m][n];
            for (int j = 0; j < n; j++) {
                double orig = p[j];

                p[j] = orig + h;
                double[] rp = this.berechnenResiduumEinfach(p, lowVerteilung, zMittelwert);

                p[j] = orig - h;
                double[] rm = this.berechnenResiduumEinfach(p, lowVerteilung, zMittelwert);

                p[j] = orig;

                for (int i = 0; i < m; i++) {
                    double d = (rp[i] - rm[i]) / (2 * h);
                    if (!Double.isFinite(d)) d = 0.0;
                    J[i][j] = d;
                }
            }


             */


            final double [] h = computeAbsoluteStep2Point(p); // Schrittweite für Zentraldifferenzen
            double[][] J = new double[m][n];
            for (int j = 0; j < n; j++) {
                double orig = p[j];

                p[j] = orig + h[j];
                double[] rp = this.berechnenResiduumEinfach(p, lowVerteilung, zMittelwert);

                p[j] = orig;
                double[] rm = this.berechnenResiduumEinfach(p, lowVerteilung, zMittelwert);

                //p[j] = orig;

                for (int i = 0; i < m; i++) {
                    double d = (rp[i] - rm[i]) / (h[j]);
                    if (!Double.isFinite(d)) d = 0.0;
                    J[i][j] = d;
                }
            }








            RealVector value    = new ArrayRealVector(resid, false);
            RealMatrix jacobian = new Array2DRowRealMatrix(J, false);
            return new Pair<>(value, jacobian);
        };

        // Ziel = 0-Vector (weil Modell bereits Residuen liefert)
        RealVector target = new ArrayRealVector(new double[m]);
        RealVector startVec = new ArrayRealVector(start, false);

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(startVec)
                .target(target)
                .model(model)
                .maxEvaluations(2000)
                .maxIterations(2000)
                .checkerPair(new SimpleVectorValueChecker(1e-10, 1e-10))
                .build();

        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum opt = optimizer.optimize(problem);

        // Logging (optional)
        double[] residFinal = this.berechnenResiduumEinfach(opt.getPoint().toArray(), lowVerteilung, zMittelwert);
        double f = 0.0; for (double r : residFinal) f += r * r;
        System.out.printf("[Hipparchus minlm] eval=%d it=%d, f=%.6e%n",
                opt.getEvaluations(), opt.getIterations(), f);

        return opt.getPoint().toArray(); // reduzierter Vektor (Einfach)
    }


    public static double[] computeAbsoluteStep2Point(double[] x0, double[] relStep) {
        final double rstep = Math.sqrt(Math.ulp(1.0)); // ≈ 1.49e-8
        final int n = x0.length;

        double[] sign = new double[n];
        for (int i = 0; i < n; i++) sign[i] = (x0[i] >= 0.0) ? 1.0 : -1.0; // 1 bei x0==0

        double[] absStep = new double[n];

        if (relStep == null) {
            for (int i = 0; i < n; i++) {
                absStep[i] = rstep * sign[i] * Math.max(1.0, Math.abs(x0[i]));
            }
        } else {
            if (relStep.length != n)
                throw new IllegalArgumentException("relStep und x0 müssen gleich lang sein.");
            for (int i = 0; i < n; i++) {
                double a  = relStep[i] * sign[i] * Math.abs(x0[i]);
                double dx = (x0[i] + a) - x0[i];
                absStep[i] = (dx == 0.0)
                        ? rstep * sign[i] * Math.max(1.0, Math.abs(x0[i]))
                        : a;
            }
        }
        return absStep;
    }

    /** Bequeme Überladung wie rel_step=None in SciPy. */
    public static double[] computeAbsoluteStep2Point(double[] x0) {
        return computeAbsoluteStep2Point(x0, null);
    }












}
