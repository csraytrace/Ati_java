package org.example;

import java.util.*;

public abstract class RöhreBasis {
    protected final Verbindung roehrenMaterial;
    protected final Verbindung fensterMaterial;
    protected final double Emax;               // max. Röhrenspannung in keV
    protected final double EinfallswinkelalphaCos; // cos(alpha)
    protected final double EinfallswinkelbetaCos;  // cos(beta)
    protected final double fensterEinfallCos;   // cos(Fensterwinkel)
    protected final double fensterDickeCm;      // cm
    protected final double raumWinkel;          // sr
    protected final double roehrenStrom;        // A
    protected final double sigmaConst;          // Konstantenanteil in sigma-Formel
    protected final double step;                // keV
    protected final double Emin;                // keV
    protected final double messZeit;
    protected double zZaehler;
    protected double zNenner;// s
    protected double[] continuousSpectrum;
    protected List<Übergang> characteristicSpectrum;
    protected double[] gesamtSpektrum;
    protected List<Verbindung> Filter;

    protected RöhreBasis(
            String roehrenMaterialName,
            double    Einfallswinkelalpha,
            double    Einfallswinkelbeta,
            double    Fensterwinkel,
            double charzucont,
            double charzucontL,
            String fensterMaterialName,
            double fensterDickeUmikron,
            double raumWinkel,
            double roehrenStrom,
            double Emin,
            double Emax,
            double sigmaConst,
            double step,
            double messZeit,
            String folderPath,
            List<Verbindung> Filter

    ) {
        this.EinfallswinkelalphaCos = Math.cos(Math.toRadians(Einfallswinkelalpha));
        this.EinfallswinkelbetaCos  = Math.cos(Math.toRadians(Einfallswinkelbeta));
        this.fensterEinfallCos      = Math.cos(Math.toRadians(Fensterwinkel));
        this.fensterDickeCm         = fensterDickeUmikron * 1e-4;
        this.raumWinkel             = raumWinkel;
        this.roehrenStrom           = roehrenStrom;
        this.Emin                   = (Emin != 0.0) ? Emin : step;
        this.Emax                   = Emax;
        this.sigmaConst             = sigmaConst;
        this.step                   = step;
        this.messZeit               = messZeit;
        this.Filter                 = Filter;


        Funktionen f = new FunktionenImpl();
        this.roehrenMaterial = f.parseVerbindung(
                roehrenMaterialName, this.Emin, this.Emax, this.step, folderPath
        );
        this.fensterMaterial = f.parseVerbindung(
                fensterMaterialName, this.Emin, this.Emax, this.step, folderPath
        );

        this.zZaehler = 0;
        this.zNenner = 0;

    }


    protected void prepareData() {
        double weight = roehrenMaterial.getWeight()
                / roehrenMaterial.getDichte();
        double Z       = roehrenMaterial.Z_gemittelt();
        double J    = Z * 0.0135;
        double maxPen = weight/Z * (0.787e-5 * Math.sqrt(J) * Math.pow(Emax,1.5) + 0.735e-6 * Emax*Emax);
        double m    = 0.1382 - 0.9211/Math.sqrt(Z);
        double lnZ  = Math.log(Z);
        double n    = Math.pow(Emax,m) * (0.1904 - 0.2236*lnZ + 0.1292*lnZ*lnZ - 0.01491*lnZ*lnZ*lnZ);
        this.zZaehler = maxPen * (0.49269 - 1.0987*n + 0.78557*n*n);
        this.zNenner = 0.70256 - 1.09865*n + 1.0046*n*n;
        this.continuousSpectrum = computeContinuousSpectrum();
        this.characteristicSpectrum = computeCharacteristicSpectrum();
        this.gesamtSpektrum = berechneGesamtspektrumCountrate();

    }

    protected abstract double getSigma(double E);

    public double[] filterErzeugen() {
        double[] result;
        double[] energies = roehrenMaterial.getEnergieArray();
        int n = energies.length;

        if (Filter == null || Filter.isEmpty()) {
            result = new double[n];
            Arrays.fill(result, 1.0);
        } else {
            result = null;
            for (Verbindung filter : Filter) {
                double[] current = filter.erzeuge_Filter_liste();
                if (result == null) {
                    result = Arrays.copyOf(current, current.length);
                } else {
                    for (int i = 0; i < result.length; i++) {
                        result[i] *= current[i];
                    }
                }
            }
        }
        return result;
    }

    public double filterErzeugen(double energie) {
        if (Filter == null || Filter.isEmpty()) {
            return 1.0;
        } else {
            double produkt = 1.0;
            for (Verbindung filter : Filter) {
                produkt *= filter.erzeuge_Filter(energie);
            }
            return produkt;
        }
    }


