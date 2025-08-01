package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Element {
    private String symbol;
    private int atomicNumber;
    private double atomicWeight;
    private double density;
    private double cm2g;
    private List<Kante> kanten;
    private List<Übergang> ubergange;
    private double[] jumps;
    private List<double[]> mcMaster;
    private List<String[]> omega;
    private List<Double> costa;
    private double Emin;
    private double Emax;
    private double step;

    private double[] tauArray;
    private double[] tauCoh ;
    private double[] tauIncoh;
    private double[] müArray;
    private double[] energieArray;
    private List<double[]> ltfArray;

    // Konstruktor, der alles aus Datenauslesen holt
    public Element(String dateipfad, String element, double Emin, double Emax, double step ) {
        Datenauslesen da = new Datenauslesen();
        da.datenerstellen(dateipfad, element); // Datei laden & parsen
        Daten erg = da.getErgebnis();        // alle Werte als Ergebnis-Objekt

        // Zuweisung aller Werte
        this.symbol = erg.getSymbol();
        this.atomicNumber = erg.getAtomicNumber();
        this.atomicWeight = erg.getAtomicWeight();
        this.density = erg.getDensity();
        this.cm2g = erg.getCm2g();
        this.kanten = erg.getKanten();
        this.ubergange = erg.getUbergange();
        this.jumps = erg.getJumps();
        this.mcMaster = erg.getMcMaster();
        this.omega = erg.getOmega();
        this.costa = erg.getCosta();
        this.Emin = Emin <= 0 ? step : Emin;
        this.Emax = Emax;
        this.step= step;

        this.energieArray = init_Energie();
        this.tauArray = massenabsorptionskoeffizientArray();
        this.tauCoh = tauCoh();
        this.tauIncoh = tauIncoh();
        this.müArray = massenschwächungskoeffizientArray();
        this.ltfArray = loecheruebertrag();
    }

    public Daten getErgebnis() {
        return new Daten(
                symbol, atomicNumber, atomicWeight, density, cm2g,
                kanten, ubergange, jumps, mcMaster, omega, costa
        );
    }

    // Getter, falls gewünscht – Beispiel:
    public String getSymbol() { return symbol; }
    public int getAtomicNumber() { return atomicNumber; }
    public double getAtomicWeight() {return atomicWeight;}

    public double getDensity() {return density;}


    public double[] init_Energie() {
        int arrayLaenge = (int) Math.round((Emax - this.Emin) / step) + 1;
        double[] energies = new double[arrayLaenge];
        for (int i = 0; i < arrayLaenge; i++) {
            energies[i] = this.Emin + i * this.step; // Reihenfolge: Emin -> Emax
        }
        return energies;
    }

    private static double tauSchaleEnergieBerechnen(double[] mcMasterParameter, double energie) {
        double summe = 0;
        double logEnergie = Math.log(energie);

        for (int i = 0; i < mcMasterParameter.length; i++) {
            summe += mcMasterParameter[i] * Math.pow(logEnergie, i);
        }

        return Math.exp(summe);
    }

    private double jump(String shell, double energie) {
        double jumpResult = 1.0;

        for (Kante kante : this.kanten) {
            //System.out.println(kante);
            String schaleName = kante.getShell().name(); // Enum zu String
            if (schaleName.startsWith(shell) && energie < kante.getEnergy()) {
                jumpResult *= kante.getJump();
            }
        }

        return 1.0 / jumpResult;
    }

    public double[] massenabsorptionskoeffizientArray() {
        int arrayLaenge = (int) Math.round((Emax - Emin) / step) + 1;
        double[] werte = new double[arrayLaenge];
        for (int i = 0; i < arrayLaenge; i++) {
            double energie = Emax - i * step;
            double wert;
            if (energie >= kanten.get(0).getEnergy()) {
                wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(0), energie) * jump("K", energie);
            } else if (energie >= kanten.get(3).getEnergy()) {
                wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(1), energie) * jump("L", energie);
            } else if (energie >= kanten.get(8).getEnergy()) {
                wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(2), energie) * jump("M", energie);
            } else {
                boolean allZero = true;
                for (double val : mcMaster.get(3)) {
                    if (val != 0) {
                        allZero = false;
                        break;
                    }
                }
                if (allZero) {
                    wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(2), energie) * jump("M", energie);
                } else {
                    wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(3), energie) * jump("N", energie);
                }
            }
            werte[i] = wert;
        }
        // Array umdrehen:
        for (int i = 0; i < arrayLaenge / 2; i++) {
            double tmp = werte[i];
            werte[i] = werte[arrayLaenge - 1 - i];
            werte[arrayLaenge - 1 - i] = tmp;
        }
        return werte;
    }


    private double[] tauCoh() {
        double[] arr = new double[energieArray.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(4), energieArray[i]); // Dummy
        }
        return arr;
    }


    private double[] tauIncoh() {
        double[] arr = new double[energieArray.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(5), energieArray[i]); // Dummy
        }
        return arr;
    }


    public double[] massenschwächungskoeffizientArray() {
        int size = tauArray.length; // alle Arrays haben dieselbe Länge!
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = tauArray[i] + tauCoh[i] + tauIncoh[i];
        }
        return result;
    }


    public double massenabsorptionskoeffizientEnergie(double energie) {
        double wert;
        if (energie >= kanten.get(0).getEnergy()) {
            wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(0), energie) * jump("K", energie);
        } else if (energie >= kanten.get(3).getEnergy()) {
            wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(1), energie) * jump("L", energie);
        } else if (energie >= kanten.get(8).getEnergy()) {
            wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(2), energie) * jump("M", energie);
        } else {
            boolean allZero = true;
            for (double val : mcMaster.get(3)) {
                if (val != 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(2), energie) * jump("M", energie);
            } else {
                wert = cm2g * tauSchaleEnergieBerechnen(mcMaster.get(3), energie) * jump("N", energie);
            }
        }
        return wert;
    }

    public double  massenschwächungskoeffizientEnergie(double energie){
        double tau = massenabsorptionskoeffizientEnergie(energie);
        tau += cm2g * tauSchaleEnergieBerechnen(mcMaster.get(4), energie);
        tau += cm2g * tauSchaleEnergieBerechnen(mcMaster.get(5), energie);
        return tau;
    }

    public double s_ij(String kante, double energie) {
        kante = kante.replace(" ", "");
        double SK = jumps[0];
        double SL1 = jumps[1];
        double SL2 = jumps[2];
        double SL3 = jumps[3];

        double KK  = kanten.get(0).getEnergy();
        double KL1 = kanten.get(1).getEnergy();
        double KL2 = kanten.get(2).getEnergy();
        double KL3 = kanten.get(3).getEnergy();

        if (energie >= KK) {
            switch (kante) {
                case "K":
                    return (SK - 1) / SK;
                case "L1":
                    return (SL1 - 1) / (SK * SL1);
                case "L2":
                    return (SL2 - 1) / (SK * SL1 * SL2);
                case "L3":
                    return (SL3 - 1) / (SK * SL1 * SL2 * SL3);
            }
        } else if (energie >= KL1) {
            switch (kante) {
                case "L1":
                    return (SL1 - 1) / SL1;
                case "L2":
                    return (SL2 - 1) / (SL1 * SL2);
                case "L3":
                    return (SL3 - 1) / (SL1 * SL2 * SL3);
            }
        } else if (energie >= KL2) {
            switch (kante) {
                case "L2":
                    return (SL2 - 1) / SL2;
                case "L3":
                    return (SL3 - 1) / (SL2 * SL3);
            }
        } else if (energie >= KL3) {
            if (kante.equals("L3")) {
                return (SL3 - 1) / SL3;
            }
        }
        return 0;
    }

    public double loecheruebertragL3Energie(double energie) {
        if (energie <= kanten.get(0).getEnergy() &&
                energie >= kanten.get(3).getEnergy() &&
                atomicNumber >= 28) {
            double sL2 = s_ij("L2", energie);
            double sL3 = s_ij("L3", energie);
            double sL1 = s_ij("L1", energie);
            double wert = 1
                    + (sL2 / sL3) * costa.get(4)
                    + (sL1 / sL3) * (costa.get(2) + costa.get(3) + costa.get(1) * costa.get(4));
            return wert;
        } else {
            return 1;
        }
    }
    public double loecheruebertragL2Energie(double energie) {
        if (energie <= kanten.get(0).getEnergy() &&
                energie >= kanten.get(2).getEnergy() &&
                atomicNumber >= 28) {
            double sL1 = s_ij("L1", energie);
            double sL2 = s_ij("L2", energie);

            // costa: List<Double>
            double retval = 1 + (sL1 / sL2) * costa.get(1);
            return retval;
        } else {
            return 1;
        }
    }

    public List<double[]> loecheruebertrag() {
        int arrayLaenge = (int) Math.round((Emax - Emin) / step) + 1;
        List<double[]> retList = new ArrayList<>();

        for (int i = 0; i < arrayLaenge; i++) {
            double energie = Emin + i * step;

            double l2Wert = loecheruebertragL2Energie(energie);
            double l3Wert = loecheruebertragL3Energie(energie);

            // Werte: [Energie, 1, 1, L2, L3]
            retList.add(new double[]{energie, 1.0, 1.0, l2Wert, l3Wert});
        }
        return formatumwandlung4(retList);
    }
    public List<double[]> formatumwandlung4(List<double[]> retList) {
        int n = retList.size();
        double[] energieArr = new double[n];
        double[] kArr = new double[n];
        double[] l1Arr = new double[n];
        double[] l2Arr = new double[n];
        double[] l3Arr = new double[n];
        for (int i = 0; i < n; i++) {
            double[] row = retList.get(i);
            energieArr[i] = row[0];
            kArr[i]      = row[1];
            l1Arr[i]     = row[2];
            l2Arr[i]     = row[3];
            l3Arr[i]     = row[4];
        }
        List<double[]> result = new ArrayList<>();
        result.add(energieArr);
        result.add(kArr);
        result.add(l1Arr);
        result.add(l2Arr);
        result.add(l3Arr);
        return result;
    }

    public double kGemittelUbergang() {
        double faktor = 0.0;
        double ubergangsenergie = 0.0;
        for (Übergang i : ubergange) {

            String schale_von = i.getSchale_von().name();
            String schale_zu = i.getSchale_zu().name();
            if (schale_von.startsWith("K") && schale_zu.startsWith("L")) {
                ubergangsenergie += i.getEnergy() * i.getRate();
                faktor += i.getRate();
            }
        }
        return faktor == 0 ? 0 : ubergangsenergie / faktor;
    }

    public double lGemittelUbergang() {
        double faktor = 0.0;
        double ubergangsenergie = 0.0;
        for (Übergang i : ubergange) {
            String schale_von = i.getSchale_von().name(); // z.B. "L3"
            String schale_zu = i.getSchale_zu().name();   // z.B. "M4" oder "M5"
            if (schale_von.startsWith("L3") &&
                    (schale_zu.startsWith("M4") || schale_zu.startsWith("M5"))) {
                ubergangsenergie += i.getEnergy() * i.getRate();
                faktor += i.getRate();
            }
        }
        return faktor == 0 ? 0 : ubergangsenergie / faktor;

    }

    public double[] getEnergieArray() { return energieArray; }
    public double[] getTauArray()     { return tauArray; }
    public double[] getTauCoh()       { return tauCoh; }
    public double[] getTauIncoh()     { return tauIncoh; }
    public double[] getMüArray()      { return müArray; }
    public  List<Kante> getKanten()   { return kanten; }
    public  List<Übergang> getÜbergänge()   { return ubergange; }
    public List<String[]> getOmega()    {return omega;}
    public List<double[]> getLtf(){return ltfArray;}



    /*
     */

    // Testmain
    public static void main(String[] args) {
        // Beispiel: Erzeuge ein Element-Objekt und gib Daten aus
        Element element = new Element("McMaster.txt", "56", 5, 10, 1);

        // Löcherübertrag aufrufen
        List<double[]> result = element.loecheruebertrag();

        // Arrays ausgeben
        System.out.println("Energie: " + Arrays.toString(result.get(0)));
        System.out.println("K:      " + Arrays.toString(result.get(1)));
        System.out.println("L1:     " + Arrays.toString(result.get(2)));
        System.out.println("L2:     " + Arrays.toString(result.get(3)));
        System.out.println("L3:     " + Arrays.toString(result.get(4)));
        System.out.println(element.kGemittelUbergang());
        System.out.println(element.lGemittelUbergang());
        double[] energieArray = element.getEnergieArray();
        double[] müArray = element.getMüArray();

        System.out.println("Energie      Mü");
        for (int i = 0; i < energieArray.length; i++) {
            System.out.printf("%8.3f  %12.6f\n", energieArray[i], müArray[i]);
        }
        System.out.println(element.massenschwächungskoeffizientEnergie(24.44));





        /*


        Element fe = new Element("McMaster.txt", "Zn",-11,2,0.1);
        //System.out.println(müArray);
        System.out.println(fe.massenabsorptionskoeffizientArray());
        for (double[] eintrag : fe.massenabsorptionskoeffizientArray()) {
            System.out.println(Arrays.toString(eintrag));


        }
        List<double[]> müArray = fe.massenschwächungskoeffizientArray();
        for (double[] zeile : müArray) {
            System.out.println(Arrays.toString(zeile));
        }
        System.out.println(fe.jump("L",1.04));
        System.out.println("Symbol: " + fe.getSymbol());
        System.out.println("Atomnummer: " + fe.getAtomicNumber());
        System.out.println(fe.masschenschwächungskoeffizient(25));
        for (double zeile : fe.masschenschwächungskoeffizient(25)) {
            System.out.println((zeile));
        }

         */

    }
}
