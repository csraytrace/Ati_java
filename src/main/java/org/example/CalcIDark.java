package org.example;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;

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
    private Probe probeBind;
    private int [] indexBind;
    private Verbindung binderProbe;
    private BinderSummary binderWerte;  //sum und mittleres Z
    private int addedDark;

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
            List<Verbindung> filter_det,
            Verbindung binder

    ) {
        assertBinderAllZBelowDarkI(binder);
        ProbeAddResult Probeneu = addMissingBinderElementsToProbeCount(probe, binder);
        Probe effectiveProbe = Probeneu.probe;
        addedDark = Probeneu.added;

        probeBind = effectiveProbe;
        binderProbe = binder;
        indexBind = mapBinderElementsToProbeIndicesOrThrow();

        this.calcDark = new CalcI(dateipfad, effectiveProbe, röhrenTyp, röhrenmaterial,
                einfallswinkelalpha, einfallswinkelbeta, fensterwinkel, sigma,
                charzucontL, fenstermaterialRöhre, fensterdickeRöhre, raumwinkel, röhrenstrom,
                emin, emax, step, messzeit, charzucont, fenstermaterialDet, fensterdickeDet,
                phiDet, kontaktmaterial, kontaktmaterialdicke, bedeckungsfaktor, palphaGrad,
                pbetaGrad, detektormaterial, totschicht, activeLayer, filter_röhre, filter_det);

        Probe probeFiltered = effectiveProbe.filterByMinZ(darkI);
        this.calcFiltered = new CalcI(dateipfad, probeFiltered, röhrenTyp, röhrenmaterial,
                einfallswinkelalpha, einfallswinkelbeta, fensterwinkel, sigma,
                charzucontL, fenstermaterialRöhre, fensterdickeRöhre, raumwinkel, röhrenstrom,
                emin, emax, step, messzeit, charzucont, fenstermaterialDet, fensterdickeDet,
                phiDet, kontaktmaterial, kontaktmaterialdicke, bedeckungsfaktor, palphaGrad,
                pbetaGrad, detektormaterial, totschicht, activeLayer, filter_röhre, filter_det);


        Dark = calcDark.werteVorbereitenAlle();
        Filtered = calcFiltered.werteVorbereitenAlle();
        binderWerte = binderSummaryOrZero();


    }

    public int[] getIndexBind() {
        return indexBind == null ? null : indexBind.clone();
    }

    private Probe addMissingBinderElementsToProbe(Probe probe, Verbindung binder) {
        if (binder == null) return probe;

        // vorhandene Z der Probe in ein Set
        List<Integer> probeZ = probe.getElementZNumbers();
        java.util.Set<Integer> seenZ = new java.util.HashSet<>(probeZ);

        // Binder-Symbole und -Z lesen (Index-weise korreliert)
        String[] binderSym = binder.getSymbole();
        List<Double> binderZ = binder.getZ_List();

        Probe p = probe; // evtl. schrittweise erweitern
        int n = Math.min(binderSym.length, binderZ.size());
        for (int i = 0; i < n; i++) {
            int Z = (int) Math.round(binderZ.get(i));
            if (!seenZ.contains(Z)) {
                // Element fehlt in Probe → anhängen
                p = p.withAddedElement(binderSym[i]);
                seenZ.add(Z);
            }
        }
        return p;
    }


    private record ProbeAddResult(Probe probe, int added) {}

    private ProbeAddResult addMissingBinderElementsToProbeCount(Probe probe, Verbindung binder) {
        if (binder == null) return new ProbeAddResult(probe, 0);

        List<Integer> probeZ = probe.getElementZNumbers();
        java.util.Set<Integer> seenZ = new java.util.HashSet<>(probeZ);

        String[] binderSym = binder.getSymbole();
        List<Double> binderZ = binder.getZ_List();

        Probe p = probe;
        int n = Math.min(binderSym.length, binderZ.size());
        int added = 0;

        for (int i = 0; i < n; i++) {
            int Z = (int) Math.round(binderZ.get(i));
            if (!seenZ.contains(Z)) {
                p = p.withAddedElement(binderSym[i]);
                seenZ.add(Z);
                added++;
            }
        }
        return new ProbeAddResult(p, added);
    }


    private void assertBinderAllZBelowDarkI(Verbindung binder) {
        if (binder == null) return; // nichts zu prüfen

        List<Double> zList = binder.getZ_List(); // Z-Werte der Binder-Verbindung
        List<Integer> offending = new ArrayList<>();

        for (double z : zList) {
            int Zi = (int) Math.round(z);
            if (Zi >= darkI) offending.add(Zi);
        }

        if (!offending.isEmpty()) {
            throw new IllegalArgumentException(
                    "Binder enthält Elemente mit Z >= " + darkI + ": " + offending
            );
        }
    }


    private int[] mapBinderElementsToProbeIndicesOrThrow() {
        if (binderProbe == null) return new int[0];

        // Map: Z -> Index in der Probe (erster Treffer gewinnt)
        java.util.List<Integer> probeZ = probeBind.getElementZNumbers();
        java.util.Map<Integer, Integer> zToIdx = new java.util.HashMap<>();
        for (int i = 0; i < probeZ.size(); i++) {
            zToIdx.putIfAbsent(probeZ.get(i), i);
        }

        // Binder-Zahlen & -Symbole (parallel)
        java.util.List<Double> binderZ = binderProbe.getZ_List();
        String[] binderSym = binderProbe.getSymbole();
        int n = Math.min(binderZ.size(), binderSym.length);

        int[] indices = new int[n];
        java.util.List<String> missing = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            int Z = (int) Math.round(binderZ.get(i));
            Integer idx = zToIdx.get(Z);
            if (idx == null) {
                // sammeln für aussagekräftige Fehlermeldung
                String sym = binderSym[i];
                missing.add("Z=" + Z + " (Symbol=" + sym + ")");
                indices[i] = -1; // Platzhalter; wird gleich via Exception abgelehnt
            } else {
                indices[i] = idx;
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Binder-Element(e) nicht in der Probe gefunden: " +
                            String.join(", ", missing) +
                            ". Erweitere die Probe vorher (z.B. withAddedElement) oder passe den Binder an."
            );
        }

        return indices;
    }


    private record BinderSummary(double total, double zAvg) {}

    private BinderSummary binderSummaryOrZero() {
        if (binderProbe == null) {
            return new BinderSummary(0.0, 1); // kein Binder
        }
        double[] conc = binderProbe.getKonzentrationen();
        java.util.List<Double> zList = binderProbe.getZ_List();

        if (conc.length != zList.size()) {
            throw new IllegalStateException("Längen passen nicht: konzentrationen="
                    + conc.length + ", Z_List=" + zList.size());
        }

        double sum = 0.0;
        double sumWeightedZ = 0.0;
        for (int i = 0; i < conc.length; i++) {
            double c = conc[i];
            double z = zList.get(i);
            sum += c;
            sumWeightedZ += c * z;
        }

        final double TOL = 1e-12;
        if (sum < -TOL) {
            throw new IllegalStateException(String.format("Binder-Konzentrationen negativ (Summe=%.6g).", sum));
        }
        if (sum > 1.0 + TOL) {
            throw new IllegalStateException(String.format("Binder-Konzentration > 1 (Summe=%.6g).", sum));
        }

        // kosmetisch clampen
        if (Math.abs(sum) < TOL) sum = 0.0;
        if (Math.abs(sum - 1.0) < TOL) sum = 1.0;

        // zAvg ist nur sinnvoll, wenn sum > 0
        double zAvg = (sum > 0.0) ? (sumWeightedZ / sum) : Double.NaN;

        return new BinderSummary(sum, zAvg);
    }


    private double[] lowKonBe(double lowKon, double[] verteilung) {
        double[] norVert = normiereDaten(verteilung); // eigene Hilfsmethode
        double[] result = new double[norVert.length];
        for (int i = 0; i < norVert.length; i++) {
            result[i] = lowKon * norVert[i];
        }
        return result;
    }


    private double[] lowKonBeBinderAware(double lowKon, double[] lowVerteilung, double sumParams) {
        // Kein Binder -> Standard
        if (binderProbe == null) {
            return lowKonBe(lowKon, lowVerteilung);
        }

        // Mapping sicherstellen
        if (indexBind == null) {
            indexBind = mapBinderElementsToProbeIndicesOrThrow();
        }

        // Low-Indizes der Probe bestimmen
        List<Integer> zNumbers = this.probeBind.getElementZNumbers();
        int[] zArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult zSplit = splitByZ(zArr, darkI);
        int[] lowIdx = zSplit.indicesLow;
        int totalElems = zArr.length;

        // Lookup: ProbeIndex -> Position im Low-Vektor
        int[] probeIdxToLowPos = new int[totalElems];
        java.util.Arrays.fill(probeIdxToLowPos, -1);
        for (int p = 0; p < lowIdx.length; p++) {
            probeIdxToLowPos[lowIdx[p]] = p;
        }

        // Binder-Konzentrationen auf Low-Reihenfolge mappen
        double[] binderConc = binderProbe.getKonzentrationen(); // evtl. extern skaliert
        double[] binderLow  = new double[lowIdx.length];
        for (int i = 0; i < binderConc.length; i++) {
            int probeIndex = (i < indexBind.length ? indexBind[i] : -1);
            if (probeIndex < 0 || probeIndex >= totalElems) continue;
            int pos = probeIdxToLowPos[probeIndex];
            if (pos >= 0) binderLow[pos] += binderConc[i];
        }

        // Summe Binderanteile (nach evtl. externer Skalierung)
        double sumBinder = 0.0;
        for (double b : binderConc) sumBinder += b;

        double denom = Math.max(sumParams, 1e-300);
        if (sumBinder >= (lowKon / denom)) {
            /*System.err.printf(
                    "WARNUNG: Binderanteil (%.6f) >= lowKon/Σparams (%.6f). Verwende Binder-Verteilung für Low-Seite.%n",
                    sumBinder, (lowKon / denom)
            );*/
            // Nutze Binder-Verteilung (auf Low gemappt) als Verteilung für lowKon
            return lowKonBe(lowKon, binderLow);
        }

        // Rest über die Basis-Low-Verteilung
        double rest = lowKon - sumBinder * sumParams;
        if (rest < 0) rest = 0.0;

        double[] low = lowKonBe(rest, lowVerteilung);

        // Binder additiv (skaliert mit Σparams)
        if (sumParams != 0.0) {
            double scale = sumParams;
            for (int i = 0; i < low.length; i++) {
                low[i] += binderLow[i] * scale;
            }
        }
        return low;
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

        double sumlow = 0.0;
        for (double v : low_verteilung) sumlow += v;
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        double[] Startkonzentration = new double[z_split.valuesHigh.length + 1];
        for (int i = 0; i < relKonz.length; i++) {
            Startkonzentration[i + 1] = relKonz[i];
        }

        double konzBinder = binderWerte.total();
        double zBinder = binderWerte.zAvg();

        double Z_mittelwerte_ohne_Binder = (Z_mittelwert - konzBinder * zBinder) / (1 - konzBinder);

        double[] x_y = ZAnpassen( low_verteilung, z_split.valuesLow, relKonz, z_split.valuesHigh, Z_mittelwerte_ohne_Binder);

        double x = x_y[0] * (1 - konzBinder);
        double y = x_y[1] * (1 - konzBinder);

        for (int i = 1; i < Startkonzentration.length; i++) {
            Startkonzentration[i] *= y;
        }
        Startkonzentration[0] = x * sumlow + konzBinder;

        double sumParams = 0.0;
        for (double v : Startkonzentration) sumParams += v;

        double[] konz_low_start = lowKonBeBinderAware(Startkonzentration[0], low_verteilung, sumParams);
        double[] konz_high_start = Arrays.copyOfRange(Startkonzentration, 1, Startkonzentration.length);

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

        x_y = ZAnpassen( low_verteilung, z_split.valuesLow, startKonzAb, z_split.valuesHigh, Z_mittelwerte_ohne_Binder);
        x = x_y[0] * (1 - konzBinder);;
        y = x_y[1] * (1 - konzBinder);;
        for (int i = 1; i < Startkonzentration.length; i++) {
            Startkonzentration[i] *= y;
        }
        Startkonzentration[0] = sumlow * x + konzBinder;
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
        //System.out.println("Startkonzentration: " + Arrays.toString(Startkonzentration));

        return Startkonzentration;
    }


    public double[] residuum(double[] params,
                             double[] low_verteilung,
                             double Z_mittelwert) {

        double [] konzentration = params.clone();
        double sumlow = 0.0;
        for (double v : low_verteilung) sumlow += v;

        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        double konzBinder = binderWerte.total();  // Zugriff auf 'total'
        double zBinder = binderWerte.zAvg();   // Zugriff auf 'zAvg'
        double Z_mittelwerte_ohne_Binder = (Z_mittelwert - konzBinder * zBinder) / (1 - konzBinder);
        double[] konz_high_start = Arrays.copyOfRange(konzentration, 1, konzentration.length);
        konz_high_start = insertAt(konz_high_start, indexMaxKonz - 1, maxValKonz);

        int gesamtLen = z_split.indicesLow.length + z_split.indicesHigh.length;
        double [] konz_low_start_ohne_Binder = lowKonBe(konzentration[0],low_verteilung);
        double[] x_y = ZAnpassen(konz_low_start_ohne_Binder, z_split.valuesLow, konz_high_start, z_split.valuesHigh, Z_mittelwerte_ohne_Binder);
        double x = x_y[0] * (1 - konzBinder);
        double y = x_y[1] * (1 - konzBinder);

        konzentration[0] *= x ;
        konzentration[0] += konzBinder ;

        for (int i = 0; i < konz_high_start.length; i++) {
            konz_high_start[i] *= y;
        }


        double sumParams = 0.0; for (double v : konz_high_start) sumParams += v;
        sumParams+=konzentration[0];


        double[] konz_low_start = lowKonBeBinderAware(konzentration[0], low_verteilung, sumParams);

        double[] gesamtKonz = new double[gesamtLen];
        for (int i = 0; i < z_split.indicesLow.length; i++) {
            gesamtKonz[z_split.indicesLow[i]] = konz_low_start[i];
        }
        for (int i = 0; i < z_split.indicesHigh.length; i++) {
            gesamtKonz[z_split.indicesHigh[i]] = konz_high_start[i];
        }

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
            diff[i] = berechneteIntensitaetDark1[idx] - gemInt[i];
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


    public double [] ergebnisEinfach(double [] konz)
    {double [] ret = insertAt(konz,indexMaxKonz,maxValKonz);
        return normiereDaten(ret);
    }


    public void printOptimizedResult(
            double[] optimizedParams,
            double[] lowVerteilung,
            double zMittelwert
    ) {

        double[] lowVerteilungNeu = appendZeros(lowVerteilung, addedDark);
        double [] optimizedParamsGanz = ergebnisEinfach(optimizedParams);
        // 1. Z-Split berechnen
        List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);


        double konzBinder = binderWerte.total();  // Zugriff auf 'total'
        double zBinder = binderWerte.zAvg();   // Zugriff auf 'zAvg'

        double Z_mittelwerte_ohne_Binder = (zMittelwert - konzBinder * zBinder) / (1 - konzBinder);
        double[] alg_angepasst = applyZAnpassen(optimizedParamsGanz,lowVerteilungNeu,Z_mittelwerte_ohne_Binder);

        for (int i = 0; i < alg_angepasst.length; i++) {
            alg_angepasst[i] *= (1-konzBinder); // faktor ist dein Multiplikator
        }

        // 2. Konzentrationsvektor erzeugen

        double sumParams = 0.0; for (double v : alg_angepasst) sumParams += v;
        sumParams += konzBinder;
        double[] konz_low = lowKonBeBinderAware(alg_angepasst[0] + konzBinder, lowVerteilungNeu, sumParams);


        double[] konz_high = Arrays.copyOfRange(alg_angepasst, 1, alg_angepasst.length);

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


        int n = gesamtKonz.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;


// Sortieren nach Ordnungszahl
        java.util.Arrays.sort(order, java.util.Comparator.comparingInt(i -> zNumbersArr[i]));

// Elemente (für Symbole)
        java.util.List<Element> elems = this.calcDark.getProbe().getElemente();

// Ausgabe
        System.out.println("===== Ergebnis-Report =====");
        System.out.println("konz_summe" + konz_summe);
        System.out.println("Optimierte Konzentrationen (nach Z sortiert):");
        for (int idx : order) {
            String sym = elems.get(idx).getSymbol();
            int z      = zNumbersArr[idx];
            double pct = gesamtKonz[idx] * 100.0;
            System.out.printf(java.util.Locale.GERMANY, "  %-3s (Z=%-2d): %.4f%%%n", sym, z, pct);
        }
        System.out.printf(java.util.Locale.GERMANY,
                "Optimierte mittlere Ordnungszahl, wird hier zwangsweise immer erreicht: %.4f%n",
                z_mittel_opt);
        System.out.println("Berechnete Intensitäten: " + java.util.Arrays.toString(berechneteIntensitaet1));
        System.out.printf(java.util.Locale.US, "Geometriefaktor: %.4e%n", geo);
        //for (int idx : order) {
        //    double pct = gesamtKonz[idx] * 100.0;
        //    System.out.printf(java.util.Locale.US, " %.4f%%%n &", pct);
       // }
        //System.out.printf(java.util.Locale.US, " %.4f%n //", z_mittel_opt);
        //System.out.printf(java.util.Locale.US, "Geometriefaktor: %.4e%n", geo);
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

    public double[] optimizeHIPPARCHUS(double zMittelwert, double[] lowVerteilung) {
        // Startvektor auf "Einfach" reduzieren
        //System.out.println("DAK"+addedDark);
        double[] lowVerteilungNeu = appendZeros(lowVerteilung, addedDark);

        double[] start1 = startwerte(zMittelwert, lowVerteilungNeu);
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
                if (p[j] < 1e-12) p[j] = 1e-12;
                if (p[j] > 2.0)   p[j] = 2.0;
            }

            double[] resid = this.residuum(p, lowVerteilungNeu, zMittelwert);
            // Safety: keine NaNs/Inf an den Optimierer liefern
            for (int i = 0; i < resid.length; i++) {
                if (!Double.isFinite(resid[i])) resid[i] = 1e150;
            }

            final double [] h = computeAbsoluteStep2Point(p); // Schrittweite für Zentraldifferenzen
            double[][] J = new double[m][n];
            for (int j = 0; j < n; j++) {
                double orig = p[j];

                p[j] = orig + h[j];
                double[] rp = this.residuum(p, lowVerteilungNeu, zMittelwert);

                p[j] = orig;
                double[] rm = this.residuum(p, lowVerteilungNeu, zMittelwert);

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
        double[] residFinal = this.residuum(opt.getPoint().toArray(), lowVerteilungNeu, zMittelwert);
        double f = 0.0; for (double r : residFinal) f += r * r;
        //System.out.printf("[Hipparchus minlm] eval=%d it=%d, f=%.6e%n",
             //   opt.getEvaluations(), opt.getIterations(), f);
        printOptimizedResult(opt.getPoint().toArray(),lowVerteilung,zMittelwert);

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


    static double[] appendZeros(double[] a, int nZeros) {
        if (nZeros <= 0) return a;              // nichts zu tun
        return Arrays.copyOf(a, a.length + nZeros);
    }


    // Ein kleines DTO für die UI
    public static class DarkUIResult {
        public final java.util.LinkedHashMap<String,Object> row; // "Name", Elemente…, "Z", "Geo"
        public final double[] gesamtKonz;                        // in 0..1
        public final double zMittel;
        public final double geo;

        public DarkUIResult(java.util.LinkedHashMap<String,Object> row,
                            double[] gesamtKonz, double zMittel, double geo) {
            this.row = row; this.gesamtKonz = gesamtKonz; this.zMittel = zMittel; this.geo = geo;
        }
    }

    /** Wie printOptimizedResult(...), nur ohne Console-Output – liefert alles für die UI. */
    public DarkUIResult computeOptimizedResultForUI(
            String resultName,
            double[] optimizedParams,
            double[] lowVerteilung,
            double zMittelwert
    ) {
        double[] lowVerteilungNeu = appendZeros(lowVerteilung, addedDark);
        double[] optimizedParamsGanz = ergebnisEinfach(optimizedParams);

        // 1) Z-Split / Binder
        java.util.List<Integer> zNumbers = this.calcDark.getProbe().getElementZNumbers();
        int[] zNumbersArr = zNumbers.stream().mapToInt(Integer::intValue).toArray();
        SplitResult z_split = splitByZ(zNumbersArr, darkI);

        double konzBinder = binderWerte.total();
        double zBinder    = binderWerte.zAvg();

        double Z_mittelwerte_ohne_Binder = (zMittelwert - konzBinder * zBinder) / (1 - konzBinder);
        double[] alg_angepasst = applyZAnpassen(optimizedParamsGanz, lowVerteilungNeu, Z_mittelwerte_ohne_Binder);
        for (int i = 0; i < alg_angepasst.length; i++) alg_angepasst[i] *= (1 - konzBinder);

        // 2) Konzentrationsvektor (inkl. Binder) zusammensetzen
        double sumParams = 0.0; for (double v : alg_angepasst) sumParams += v;
        sumParams += konzBinder;
        double[] konz_low  = lowKonBeBinderAware(alg_angepasst[0] + konzBinder, lowVerteilungNeu, sumParams);
        double[] konz_high = java.util.Arrays.copyOfRange(alg_angepasst, 1, alg_angepasst.length);

        double[] gesamtKonz = new double[z_split.indicesLow.length + z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesLow.length;  i++) gesamtKonz[z_split.indicesLow[i]]  = konz_low[i];
        for (int i = 0; i < z_split.indicesHigh.length; i++) gesamtKonz[z_split.indicesHigh[i]] = konz_high[i];

        // 3) Z_mittel und Geo
        double ordnungszahl_summe = 0.0, konz_summe = 0.0;
        for (int i = 0; i < gesamtKonz.length; i++) { ordnungszahl_summe += gesamtKonz[i] * zNumbersArr[i]; konz_summe += gesamtKonz[i]; }
        double z_mittel_opt = ordnungszahl_summe / konz_summe;

        double[] berechneteIntensitaet = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz);
        double[] IbHigh = new double[z_split.indicesHigh.length];
        for (int i = 0; i < z_split.indicesHigh.length; i++) IbHigh[i] = berechneteIntensitaet[z_split.indicesHigh[i]];
        double geo = geoIbIg(IbHigh);
        double[] _unused = calcDark.berechneSummenintensitaetMitKonz(gesamtKonz, geo); // nur zur Konsistenz

        // 4) UI-Row: Name, Elemente (nach Z sortiert), Z, Geo
        Integer[] order = new Integer[gesamtKonz.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingInt(i -> zNumbersArr[i]));

        java.util.List<Element> elems = this.calcDark.getProbe().getElemente();

        java.util.LinkedHashMap<String,Object> row = new java.util.LinkedHashMap<>();
        row.put("Name", resultName);
        for (int idx : order) {
            String sym = elems.get(idx).getSymbol();
            double pct = gesamtKonz[idx] * 100.0;
            row.put(sym, pct);
        }
        row.put("Z", z_mittel_opt);
        row.put("Geo", geo);

        return new DarkUIResult(row, gesamtKonz, z_mittel_opt, geo);
    }



}
