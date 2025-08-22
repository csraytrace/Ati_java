package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Verbindung {
    private final String[] symbole;
    private double[] konzentrationen;
    private final double Emin;
    private  final double Emax;
    private final double step;
    private final String dateipfad;
    private double dichte;
    private final List<Element> Elementliste;
    private double fensterDickeCm;
    private MathParser modulation;

    public Verbindung(String[] symbole, double[] konzentrationen,  double Emin, double Emax, double step, String dateipfad, double dichte) {
        this.symbole = symbole;
        this.konzentrationen = konzentrationen;
        this.Emin = Emin;
        this.Emax = Emax;
        this.step = step;
        this.dateipfad = dateipfad;
        this.Elementliste = erzeugeElementListe();
        this.dichte = dichte;
        this.fensterDickeCm = 0;
        zuMassenprozent();
        dichteBestimmen();


    }

        public List<Element> erzeugeElementListe() {
            List<Element> elemente = new ArrayList<>();
            for (String symbol : symbole) {
                Element ele = new Element(dateipfad, symbol, Emin, Emax, step);
                elemente.add(ele);
            }
            return elemente;
        }

    public double Z_gemittelt() {
        double Z = 0;
        for (int i = 0; i < Elementliste.size(); i++) {
            Element ele = Elementliste.get(i);
            double konz = konzentrationen[i];
            Z += ele.getAtomicNumber() * konz;
        }
        return Z;
    }

    public List<Double> getZ_List() {
        List<Double> zWerte = new ArrayList<>();
        for (int i = 0; i < Elementliste.size(); i++) {
            Element ele = Elementliste.get(i);
            double z = ele.getAtomicNumber();
            zWerte.add(z);
        }
        return zWerte;
    }



    public void dichteBestimmen() {
        if (dichte == 0) {
            double neueDichte = 0.0;
            for (int i = 0; i < Elementliste.size(); i++) {
                double konz = konzentrationen[i];
                double dichteEl = Elementliste.get(i).getDensity();
                neueDichte += konz * dichteEl;
            }
            this.dichte = neueDichte;
        }
    }

    public void zuMassenprozent() {
        int n = Elementliste.size();
        double[] massen = new double[n];
        double summeMasse = 0.0;
        for (int i = 0; i < n; i++) {
            massen[i] = Elementliste.get(i).getAtomicWeight() * konzentrationen[i];
            summeMasse += massen[i];
        }
        double[] massenProzent = new double[n];
        for (int i = 0; i < n; i++) {
            massenProzent[i] =  massen[i] / summeMasse;
        }
        this.konzentrationen = massenProzent;
    }

    public void zuAtomprozent() {
        int n = Elementliste.size();
        double[] atomAnteile = new double[n];
        double summeAtom = 0.0;
        for (int i = 0; i < n; i++) {
            atomAnteile[i] = konzentrationen[i] / Elementliste.get(i).getAtomicWeight();
            summeAtom += atomAnteile[i];
        }
        for (int i = 0; i < n; i++) {
            atomAnteile[i] /= summeAtom; // Normieren
        }
        this.konzentrationen = atomAnteile;
    }

    public String[] getSymbole() {
        return Elementliste.stream()
                .map(Element::getSymbol)
                .toArray(String[]::new);
    }

    public double[] getKonzentrationen() {
        return konzentrationen;
    }
    public double getEmin() {
        return Emin;
    }
    public double getEmax() {
        return Emax;
    }
    public double getStep() {
        return step;
    }
    public String getDateipfad() {
        return dateipfad;
    }
    public double getDichte(){return dichte;}
    public double [] getEnergieArray(){return Elementliste.get(0).getEnergieArray();}


    private static boolean warningShown = false;

    //!!! VORSICHT: Funktioniert nur bei einer Verbindung und nicht mit Gemisch (1 Al2O3 + 1 TiO2) geht nicht !!!
    public double getWeight() {

        if (Elementliste.size() > 1 && !warningShown) {
            System.err.println("WARNUNG: getWeight() ist nur für eine einzelne Verbindung korrekt! Das Ergebnis ist falsch bei einem Gemisch.");
            warningShown = true;
        }
        // 1. Schritt: Stoffmenge (mol) jedes Elements berechnen
        double[] mol = new double[Elementliste.size()];
        for (int i = 0; i < Elementliste.size(); i++) {
            mol[i] = konzentrationen[i] / Elementliste.get(i).getAtomicWeight();
        }

        // 2. Schritt: Alle Stoffmengen durch die kleinste teilen (Verhältnis)
        double minMol = Arrays.stream(mol).min().orElse(1);
        double[] verhältnis = new double[Elementliste.size()];
        for (int i = 0; i < Elementliste.size(); i++) {
            verhältnis[i] = mol[i] / minMol;
        }

        // 3. Schritt: Skalierungsfaktor finden, der alle Werte möglichst ganz macht
        int faktor = 1;
        boolean fertig = false;
        int maxFaktor = 10;  // Notfalls erhöhen (für ganz krumme Zahlen)
        int[] anzahlen = new int[Elementliste.size()];

        while (!fertig && faktor <= maxFaktor) {
            fertig = true;
            for (int i = 0; i < verhältnis.length; i++) {
                double wert = verhältnis[i] * faktor;
                // Ist nah genug an einer ganzen Zahl?
                if (Math.abs(wert - Math.round(wert)) > 0.05) {
                    fertig = false;
                    break;
                }
            }
            if (!fertig) {
                faktor++;
            }
        }

        // 4. Schritt: Runde und setze die Anzahlen
        for (int i = 0; i < verhältnis.length; i++) {
            anzahlen[i] = (int) Math.round(verhältnis[i] * faktor);
        }

        // 5. Schritt: Molmasse der Summenformel berechnen
        double summenformelGewicht = 0;
        for (int i = 0; i < anzahlen.length; i++) {
            //System.out.println("Atomanzahl"+anzahlen[i]);
        }


        for (int i = 0; i < Elementliste.size(); i++) {
            summenformelGewicht += anzahlen[i] * Elementliste.get(i).getAtomicWeight();
        }
        return summenformelGewicht;
    }




    public List<List<Kante>> erzeugeKantenListe() {
        List<List<Kante>> gesamtKantenListe = new ArrayList<>();

        for (Element element : Elementliste) {
            List<Kante> kantenProElement = new ArrayList<>();
            kantenProElement = element.getKanten();

            gesamtKantenListe.add(kantenProElement);
        }

        return gesamtKantenListe;
    }


    public double[] erzeuge_Kantenenergien() {
        List<Double> kantenEnergienListe = new ArrayList<>();

        for (Element element : Elementliste) {
            List<Kante> kantenProElement = element.getKanten();
            for (Kante kante : kantenProElement) {
                kantenEnergienListe.add(kante.getEnergy());
            }
        }
        // Umwandlung List<Double> → double[]
        double[] kantenEnergien = new double[kantenEnergienListe.size()];
        for (int i = 0; i < kantenEnergienListe.size(); i++) {
            kantenEnergien[i] = kantenEnergienListe.get(i);
        }

        return kantenEnergien;
    }

    public List<List<Übergang>> erzeugeÜbergängeListe() {
        List<List<Übergang>> gesamtÜbergangListe = new ArrayList<>();

        for (Element element : Elementliste) {
            List<Übergang> übergangProElement = new ArrayList<>();
            übergangProElement = element.getÜbergänge();

            gesamtÜbergangListe.add(übergangProElement);
        }

        return gesamtÜbergangListe;
    }

    public List<List<String[]>> erzeugeOmegaListe() {
        List<List<String[]>> gesamtOmegaListe = new ArrayList<>();

        for (Element element : Elementliste) {
            List<String[]> omegaProElement = element.getOmega(); // Typ: List<String[]>
            gesamtOmegaListe.add(omegaProElement);
        }

        return gesamtOmegaListe;
    }

    //Fensterdicken für Filter

    public double getFensterDickeCm() {
        return this.fensterDickeCm;
    }

    public void setFensterDickeCm(double d) {
        this.fensterDickeCm = d;
    }


    /** Setzt die Modulation auf “immer 1” (keine Änderung). */
    public void setModulationIdentitaet() {
        this.modulation = MathParser.withDefault(1.0); // ohne Segmente -> evaluate(x) = 1
    }

    /** Leert/initialisiert die Modulation mit Default-Wert außerhalb aller Intervalle. */
    public void clearModulation(double defaultValue) {
        this.modulation = MathParser.withDefault(defaultValue);
    }

    /** Fügt ein stückweises Segment hinzu: Ausdruck gilt im Intervall [a,b] (inkl/exkl). */
    public void addModulationSegment(String expr, double a, boolean inklA, double b, boolean inklB) {
        if (this.modulation == null) {
            this.modulation = MathParser.withDefault(1.0);
        }
        this.modulation = this.modulation.addExprSegment(expr, a, inklA, b, inklB);
    }



    public double erzeuge_Filter(double energie, double Fensterwinkel){
        double fensterMue = massenschwächungskoeffizientEnergie(energie);
        double fensterEinfallCos = Math.cos(Math.toRadians(Fensterwinkel));

        double transF = Math.exp(-fensterMue * dichte * fensterDickeCm / fensterEinfallCos);
        double faktor = (modulation != null) ? modulation.evaluate(energie) : 1.0;
        return transF * faktor;}

    public double erzeuge_Filter(double energie) {
        return erzeuge_Filter(energie, 0.0);
    }

    public double[] erzeuge_Filter_liste(double Fensterwinkel) {
        double[] energien = Elementliste.get(0).getEnergieArray();
        double[] result = new double[energien.length];
        double fensterEinfallCos = Math.cos(Math.toRadians(Fensterwinkel));
        for (int i = 0; i < energien.length; i++) {
            double energie = energien[i];
            double fensterMue = massenschwächungskoeffizientEnergie(energie);

            //result[i] = Math.exp(-fensterMue * dichte * fensterDickeCm / fensterEinfallCos);
            double transF = Math.exp(-fensterMue * dichte * fensterDickeCm / fensterEinfallCos);
            double faktor = (modulation != null) ? modulation.evaluate(energie) : 1.0;
            result[i] = transF * faktor;
        }
        return result;
    }

    public double[] erzeuge_Filter_liste() {
        return erzeuge_Filter_liste( 0.0);
    }

    public double[] tau_array_sum() {
        if (Elementliste.isEmpty()) return new double[0];
        double[] energien = Elementliste.get(0).getEnergieArray();
        int nEnergie = energien.length;
        double[] tauSum = new double[nEnergie];
        for (int j = 0; j < Elementliste.size(); j++) {
            Element el = Elementliste.get(j);
            double[] tau = el.getTauArray();
            double konz = konzentrationen[j];
            for (int i = 0; i < nEnergie; i++) {
                tauSum[i] += tau[i] * konz;
            }
        }
        return tauSum;
    }

    public double[] mü_array_sum() {
        if (Elementliste.isEmpty()) return new double[0];
        double[] energien = Elementliste.get(0).getEnergieArray();
        int nEnergie = energien.length;
        double[] müSum = new double[nEnergie];
        for (int j = 0; j < Elementliste.size(); j++) {
            Element el = Elementliste.get(j);
            double[] mü = el.getMüArray();
            double konz = konzentrationen[j];
            for (int i = 0; i < nEnergie; i++) {
                müSum[i] += mü[i] * konz;
            }
        }
        //System.out.println(Arrays.toString(müSum));

        return müSum;
    }

    public double massenschwächungskoeffizientEnergie(double energie){

        double Mü = 0;
        for (int j = 0; j < Elementliste.size(); j++) {
            Element el = Elementliste.get(j);
            double mü_x = el.massenschwächungskoeffizientEnergie(energie);
            Mü += mü_x * konzentrationen[j];
        }
        return Mü;
    }

    public double massenabsorptionskoeffizientEnergie(double energie){

        double Tau = 0;
        for (int j = 0; j < Elementliste.size(); j++) {
            Element el = Elementliste.get(j);
            double tau_x = el.massenabsorptionskoeffizientEnergie(energie);
            Tau += tau_x * konzentrationen[j];
        }
        return Tau;
    }


    public void multipliziereKonzentrationen(double faktor) {
        for (int i = 0; i < konzentrationen.length; i++) {
            konzentrationen[i] *= faktor;
        }
    }




}
