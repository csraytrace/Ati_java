package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class GUItest {

    private static String symbolFromZ(int z) {
        Elementsymbole[] vals = Elementsymbole.values();
        if (z < 1 || z > vals.length) throw new IllegalArgumentException("Ungültiges Z: " + z);
        return vals[z - 1].name();
    }

    public static void main(String[] args) throws Exception {

        // =========================
        //  Deine Messwerte (Z, KL=1 -> K, E (nur Info), Intensität)
        // =========================
        record Row(int Z, int KL, double E, double I) {}

        List<Row> rows = List.of(
                new Row(26, 1, 6.399,   13450.0),
                new Row(28, 1, 7.472,    1753.0),
                new Row(29, 1, 8.041, 1123268.0),
                new Row(30, 1, 8.631,  630929.0)
        );

        // K hat Vorrang (hier eh nur K); 0-Intensitäten würden übersprungen
        Map<String, Integer> elementInt = new LinkedHashMap<>();
        Map<String, String>  elementLine = new LinkedHashMap<>();
        for (Row r : rows) {
            if (r.I == 0.0) continue;
            String sym = symbolFromZ(r.Z);
            String tr  = (r.KL == 1 ? "K" : "L");
            if (!elementLine.containsKey(sym)) {
                elementLine.put(sym, tr);
                elementInt.put(sym, (int)Math.round(r.I));
            } else if ("K".equals(tr) && !"K".equals(elementLine.get(sym))) {
                elementLine.put(sym, "K");
                elementInt.put(sym, (int)Math.round(r.I));
            }
        }

        List<String> elementSymbole = new ArrayList<>(elementInt.keySet());
        List<Integer> intens       = elementSymbole.stream().map(elementInt::get).collect(Collectors.toList());
        List<String>  whichLine    = elementSymbole.stream().map(elementLine::get).collect(Collectors.toList());

        // =========================
        //  Deine Einstellungen (aus JSON)
        // =========================
        String dateiPfad = "MCMASTER.TXT";   // dataSource="McMaster"
        String roehrentyp = "widerschwinger"; // TubeModel: "Wiederschwinger"
        String roehrenMat = "Rh";

        double alpha  = 20.325;
        double beta   = 69.675;
        double fensterW = 0.0;

        double charZuCont  = 0.816;
        double charZuContL = 0.1;
        double sigma       = 0.8;

        String rFenstMat   = "Be";
        double rFenstD_um  = 125.0;

        double xRayTubeVoltage = 40.0; // -> Emax
        double step            = 0.01;
        double Emin            = 0.0;
        double Emax            = xRayTubeVoltage;

        double raumwinkel = 1.0;
        double I_A        = 0.01;   // Strom: Annahme wie in deinen Beispielen (A)
        double messzeit   = 30.0;   // Seconds (nicht in JSON; wie besprochen)

        String dFenstMat  = "Be";
        double dFenst_um  = 7.62;
        double phiDet     = 0.0;
        String kontaktMat = "Au";
        double kontakt_nm = 24.838;
        double bedeck     = 1.0;
        double palpha     = 45.0;   // (falls du UI-Felder hast, dort hernehmen)
        double pbeta      = 45.0;
        String detMat     = "Si";
        double tots_um    = 0.0;
        double act_mm     = 2.513;

        // Keine Material-Filter (laut JSON)
        List<Verbindung> tubeFilters = null;
        List<Verbindung> detFilters  = null;

        // =========================
        //  1) Ohne Dark: CalcI
        // =========================
        System.out.println("=== OHNE Dark (CalcI) ===");

        Probe probe = new Probe(elementSymbole, dateiPfad, Emin, Emax, step, intens);
        for (int i = 0; i < whichLine.size(); i++) {
            if ("K".equalsIgnoreCase(whichLine.get(i))) probe.setzeUebergangAktivFuerElementKAlpha(i);
            else                                        probe.setzeUebergangAktivFuerElementLAlpha(i);
        }

        CalcI calc = new CalcI(
                dateiPfad, probe, roehrentyp, roehrenMat,
                alpha, beta, fensterW,
                sigma, charZuContL,
                rFenstMat, rFenstD_um, raumwinkel, I_A,
                Emin, Emax, step, messzeit, charZuCont,
                dFenstMat, dFenst_um, phiDet, kontaktMat, kontakt_nm, bedeck, palpha, pbeta,
                detMat, tots_um, act_mm,
                tubeFilters, detFilters
        );

        PreparedValues pv = calc.werteVorbereitenAlle();
        Übergang[][] prim = calc.primaerintensitaetBerechnen(pv);

        for (int i = 0; i < prim.length; i++) {
            for (int j = 0; j < prim[i].length; j++) {
                Übergang u = prim[i][j];
                if (u != null && u.isAktiv() && u.getEnergy() != 0.0) {
                    System.out.printf(java.util.Locale.US,
                            "Elem %s: %s→%s  E=%.4f keV  I=%g%n",
                            elementSymbole.get(i), u.getSchale_von().name(), u.getSchale_zu().name(),
                            u.getEnergy(), u.getRate());
                }
            }
        }

        double[] relKonz = calc.berechneRelKonzentrationen(calc, pv, 10000);
        System.out.println("\nRelative Konzentrationen [%]:");
        for (int i = 0; i < relKonz.length; i++) {
            System.out.printf(java.util.Locale.US, "  %s: %.2f%n", elementSymbole.get(i), relKonz[i]);
        }

        // =========================
        //  2) Mit Dark: CalcIDark
        // =========================
        System.out.println("\n=== MIT Dark (CalcIDark) ===");


        // Binder: nicht gesetzt (null) – du hast nichts dafür vorgegeben

        Funktionen f = new FunktionenImpl();


        String ver1 = "1 C38H76N2O2";
        //String ver1 = "luft";

        Verbindung v1 = f.parseVerbindung(ver1, Emin, Emax, step, "McMaster.txt");


        v1.multipliziereKonzentrationen(1.04 / 5.52);
        Verbindung binder = v1;


// Dark-Element(e) ergänzen – hier: O dazu (mit Intensität 0)
        List<String> darkElems = new ArrayList<>(elementSymbole);
        List<Integer> darkInts = new ArrayList<>(intens);
        if (!darkElems.contains("O")) {
            darkElems.add(0, "O");
            darkInts.add(0, 0); // O mit 0-Intensität
        }

// ProbeDark mit Mess- + Dark-Elementen
        Probe probeDark = new Probe(darkElems, dateiPfad, Emin, Emax, step, darkInts);
        for (int i = 0; i < darkElems.size(); i++) {
            // hier der Einfachheit halber überall Kα aktiv
            probeDark.setzeUebergangAktivFuerElementKAlpha(i);
        }



        CalcIDark calcDark = new CalcIDark(
                dateiPfad, probeDark, roehrentyp, roehrenMat,
                alpha, beta, fensterW,
                sigma, charZuContL,
                rFenstMat, rFenstD_um, raumwinkel, I_A,
                Emin, Emax, step, messzeit, charZuCont,
                dFenstMat, dFenst_um, phiDet, kontaktMat, kontakt_nm, bedeck, palpha, pbeta,
                detMat, tots_um, act_mm,
                tubeFilters, detFilters,
                binder
        );

// Dark-Matrix & Z wie in deinem Beispiel
        double[] darkMatrix = new double[]{5.0};
        double Z = 21.47;

        double[] optimum = calcDark.optimizeHIPPARCHUS(Z, darkMatrix);
        System.out.println("optimum: " + java.util.Arrays.toString(optimum));
        calcDark.printOptimizedResult(optimum, darkMatrix, Z);

    }
}
