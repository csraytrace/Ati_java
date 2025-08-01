package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CalcI {
    // --- Felder ---
    public Probe probe;
    public Detektor detektor;
    public RöhreBasis röhre;
    public double fensterdickeDet;
    public String dateipfad;
    public double palpha;
    public double pbeta;
    public double emin;
    public double emax;
    public double step;
    public double[] konzentration;
    public PreparedValues pv;

    // --- Konstruktor ---
    public CalcI(
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
        this.detektor = new Detektor(fenstermaterialDet, fensterdickeDet, phiDet,
                kontaktmaterial, kontaktmaterialdicke, bedeckungsfaktor,
                detektormaterial, totschicht, activeLayer,
                dateipfad, emin, emax, step, filter_det);

        // Röhrentyp auswählen
        switch (röhrenTyp.toLowerCase()) {
            case "widerschwinger":
                this.röhre = new WiderschwingerRöhre(
                        einfallswinkelalpha, einfallswinkelbeta, fensterwinkel,
                        charzucont, charzucontL, fensterdickeRöhre, raumwinkel, röhrenstrom,
                        emin, emax, sigma, step, messzeit, röhrenmaterial, fenstermaterialRöhre, dateipfad, filter_röhre
                );
                break;
            case "lovescott":
                this.röhre = new LoveScottRöhre(
                        einfallswinkelalpha, einfallswinkelbeta, fensterwinkel,
                        charzucont, charzucontL, fensterdickeRöhre, raumwinkel, röhrenstrom,
                        emin, emax, sigma, step, messzeit, röhrenmaterial, fenstermaterialRöhre, dateipfad, filter_röhre
                );
                break;
            default:
                throw new IllegalArgumentException("Unbekannter Röhrentyp: " + röhrenTyp);
        }

        //this.fensterdickeDet = fensterdickeDet * 1e-4;
        //this.dateipfad = dateipfad;
        this.palpha = Math.cos(Math.PI / 180 * palphaGrad);
        this.pbeta = Math.cos(Math.PI / 180 * pbetaGrad);
        this.emin = (emin != 0) ? emin : step;
        this.emax = emax;
        this.step = step;

        this.probe = probe;
        this.konzentration = probe.getIntensitäten();
        this.pv = werteVorbereitenAlle();
        //System.out.println(charzucont);

    }

    public static final Übergang EMPTY =
            new Übergang(Schale.K, Schale.K, 0.0, 0.0, false);


    public PreparedValues werteVorbereitenAlle() {

        /* ---------- 1. Probe-Liste erzeugen ---------- */
        List<Integer> zNumbers = probe.getElementZNumbers();
        int nEle = zNumbers.size();

        List<Element> elementListe = probe.getElemente();

        List<double[]> alleKanten = new ArrayList<>(nEle);   // jedes Array hat Länge 4
        int mau = 0;                                         // maximale Anzahl Übergänge
        List<double[]> tauList = new ArrayList<>(nEle);
        List<double[]> muList  = new ArrayList<>(nEle);
        List<List<double[]>> ltfList = new ArrayList<>();

        for (int i = 0; i < zNumbers.size(); i++) {

            List<Kante> kanten = elementListe.get(i).getKanten();   // alle Kanten
            double[] kanten4 = new double[Math.min(4, kanten.size())];

            for (int j = 0; j < 4 && j < kanten.size(); j++) {
                kanten4[j] = kanten.get(j).getEnergy();             // Energy kopieren
            }// Größe 4
            alleKanten.add(kanten4);

            mau = Math.max(mau, elementListe.get(i).getÜbergänge().size());
            tauList.add(elementListe.get(i).getTauArray());
            muList.add(elementListe.get(i).getMüArray());
            ltfList.add(elementListe.get(i).getLtf());
        }

        /* ---------- 2. Arrays instanziieren ---------- */
        double[][] alleKantenArr   = new double[nEle][4];
        double[][] tauArr          = new double[nEle][tauList.get(0).length];
        double[][] muArr           = new double[nEle][muList.get(0).length];
        Übergang[][] alleUebergArr = new Übergang[nEle][mau];

        double[][] tube0  = new double[nEle][4];
        double[][] tau0   = new double[nEle][4];
        double[][] omega0 = new double[nEle][4];
        double[][] sij0   = new double[nEle][4];
        double[][][] sij  = new double[nEle][4][tauArr[0].length];

        double[][] det_ijk     = new double[nEle][mau];
        double[][][] mu0       = new double[nEle][nEle][4];
        double[][][] tau_ijk   = new double[nEle][nEle][mau];
        double[][][] mu_ijk    = new double[nEle][nEle][mau];
        double[][][] sij_xyz   = new double[nEle][4][mau * nEle]; // Platzhalter

        /* ---------- 3. Kanten & Übergänge füllen ---------- */
        int idxEle = 0;
        for (Element el : elementListe) {

            /* 3a – Kanten kopieren */
            System.arraycopy(alleKanten.get(idxEle), 0, alleKantenArr[idxEle], 0, 4);

            /* 3b – Übergänge in alleUebergArr  */
            List<Übergang> uebers = el.getÜbergänge();


            for (int j = 0; j < mau; j++) {                 // bis zur Maximalzahl mau durchlaufen
                alleUebergArr[idxEle][j] =
                        (j < uebers.size())                 // gibt es an dieser Position einen echten?
                                ? uebers.get(j)                     // → ja: einsetzen
                                : EMPTY;                   // → nein: Platzhalter
            }


            /* 3c – Tau / Mu / Ltf in Arrays kopieren */
            System.arraycopy(tauList.get(idxEle), 0, tauArr[idxEle], 0, tauArr[0].length);
            System.arraycopy(muList .get(idxEle), 0, muArr [idxEle], 0, muArr [0].length);

            idxEle++;
        }

        /* ---------- 4. Schleifen---------- */
        for (int i = 0; i < nEle; i++) {
            Element el = elementListe.get(i);
            double[] kanten = alleKantenArr[i];
            for (int kIndex = 0; kIndex < 4; kIndex++) {

                double e = kanten[kIndex] == 0 ? 1e-6 : kanten[kIndex];
                double stepDelta = (Math.floor(e / step) + 1) * step - e;

                tube0 [i][kIndex] = röhre.getCountRateChar(e, stepDelta)
                        + röhre.getCountRateCont(e, stepDelta);
                tau0  [i][kIndex] = el.massenabsorptionskoeffizientEnergie(e);
                sij0  [i][kIndex] = el.s_ij(el.getKanten().get(kIndex).getShell().name(), e);
                omega0[i][kIndex] = RöhreBasis.getOmegaValue(el.getOmega(),el.getKanten().get(kIndex).getShell().name() );

                // Sij über Energiesweep
                for (int t = 0; t < tauArr[0].length; t++) {
                    double eSweep = (t + (int) (emin / step)) * step;
                    sij[i][kIndex][t] =
                            el.s_ij(el.getKanten().get(kIndex).getShell().name(), eSweep);
                }
            }
        }

        /* ---------- 5. Mü0 – dreidimensional ---------- */
        for (int i = 0; i < nEle; i++) {
            Element elI = elementListe.get(i);
            for (int j = 0; j < nEle; j++) {
                for (int k = 0; k < 4; k++) {
                    double e = alleKantenArr[j][k];
                    mu0[i][j][k] = (e == 0)
                            ? 0.0
                            : elI.massenschwächungskoeffizientEnergie(e);
                }
            }
        }

        /* ---------- 6. Det_ijk, Tau_ijk, Mu_ijk etc. ---------- */
        double[] alleUebergEnergien = new double[nEle * mau];
        int ptr = 0;
        for (int i = 0; i < nEle; i++) {
            for (int j = 0; j < mau; j++) {
                double eUe = alleUebergArr[i][j].getEnergy();
                alleUebergEnergien[ptr++] = eUe;
                if (eUe != 0) {
                    det_ijk[i][j] = detektor.detektoreffizienz(eUe);
                }
            }
        }

        for (int i = 0; i < nEle; i++) {
            Element el = elementListe.get(i);
            for (int j = 0; j < nEle; j++) {
                for (int k = 0; k < mau; k++) {
                    double eUe = alleUebergArr[j][k].getEnergy();
                    if (eUe != 0) {
                        tau_ijk[i][j][k] = el.massenabsorptionskoeffizientEnergie(eUe);
                        mu_ijk [i][j][k] = el.massenschwächungskoeffizientEnergie(eUe);
                    }
                }
            }
        }

        /* ---------- 7. Countrate + Sij_xyz ---------- */
        double[] countrate = röhre.getGesamtspektrum();

        //int idx = 0;
        for (int i = 0; i < nEle; i++) {
            Element el = elementListe.get(i);
            for (String kante : List.of(" K", "L1", "L2", "L3")) {
                int idx = 0;  // <-- Wichtig! Hier setzen!
                for (double eUe : alleUebergEnergien) {
                    sij_xyz[i][kanteIndex(kante)][idx] = el.s_ij(kante, eUe);
                    idx++;
                }
            }
        }

        /* ---------- 8. Konzentrationen normalisieren ---------- */
        double sum = Arrays.stream(konzentration).sum();
        double[] konzNorm = Arrays.stream(konzentration)
                .map(c -> c / sum)
                .toArray();



        double[] ltfFlat = ltfList.stream()          // Stream<List<double[]>>
                .flatMap(List::stream)               // Stream<double[]>
                .flatMapToDouble(Arrays::stream)     // DoubleStream
                .toArray();

        /* ---------- 9. Record zurückgeben ---------- */
        return new PreparedValues(
                palpha, pbeta,  tube0, tau0, omega0, mu0, sij0,
                tauArr, muArr,  countrate, mu_ijk, det_ijk, sij,
                alleKantenArr, alleUebergArr, sij_xyz, tau_ijk,
                konzNorm,      step,   emin,
                ltfFlat
        );
    }

    /* Hilfsroutine: " K"→0, "L1"→1, … */
    private static int kanteIndex(String kante) {
        return switch (kante) { case " K" -> 0; case "L1" -> 1; case "L2" -> 2; case "L3" -> 3; default -> -1; };
    }


    public Übergang[][] primaerintensitaetBerechnen(PreparedValues pv) {

        /* -- Kürzel für schnelleres Tippen ----------------------------------- */
        double   Pα   = pv.palpha();
        double   Pβ   = pv.pbeta();
        double[][]  Tube0   = pv.tube0();
        double[][]  Tau0    = pv.tau0();
        double[][]  Ω0      = pv.omega0();
        double[][][] μ0     = pv.mu0();
        double[][]   Σij0   = pv.sij0();
        double[][] Tau    = pv.tau();
        double[][][] Σij    = pv.sij();
        double[][]   μ      = pv.mu();
        double[]     Cnt    = pv.countrate();
        double[][][] μ_ijk  = pv.mu_ijk();
        double[][]   Det    = pv.det_ijk();
        Übergang[][] Ü      = pv.alleUeberg();
        double[][][] Σij_xyz= pv.sij_xyz();
        double[][][] Tau_ijk= pv.tau_ijk();
        double[]     Kz     = pv.konzNorm();          // Konzentrationen
        double[][]   Kanten = pv.alleKanten();
        double[]     LtfFlat= pv.ltf();               // 1-D → gleich rekonst.
        double    energy=0.0;
        boolean aktiv = false;

        final int nEle = Ü.length;
        final int mau  = Ü[0].length;
        Übergang[][] ret = new Übergang[nEle][mau];

        /* -------- Ltf wieder in [nEle][5][TauLen] formen -------------------- */
        final int tauLen = Tau[0].length;
        double[][][] Ltf = new double[nEle][5][tauLen];
        int p = 0;
        for (int i = 0; i < nEle; i++)
            for (int s = 0; s < 5; s++)
                for (int t = 0; t < tauLen; t++)
                    Ltf[i][s][t] = LtfFlat[p++];

        /* ---------------------- Konstanten ---------------------------------- */
        final double CONST = (1 / Pα) * 30.0e-2 / (4 * Math.PI * 0.5 * 0.5);

        /* --------- Summen µ_add, µ0_add, µijk_add --------------------------- */
        double[][]   μ_add      = new double[tauLen][];
        double[][][] μ0_add     = new double[nEle][4][];
        double[][]   μijk_add   = new double[nEle][mau];
        for (int k = 0; k < tauLen; k++)
            μ_add[k] = new double[]{0.0};              // Dummy‐Init, füllen unten

        /* Initialisieren auf 0 */
        double[][]   μAdd      = new double[tauLen][1];
        double[][] μ0Add = new double[nEle][4];
        double[][]   μijkAdd   = new double[nEle][mau];

// μ_add: tauLen
        for (int k = 0; k < tauLen; k++)
            for (int i = 0; i < nEle; i++)
                μAdd[k][0] += Kz[i] * μ[i][k];

// μ0Add: [nEle][4]
        for (int j = 0; j < nEle; j++)
            for (int s = 0; s < 4; s++)
                for (int i = 0; i < nEle; i++)
                    μ0Add[j][s] += Kz[i] * μ0[i][j][s];

// μijkAdd: [nEle][mau]
        for (int j = 0; j < nEle; j++)
            for (int k = 0; k < mau; k++)
                for (int i = 0; i < nEle; i++)
                    μijkAdd[j][k] += Kz[i] * μ_ijk[i][j][k];

        /* ---------------- Hauptschleifen ------------------------------------ */
        for (int i = 0; i < nEle; i++) {

            // Skip, wenn Element gar keine Übergänge
            if (Ü[i][0].getEnergy() == 0.0) continue;


            for (int j = 0; j < mau; j++) {                 // maximale Anzahl Übergänge

                Übergang u = Ü[i][j];
                if (u.getEnergy() == 0.0) continue;            // Platzhalter überspringen

                String ijk = u.getSchale_von().name();
                Schale Schale_zu = (Schale) u.getSchale_zu();
                Schale Schale_von = (Schale) u.getSchale_von();
                double ω_ij = u.getRate();                     // Übergangswahrsch.
                energy = u.getEnergy();
                aktiv = u.isAktiv();

                /* --- Kantenindex bestimmen --------------------------------- */
                int kanteIdx;
                if      (ijk == "K") kanteIdx = 0;
                else if (ijk == "L1") kanteIdx = 1;
                else if (ijk == "L2") kanteIdx = 2;
                else                 kanteIdx = 3;

                /* --- erster Summand (Char-Linie + Kontinuum an Kante) ------- */
                double num1 = Tube0[i][kanteIdx]      * Kz[i]
                        * Tau0[i][kanteIdx]       * Σij0[i][kanteIdx]
                        * Ω0[i][kanteIdx]         * ω_ij;

                double den1 = μ0Add[i][kanteIdx] / Pα
                        + μijkAdd[i][j]          / Pβ;

                double dummy = num1 / den1;

                /* --- Schleife über weitere Energien ab Kantenenergie -------- */
                int startK = (int) ((Kanten[i][kanteIdx] / pv.step())
                        - (pv.emin() / pv.step()) + 1);

                for (int k = startK; k < tauLen; k++) {

                    double num2 = Cnt[k] * Kz[i]             * Tau[i][k]
                            * Σij[i][kanteIdx][k]         * Ω0[i][kanteIdx]
                            * ω_ij * Ltf[i][kanteIdx + 1][k];

                    double den2 = μAdd[k][0] / Pα
                            + μijkAdd[i][j] / Pβ;

                    dummy += num2 / den2;
                }

                /* --- Detektor-Effizienz, Konstante, record ------------------ */
                dummy *= Det[i][j] * CONST;



                ret[i][j] = new Übergang(Schale_von,Schale_zu,energy,dummy,aktiv);
            }
        }
        return ret;
    }




    public Übergang[][] sekundaerintensitaetBerechnen(PreparedValues pv) {

        double   Pα   = pv.palpha();
        double   Pβ   = pv.pbeta();
        double[][]  Tube0   = pv.tube0();
        double[][]  Tau0    = pv.tau0();
        double[][]  Ω0      = pv.omega0();
        double[][][] μ0     = pv.mu0();
        double[][] sij0 = pv.sij0();
        double[][] Tau    = pv.tau();
        double[][][] sij    = pv.sij();
        double[][]   μ      = pv.mu();
        double[]     Cnt    = pv.countrate();
        double[][][] μ_ijk  = pv.mu_ijk();
        double[][]   Det    = pv.det_ijk();
        Übergang[][] Ü      = pv.alleUeberg();
        double[][][] sij_xyz= pv.sij_xyz();
        double[][][] Tau_ijk= pv.tau_ijk();
        double[]     Kz     = pv.konzNorm();
        double[][]   Kanten = pv.alleKanten();
        double[]     LtfFlat= pv.ltf();
        boolean aktiv = false;



        final int nEle = Ü.length;
        final int mau  = Ü[0].length;
        Übergang[][] ret = new Übergang[nEle][mau];

        // Ltf wiederherstellen
        final int tauLen = Tau[0].length;
        double[][][] Ltf = new double[nEle][5][tauLen];
        int p = 0;
        for (int i = 0; i < nEle; i++)
            for (int s = 0; s < 5; s++)
                for (int t = 0; t < tauLen; t++)
                    Ltf[i][s][t] = LtfFlat[p++];

        // Konstanten
        final double CONST = (1 / Pα) * 30.0e-2 / (4 * Math.PI * 0.5 * 0.5);

        // Aufsummierungen wie vorher
        double[][]   μAdd      = new double[tauLen][1];
        double[][]   μ0Add = new double[nEle][4];
        double[][]   μijkAdd   = new double[nEle][mau];
        for (int k = 0; k < tauLen; k++)
            for (int i = 0; i < nEle; i++)
                μAdd[k][0] += Kz[i] * μ[i][k];

        for (int j = 0; j < nEle; j++)
            for (int s = 0; s < 4; s++)
                for (int i = 0; i < nEle; i++)
                    μ0Add[j][s] += Kz[i] * μ0[i][j][s];

        for (int j = 0; j < nEle; j++)
            for (int k = 0; k < mau; k++)
                for (int i = 0; i < nEle; i++)
                    μijkAdd[j][k] += Kz[i] * μ_ijk[i][j][k];

        // Hauptschleifen
        for (int i = 0; i < nEle; i++) {
            if (Ü[i][0].getEnergy() == 0.0) continue;

            for (int index_ijk = 0; index_ijk < mau; index_ijk++) {

                Übergang u_ijk = Ü[i][index_ijk];
                String ijk = u_ijk.getSchale_von().name();
                Schale Schale_von = (Schale) u_ijk.getSchale_von();
                Schale Schale_zu = (Schale) u_ijk.getSchale_zu();
                aktiv = u_ijk.isAktiv();
                double ω_ij = u_ijk.getRate();
                double Ubergangsenergie_ijk = u_ijk.getEnergy();
                double countrate = 0.0;

                // --- Kantenindex bestimmen wie zuvor ---
                int Kante_ij;
                if      (ijk.equals("K"))   Kante_ij = 0;
                else if (ijk.equals("L1"))  Kante_ij = 1;
                else if (ijk.equals("L2"))  Kante_ij = 2;
                else                        Kante_ij = 3;

                if (Ubergangsenergie_ijk != 0.0) {

                    for (int j = 0; j < nEle; j++) {
                        if (Ü[j][0].getEnergy() == 0.0) continue;

                        for (int index_xyz = 0; index_xyz < mau; index_xyz++) {

                            Übergang u_xyz = Ü[j][index_xyz];
                            String xyz_str = u_xyz.getSchale_von().name();
                            int Kante_xy;
                            if      (xyz_str.equals("K"))   Kante_xy = 0;
                            else if (xyz_str.equals("L1"))  Kante_xy = 1;
                            else if (xyz_str.equals("L2"))  Kante_xy = 2;
                            else                            Kante_xy = 3;

                            //double xyz = str_zu_zahl(xyz_str); // Oder benutze direkt getCode(), falls du eine eigene Methode hast!
                            double Ubergangsenergie_xyz = u_xyz.getEnergy();
                            double Ubergangswhs_xyz = u_xyz.getRate();

                            //if (xyz != 0 && (Kanten[i][Kante_ij] < Ubergangsenergie_xyz)) {
                            if ((Kanten[i][Kante_ij] < Ubergangsenergie_xyz)) {

                                double integral = Pβ / μijkAdd[i][index_ijk] * Math.log(1 + μijkAdd[i][index_ijk] / (Pβ * μijkAdd[j][index_xyz]));
                                integral += (Pα / μ0Add[j][Kante_xy]) * Math.log(1 + μ0Add[j][Kante_xy] / (Pα * μijkAdd[j][index_xyz]));
                                integral *= sij0[j][Kante_xy] * Tube0[j][Kante_xy] * Ω0[j][Kante_xy] * Ubergangswhs_xyz * Tau0[j][Kante_xy] /
                                        ((μ0Add[j][Kante_xy] / Pα) + (μijkAdd[i][index_ijk] / Pβ));


                                double integralconst = Pβ / μijkAdd[i][index_ijk] * Math.log(1 + μijkAdd[i][index_ijk] / (Pβ * μijkAdd[j][index_xyz]));


                                int startK = (int) ((Kanten[j][Kante_xy] / pv.step()) - (pv.emin() / pv.step()) + 1);
                                for (int k = startK; k < tauLen; k++) {
                                    double dummy = integralconst + (Pα / μAdd[k][0]) * Math.log(1 + μAdd[k][0] / (Pα * μijkAdd[j][index_xyz]));
                                    dummy *= sij[j][Kante_xy][k] * Tau[j][k] * Ltf[j][Kante_xy + 1][k] * Ω0[j][Kante_xy] * Ubergangswhs_xyz * Cnt[k] /
                                            ((μAdd[k][0] / Pα) + (μijkAdd[i][index_ijk] / Pβ));
                                    integral += dummy;
                                }

                                integral *= CONST * sij_xyz[i][Kante_ij][index_xyz + j * mau]
                                        * Ω0[i][Kante_ij] * ω_ij * Det[i][index_ijk] * 0.5
                                        * Tau_ijk[i][j][index_xyz] * Kz[i] * Kz[j];

                                countrate += integral;

                            }
                        }
                    }
                }

                // Rückgabe-Objekt wie gehabt
                ret[i][index_ijk] = new Übergang(
                        Schale_von,
                        Schale_zu,
                        Ubergangsenergie_ijk,
                        countrate,
                        aktiv
                );
            }
        }
        return ret;
    }




    /**
     * Gibt ein Übergang-Array zurück, bei dem die Intensität (getRate) aus Primär und Sekundär addiert ist.
     * Alle anderen Felder (Schale_von, Schale_zu, Energy, Aktiv) werden wie im Primär-Feld gesetzt.
     */
    public static Übergang[][] addiereIntensitäten(Übergang[][] primaer, Übergang[][] sekundaer) {

        int n = primaer.length;
        int m = primaer[0].length;
        Übergang[][] result = new Übergang[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Übergang uPrim = primaer[i][j];
                Übergang uSek = sekundaer[i][j];

                if (uPrim == null) {
                    result[i][j] = uPrim; // oder null, wie du magst
                    continue;
                }

                // Addiere die Intensitäten (kann bei Sekundär null sein!)
                double intensSumme = uPrim.getRate();
                if (uSek != null) {
                    intensSumme += uSek.getRate();
                }

                // Neuer Übergang, alle Felder gleich wie Primär, außer Rate = Summe
                result[i][j] = new Übergang(
                        (Schale) uPrim.getSchale_von(),
                        (Schale) uPrim.getSchale_zu(),
                        uPrim.getEnergy(),
                        intensSumme,
                        uPrim.isAktiv() // oder beliebige Logik für Aktiv-Status
                );
            }
        }
        return result;
    }

    public static double[] berechneSummenintensitaetProElement(
            Übergang[][] primaer,
            Übergang[][] sekundaer
    ) {
        int nElem = primaer.length;
        double[] summen = new double[nElem];

        for (int i = 0; i < nElem; i++) {
            double sum = 0.0;
            for (int j = 0; j < primaer[i].length; j++) {
                Übergang p = primaer[i][j];
                Übergang s = sekundaer[i][j];

                // Es wird angenommen, dass die Übergänge in beiden Arrays an gleicher Stelle sind!
                if (p == null || !p.isAktiv()) continue; // Nur aktive zählen!

                double intens = 0.0;
                if (p != null) intens += p.getRate();
                if (s != null) intens += s.getRate();
                sum += intens;
            }
            // Sicherstellen, dass nicht 0 (sonst Division durch Null in Relativanteil)
            summen[i] = (sum == 0.0) ? 1.0 : sum;
        }
        return summen;
    }


    public double[] berechneSummenintensitaetMitKonz(double[] neueKonz, double geo) {
        // Neues PreparedValues mit den neuen Konzentrationen
        PreparedValues pvNeu = mitNeuerKonzentration(this.pv, neueKonz);

        Übergang[][] primaer = this.primaerintensitaetBerechnen(pvNeu);
        Übergang[][] sekundaer = this.sekundaerintensitaetBerechnen(pvNeu);

        double[] summen = new double[primaer.length];
        for (int i = 0; i < primaer.length; i++) {
            double sum = 0.0;
            for (int j = 0; j < primaer[i].length; j++) {
                Übergang p = primaer[i][j];
                Übergang s = sekundaer[i][j];
                if (p == null || !p.isAktiv()) continue;
                double intens = 0.0;
                if (p != null) intens += p.getRate();
                if (s != null) intens += s.getRate();
                sum += intens;
            }
            summen[i] = sum * geo;
        }//System.out.println(summen[0]);
        return summen;
    }

    // Optional: Überladung ohne geo, default = 1.0
    public double[] berechneSummenintensitaetMitKonz(double[] neueKonz) {
        return berechneSummenintensitaetMitKonz(neueKonz, 1.0);
    }

    public static double[] geometriefaktor(
            double[] konzentration,
            double[] summenintensitaet
    ) {
        if (konzentration.length != summenintensitaet.length)
            throw new IllegalArgumentException("Arrays müssen gleich lang sein!");

        double[] quot = new double[konzentration.length];
        for (int i = 0; i < konzentration.length; i++) {
            quot[i] = konzentration[i] / (summenintensitaet[i] == 0.0 ? 1.0 : summenintensitaet[i]);
        }
        return quot;
    }


    public static PreparedValues mitNeuerKonzentration(PreparedValues alt, double[] neueKonzNorm) {
        // Alle Felder vom alten übernehmen, außer konzNorm
        return new PreparedValues(
                alt.palpha(),
                alt.pbeta(),
                alt.tube0(),
                alt.tau0(),
                alt.omega0(),
                alt.mu0(),
                alt.sij0(),
                alt.tau(),
                alt.mu(),
                alt.countrate(),
                alt.mu_ijk(),
                alt.det_ijk(),
                alt.sij(),
                alt.alleKanten(),
                alt.alleUeberg(),
                alt.sij_xyz(),
                alt.tau_ijk(),
                neueKonzNorm,
                alt.step(),
                alt.emin(),
                alt.ltf()
        );
    }


    public static double[] berechneRelKonzentrationen(
            CalcI calc, PreparedValues pvStart, int maxIter
    ) {
        PreparedValues pv = pvStart;
        double[] originalKonzentration = pvStart.konzNorm();
        double[] alteKonz = originalKonzentration.clone();
        double[] neueKonz = originalKonzentration.clone();

        double threshold = 1e-6;   // Schwellwert für Abbruch

        for (int iter = 0; iter < maxIter; iter++) {
            Übergang[][] primaer = calc.primaerintensitaetBerechnen(pv);
            Übergang[][] sekundaer = calc.sekundaerintensitaetBerechnen(pv);
            double[] countsArray = berechneSummenintensitaetProElement(primaer, sekundaer);

            for (int i = 0; i < countsArray.length; i++) {
                if (countsArray[i] == 0.0) countsArray[i] = 1.0;
            }

            for (int i = 0; i < neueKonz.length; i++) {
                neueKonz[i] *= originalKonzentration[i] / (countsArray[i]);
            }

            double sum = Arrays.stream(neueKonz).sum();
            double[] neueKonzNorm = Arrays.stream(neueKonz).map(c -> c / sum).toArray();

            // Abbruchkriterium: maximale Änderung < threshold?
            double maxDelta = 0.0;
            for (int i = 0; i < neueKonzNorm.length; i++) {
                double delta = Math.abs(neueKonzNorm[i] - alteKonz[i]);
                if (delta > maxDelta) maxDelta = delta;
            }
            if (maxDelta < threshold) {
                //System.out.println("Konvergenz nach " + iter + " Iterationen");
                alteKonz = neueKonzNorm;
                break;
            }

            // Jetzt alteKonz updaten für den nächsten Durchlauf
            alteKonz = neueKonzNorm;
            neueKonz = neueKonzNorm.clone();
            pv = mitNeuerKonzentration(pv, neueKonzNorm);
        }

        double finalSum = Arrays.stream(alteKonz).sum();
        double[] rel = Arrays.stream(alteKonz).map(c -> c / finalSum * 100.0).toArray();
        return rel;
    }











}
