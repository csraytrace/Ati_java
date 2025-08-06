package org.example;

import java.util.List;

public class CalcIDark {

    private CalcI calcDark;
    private CalcI calcFiltered;
    private PreparedValues Dark;
    private PreparedValues Filtered;
    private final int darkI = 12;

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
        //int darkI = 12;
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
            System.out.println(zMittelLow + " " + zMittelHigh + " " + zGewuenscht + " Wert muss dazwischen liegen!");
            System.err.println("WARNUNG: Z nicht in den Grenzen" + zMittelLow + "<" + zGewuenscht + "<" +  zMittelHigh);
            // throw new IllegalArgumentException("Z_gemittel kann nicht erreicht werden!");
            return new double[]{
                    Math.abs(konzNeuLow / konzSumLow),
                    Math.abs(konzNeuHigh / konzSumHigh)
            };
        }

        return new double[]{konzNeuLow / konzSumLow, konzNeuHigh / konzSumHigh};
        //return new double[]{konzNeuLow / konzSumLowNorm, konzNeuHigh / konzSumHighNorm};  #falls die originalen Konzentrationenn nicht normiert werden sollen
    }



    public double[] KonzDark(double Z_mittelwert, double [] low_verteilung) {

        double [] ret = CalcI.berechneRelKonzentrationen(this.calcFiltered,this.Filtered);
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        int countLow = 0, countHigh = 0;
        for (int z : zNumbersArr) {
            if (z < this.darkI) countLow++;
            else countHigh++;
        }
        int[] indexLowArr = new int[countLow];
        int[] indexHighArr = new int[countHigh];
        int[] zLowArr = new int[countLow];
        int[] zHighArr = new int[countHigh];
        int li = 0, hi = 0;
        for (int i = 0; i < zNumbersArr.length; i++) {
            if (zNumbersArr[i] < darkI) {
                indexLowArr[li] = i;
                zLowArr[li++] = zNumbersArr[i];
            } else {
                indexHighArr[hi] = i;
                zHighArr[hi++] = zNumbersArr[i];
            }
        }
        //double[] konzLow, int[] zLow, double[] konzHigh, int[] zHigh, double zGewuenscht
        //double [] x = ZAnpassen()

        return ret;

    }








}