    public double[] computeContinuousSpectrum() {
        double[] energies = roehrenMaterial.getEnergieArray();
        double rhoF     = fensterMaterial.getDichte();
        double[] muF    = fensterMaterial.mü_array_sum();
        double[] tauA   = roehrenMaterial.tau_array_sum();
        double rhoA     = roehrenMaterial.getDichte();
        double[] result = new double[energies.length];
        double[] FilterAbsorption = filterErzeugen();

        for (int i = 0; i < energies.length; i++) {
            double E = energies[i];
            if (E <= 0 || E >= Emax) {
                result[i] = 0;
                continue;
            }
            double U = Emax / E;
            double lnU = Math.log(U);
            double zQ = zZaehler * lnU / (zNenner + lnU);
            double sigma = getSigma(E);
            sigma *= sigmaConst;            // um das Sigma selbst zu verändern = eigenes Spektrum

            double xi = tauA[i] * EinfallswinkelalphaCos / EinfallswinkelbetaCos;
         //   if (Math.abs(Math.toDegrees(Math.acos(EinfallswinkelbetaCos)) - 75) > 1e-6){
        //        System.out.println(Math.toDegrees(Math.acos(EinfallswinkelalphaCos)));
        //    System.out.println(Math.toDegrees(Math.acos(EinfallswinkelbetaCos)));
      //  }
            double fx = (xi == 0 || zQ == 0)
                    ? 0
                    : (1 - Math.exp(-2 * xi * rhoA * zQ)) / (2 * xi * rhoA * zQ);

            double photons = sigma
                    * fx
                    * roehrenStrom
                    * messZeit
                    * step
                    * raumWinkel;

            // Threshold
            if (photons <= 1 || E <= 0.2) {
                photons = 0;
            }
            // Fensterdämpfung
            double transF = Math.exp(-muF[i] * rhoF * fensterDickeCm / fensterEinfallCos);

            result[i] = photons * transF * FilterAbsorption[i] ;//
        }
        return result;
    }


    protected abstract List<Double> RzuS_j();


    public List<Übergang> computeCharacteristicSpectrum() {
        List<Übergang> charSpec = new ArrayList<>();

        double Z = roehrenMaterial.Z_gemittelt();
        double D = roehrenMaterial.getDichte();
        double rhoF     = fensterMaterial.getDichte();


        double J = 0.0135 * Z;
        List<Double> RzuS_j_Liste = RzuS_j();
        List<List<Übergang>> Übergänge_list = roehrenMaterial.erzeugeÜbergängeListe();
        List<List<String[]>> omegaListe = roehrenMaterial.erzeugeOmegaListe();

        for (int i = 0; i < Übergänge_list.size(); i++) {
            List<Übergang> ÜbergängeProElement = Übergänge_list.get(i);
            for (Übergang Übergang : ÜbergängeProElement) {
                double U_energie = Übergang.getEnergy();
                double p_jk = Übergang.getRate();
                Enum Schale_von = Übergang.getSchale_von();
                Enum Schale_zu = Übergang.getSchale_zu();
                double Omega = getOmegaValue(omegaListe.get(i),Schale_von.name());
                double Konstante = 6E13;

                double tauAnode = roehrenMaterial.massenabsorptionskoeffizientEnergie(U_energie);
                double muF = fensterMaterial.massenschwächungskoeffizientEnergie(U_energie);
                //double muFenster = datenFenster.getMassAttenuationCoefficient(U_energy);
                //double dichteFenster = datenFenster.getDensity();

                double xi = tauAnode * EinfallswinkelalphaCos / EinfallswinkelbetaCos;
                double U     = Emax / U_energie;
                double lnU   = Math.log(U);
                double zQuer = zZaehler * lnU / (zNenner + lnU);
                double fx = (xi == 0 || zQuer == 0) ? 0 : (1 - Math.exp(-2 * xi * D * zQuer)) / (2 * xi * D * zQuer);

                // Fensterdämpfung
                double transF = Math.exp(-muF * rhoF * fensterDickeCm / fensterEinfallCos);
                //Filter
                double FilterAbsorption = filterErzeugen(U_energie);

                double Intensität = Konstante * RzuS_j_Liste.get(shellToIndex(Schale_von.name())+ i * 4) * Omega * p_jk
                        * fx * transF * roehrenStrom * messZeit * raumWinkel * roehrenMaterial.getKonzentrationen()[i]  //simpler Ansatz mit Multiplikation mit Konzentration
                        * FilterAbsorption;

                if (Intensität > 0) {
                    Übergang neuerUebergang = new Übergang(
                            (Schale) Schale_von,
                            (Schale) Schale_zu,
                            U_energie,
                            Intensität
                    );
                    charSpec.add(neuerUebergang);
                }
            }
        }

        return charSpec;
    }

