package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CalcIBuilder {
    // Felder
    private String dateipfad = "MCMASTER.TXT";
    private Probe probe;
    private String röhrenTyp = "widerschwinger";
    private String röhrenmaterial = "Rh";
    private double einfallswinkelalpha = 20;
    private double einfallswinkelbeta = 70;
    private double fensterwinkel = 0;
    private double sigma = 1;
    private double charzucontL = 1;
    private String fenstermaterialRöhre = "Be";
    private double fensterdickeRöhre = 125;
    private double raumwinkel = 1;
    private double röhrenstrom = 0.01;
    private double emin = 0;
    private double emax = 35;
    private double step = 0.05;
    private double messzeit = 30;
    private double charzucont = 1;
    private String fenstermaterialDet = "Be";
    private double fensterdickeDet = 7.62;
    private double phiDet = 0;
    private String kontaktmaterial = "Au";
    private double kontaktmaterialdicke = 50;
    private double bedeckungsfaktor = 1;
    private double palphaGrad = 45;
    private double pbetaGrad = 45;
    private String detektormaterial = "Si";
    private double totschicht = 0.05;
    private double activeLayer = 3;
    private List<Verbindung> filter_röhre = new ArrayList<>();
    private List<Verbindung> filter_det = new ArrayList<>();

    // Speichert, ob Emax verändert wurde seit letztem setProbe!
    private boolean emaxGeändert = false;

    // --- Setter ---
    public CalcIBuilder setDateipfad(String dateipfad) { this.dateipfad = dateipfad; return this; }
    public CalcIBuilder setProbe(Probe probe) { this.probe = probe; this.emax = probe.getEmax(); this.emaxGeändert = false; return this; }
    public CalcIBuilder setRöhrenTyp(String röhrenTyp) { this.röhrenTyp = röhrenTyp; return this; }
    public CalcIBuilder setRöhrenmaterial(String röhrenmaterial) { this.röhrenmaterial = röhrenmaterial; return this; }
    public CalcIBuilder setEinfallswinkelalpha(double val) { this.einfallswinkelalpha = val; return this; }
    public CalcIBuilder setEinfallswinkelbeta(double val) { this.einfallswinkelbeta = val; return this; }
    public CalcIBuilder setFensterwinkel(double val) { this.fensterwinkel = val; return this; }
    public CalcIBuilder setSigma(double val) { this.sigma = val; return this; }
    public CalcIBuilder setCharzucontL(double val) { this.charzucontL = val; return this; }
    public CalcIBuilder setFenstermaterialRöhre(String val) { this.fenstermaterialRöhre = val; return this; }
    public CalcIBuilder setFensterdickeRöhre(double val) { this.fensterdickeRöhre = val; return this; }
    public CalcIBuilder setRaumwinkel(double val) { this.raumwinkel = val; return this; }
    public CalcIBuilder setRöhrenstrom(double val) { this.röhrenstrom = val; return this; }
    public CalcIBuilder setEmin(double val) { this.emin = val; return this; }
    public CalcIBuilder setEmax(double val) { this.emax = val; this.emaxGeändert = true; return this; }
    public CalcIBuilder setStep(double val) { this.step = val; return this; }
    public CalcIBuilder setMesszeit(double val) { this.messzeit = val; return this; }
    public CalcIBuilder setCharzucont(double val) { this.charzucont = val; return this; }
    public CalcIBuilder setFenstermaterialDet(String val) { this.fenstermaterialDet = val; return this; }
    public CalcIBuilder setFensterdickeDet(double val) { this.fensterdickeDet = val; return this; }
    public CalcIBuilder setPhiDet(double val) { this.phiDet = val; return this; }
    public CalcIBuilder setKontaktmaterial(String val) { this.kontaktmaterial = val; return this; }
    public CalcIBuilder setKontaktmaterialdicke(double val) { this.kontaktmaterialdicke = val; return this; }
    public CalcIBuilder setBedeckungsfaktor(double val) { this.bedeckungsfaktor = val; return this; }
    public CalcIBuilder setPalphaGrad(double val) { this.palphaGrad = val; return this; }
    public CalcIBuilder setPbetaGrad(double val) { this.pbetaGrad = val; return this; }
    public CalcIBuilder setDetektormaterial(String val) { this.detektormaterial = val; return this; }
    public CalcIBuilder setTotschicht(double val) { this.totschicht = val; return this; }
    public CalcIBuilder setActiveLayer(double val) { this.activeLayer = val; return this; }
    public CalcIBuilder setFilterRöhre(List<Verbindung> val) { this.filter_röhre = val; return this; }
    public CalcIBuilder setFilterDet(List<Verbindung> val) { this.filter_det = val; return this; }

    // --- Probe-Klonen mit neuem Emax und aktiven Übergängen ---
    private static void kopiereAktivStatus(Probe alteProbe, Probe neueProbe) {
        List<List<Übergang>> alteUeb = alteProbe.getUebergaengeProElement();
        List<List<Übergang>> neueUeb = neueProbe.getUebergaengeProElement();
        for (int el = 0; el < alteUeb.size(); el++) {
            List<Übergang> altList = alteUeb.get(el);
            List<Übergang> neuList = neueUeb.get(el);
            for (int i = 0; i < altList.size(); i++) {
                if (altList.get(i).isAktiv()) {
                    neuList.get(i).setAktiv(true);
                }
            }
        }
    }

    private Probe baueNeueProbeMitEmax(Probe alt, double neuesEmax) {
        List<String> symbole = alt.getElemente().stream()
                .map(Element::getSymbol)
                .collect(Collectors.toList());
        List<Double> intensitaeten = Arrays.stream(alt.getIntensitäten()).boxed().collect(Collectors.toList());
        Probe neu = new Probe(
                symbole,
                alt.getDateipfad(),
                alt.getEmin(),
                neuesEmax,
                alt.getStep(),
                intensitaeten
        );
        kopiereAktivStatus(alt, neu);
        return neu;
    }

    // --- Build ---
    public CalcI build() {
        Probe finaleProbe = this.probe;
        if (finaleProbe != null && emaxGeändert) {
            finaleProbe = baueNeueProbeMitEmax(finaleProbe, this.emax);
            this.emaxGeändert = false; // Reset, falls mehrfach build()
        }
        return new CalcI(
                dateipfad,
                finaleProbe,
                röhrenTyp,
                röhrenmaterial,
                einfallswinkelalpha,
                einfallswinkelbeta,
                fensterwinkel,
                sigma,
                charzucontL,
                fenstermaterialRöhre,
                fensterdickeRöhre,
                raumwinkel,
                röhrenstrom,
                emin,
                emax,
                step,
                messzeit,
                charzucont,
                fenstermaterialDet,
                fensterdickeDet,
                phiDet,
                kontaktmaterial,
                kontaktmaterialdicke,
                bedeckungsfaktor,
                palphaGrad,
                pbetaGrad,
                detektormaterial,
                totschicht,
                activeLayer,
                filter_röhre,
                filter_det
        );
    }
}