    public static double getOmegaValue(List<String[]> omegaList, String shell) {
        shell = shell.trim();
        for (String[] arr : omegaList) {
            String schalenBezeichnung = arr[0].trim();
            // Nimm alles NACH dem Bindestrich (z.B. "Omega-K" → "K"), sonst das Original
            String letzterTeil = schalenBezeichnung.contains("-")
                    ? schalenBezeichnung.substring(schalenBezeichnung.lastIndexOf('-') + 1)
                    : schalenBezeichnung;
            if (letzterTeil.equalsIgnoreCase(shell)) {
                return Double.parseDouble(arr[1]);
            }
        }
        throw new IllegalArgumentException("Kein Wert für Schale " + shell + " gefunden!");
    }

    public static int shellToIndex(String shell) {
        switch (shell) {
            case "K": return 0;
            case "L1": return 1;
            case "L2": return 2;
            case "L3": return 3;
            default: throw new IllegalArgumentException("Unbekannte Schale: " + shell);
        }
    }

    public List<List<Double>> getEnergienVonKanten() {
        List<List<Kante>> kantenListe = roehrenMaterial.erzeugeKantenListe();
        List<List<Double>> alleEnergien = new ArrayList<>();
        String[] gewünschteReihenfolge = {"K", "L1", "L2", "L3"};

        for (List<Kante> kanten : kantenListe) {
            List<Double> energieListe = new ArrayList<>();
            Map<String, Double> energieMap = new HashMap<>();

            for (Kante kante : kanten) {
                energieMap.put(kante.getShell().name(), kante.getEnergy());
            }

            for (String schale : gewünschteReihenfolge) {
                if (energieMap.containsKey(schale)) {
                    energieListe.add(energieMap.get(schale));
                }
            }
            alleEnergien.add(energieListe);
        }

        return alleEnergien;
    }


    public double getCountRateChar(double energie, Double step) {
        // Wenn step null ist, nutze das Default-Objektattribut
        if (step == null) {
            step = this.step;
        }
        List<Übergang> spektrum = this.characteristicSpectrum;
        double summe = 0;
        for (int i = 0; i < spektrum.size(); i++) {
            if (energie <= spektrum.get(i).getEnergy() && energie + step > spektrum.get(i).getEnergy()) {
                summe += spektrum.get(i).getRate();
            }
        }
        return summe;
    }

    public double getCountRateChar(double energie) {
        return getCountRateChar(energie, null);
    }

    public double getCountRateCont(double energie, Double step) {
        if (step == null) {
            step = this.step;
        }
        double anodenTau = roehrenMaterial.massenabsorptionskoeffizientEnergie(energie);

        if (this.Emax <= energie) {
            return 0.0;
        }
        if (energie + step > this.Emax) {
            step = this.Emax - energie;
        }
        double fensterMue = fensterMaterial.massenschwächungskoeffizientEnergie(energie);
        double dichteFenster = fensterMaterial.getDichte();
        double D = roehrenMaterial.getDichte();

        double U = this.Emax / energie;
        double lnU = Math.log(U);
        double zQuer = zZaehler * lnU / (zNenner + lnU);
        double sigma = getSigma(energie);
        sigma *= sigmaConst;
        double Xi = anodenTau * this.EinfallswinkelalphaCos / this.EinfallswinkelbetaCos;
        double fx;
        if (Xi == 0 || zQuer == 0) {
            fx = 0;
        } else {
            fx = (1 - Math.exp(-2 * Xi * D * zQuer)) / (2 * Xi * D * zQuer);
        }
        double photonenAnzahl = sigma * fx * this.roehrenStrom * this.messZeit * step * this.raumWinkel;

        double transF = Math.exp(-fensterMue * dichteFenster * this.fensterDickeCm / this.fensterEinfallCos);

        //Filter
        double FilterAbsorption = filterErzeugen(energie);

        return photonenAnzahl*transF*FilterAbsorption;
    }

    public double getCountRateCont(double energie) {
        return getCountRateCont(energie, null);
    }


    public double[] berechneGesamtspektrumCountrate() {
        // Kopie der kontinuierlichen Spektrum-Counts:
        double[] gesamtspektrum = this.continuousSpectrum.clone();

        // Linien (characteristicSpectrum) addieren
        for (Übergang peak : this.characteristicSpectrum) {
            double energie = peak.getEnergy();
            double intensitaet = peak.getRate();
            // Prüfe auf Wertebereich
            if (energie < this.Emax && intensitaet > 1 && energie > this.Emin) {
                int index = (int) ((energie - this.Emin) / this.step);
                if (index >= 0 && index < gesamtspektrum.length) {
                    gesamtspektrum[index] += intensitaet;
                }
            }
        }

        return gesamtspektrum;
    }

    public double [] getGesamtspektrum(){return this.gesamtSpektrum;}


}
