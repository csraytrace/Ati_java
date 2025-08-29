package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Detektor {
    private String fenstermaterial;
    private double fensterdicke; // in cm
    private double phi; // in Grad
    private String kontaktmaterial;
    private double kontaktmaterialdicke; // in cm
    private double bedeckungsfaktor;
    private String detektormaterial;
    private double totschicht; // in cm
    private double activeLayer; // in cm
    private String datei;
    private double Emin;
    private double Emax;
    private double step;
    protected List<Verbindung> Filter;


    private Verbindung müKontaktmaterial;
    private Verbindung müFenstermaterial;
    private Verbindung müDetektor;
    private double [] detektorspektrumArray;
    private double [] energieArray;

    public Detektor(
            String fenstermaterial,
            double fensterdicke,         // in µm
            double phi,                  // in Grad
            String kontaktmaterial,
            double kontaktmaterialdicke, // in nm
            double bedeckungsfaktor,
            String detektormaterial,
            double totschicht,           // in µm
            double activeLayer,          // in mm
            String datei,
            double Emin,
            double Emax,
            double step,
            List<Verbindung> Filter
    ) {
        this.fenstermaterial = fenstermaterial;
        this.fensterdicke = fensterdicke * 1e-4;            // µm → cm
        this.phi = phi;
        this.kontaktmaterial = kontaktmaterial;
        this.kontaktmaterialdicke = kontaktmaterialdicke * 1e-7; // nm → cm
        this.bedeckungsfaktor = bedeckungsfaktor;
        this.detektormaterial = detektormaterial;
        this.totschicht = totschicht * 1e-4;                // µm → cm
        this.activeLayer = activeLayer * 1e-1;              // mm → cm
        this.datei = datei;
        this.Emin = (Emin != 0) ? Emin : step;
        this.Emax = Emax;
        this.step = step;
        this.Filter = Filter;
        //System.out.println(kontaktmaterialdicke);
        //System.out.println(activeLayer);

        Funktionen f = new FunktionenImpl();

        this.müKontaktmaterial = f.parseVerbindung(kontaktmaterial,Emin, Emax, step, datei);
        this.müFenstermaterial = f.parseVerbindung(fenstermaterial,Emin, Emax, step, datei);
        this.müDetektor = f.parseVerbindung(detektormaterial,Emin, Emax, step, datei);


        this.detektorspektrumArray = detektorspektrum();
    }

    // Optional: Default-Konstruktor
    public Detektor() {
        this(
                "Be",           // fenstermaterial
                7.62,           // fensterdicke [µm]
                0,              // phi [Grad]
                "Au",           // kontaktmaterial
                50,             // kontaktmaterialdicke [nm]
                1,              // bedeckungsfaktor
                "Si",           // detektormaterial
                0.05,           // totschicht [µm]
                3,              // activeLayer [mm]
                "MCMASTER.TXT", // Datei/Dateipfad
                0,              // Emin
                35,             // Emax
                0.05,           // step
                /* Filter */                   new ArrayList<>()
        );
    }

    public static double beerLambert(double massenschwKoeff, double dichte, double dicke, double phiGrad) {
        // phiGrad: Einfallswinkel in Grad
        // Math.toRadians(phiGrad) wandelt Grad in Bogenmaß um
        return Math.exp(-massenschwKoeff * dichte * dicke / Math.cos(Math.toRadians(phiGrad)));
    }

    public double[] filterErzeugen() {
        double[] result;
        double[] energies = müKontaktmaterial.getEnergieArray();
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






    public double detektoreffizienz(double energie) {
        // 1. Fenster-Effizienz
        double effizienz = beerLambert(
                müFenstermaterial.massenschwächungskoeffizientEnergie(energie),
                müFenstermaterial.getDichte(),
                fensterdicke,
                phi
        );

        // 2. Totschicht-Effizienz (im Detektor)
        effizienz *= beerLambert(
                müDetektor.massenschwächungskoeffizientEnergie(energie),
                müDetektor.getDichte(),
                totschicht,
                phi
        );

        // 3. Kontaktmaterial-Effizienz
        effizienz *= 1 - bedeckungsfaktor
                + bedeckungsfaktor * beerLambert(
                müKontaktmaterial.massenschwächungskoeffizientEnergie(energie),
                müKontaktmaterial.getDichte(),
                kontaktmaterialdicke,
                phi
        );

        // 4. Active Layer ("Signalverlust" im aktiven Bereich)
        effizienz *= 1 - beerLambert(
                müDetektor.massenschwächungskoeffizientEnergie(energie),
                müDetektor.getDichte(),
                activeLayer,
                phi
        );
        //Filter

        double FilterAbsorption = filterErzeugen(energie);

        return effizienz*FilterAbsorption;
    }
    public double[] detektorspektrum() {
        double[] energieArray = müFenstermaterial.getEnergieArray();
        double[] fensterArr = müFenstermaterial.mü_array_sum();
        double[] kontaktArr = müKontaktmaterial.mü_array_sum();
        double[] detektorArr = müDetektor.mü_array_sum();
        double[] FilterAbsorption = filterErzeugen();
        this.energieArray=energieArray;

        int n = energieArray.length;
        double[] effizienzArr = new double[n];

        double fensterDichte = müFenstermaterial.getDichte();
        double kontaktDichte = müKontaktmaterial.getDichte();
        double detektorDichte = müDetektor.getDichte();

        for (int i = 0; i < n; i++) {
            double müFenster = fensterArr[i];
            double müKontakt = kontaktArr[i];
            double müDetektor = detektorArr[i];

            double effizienz = beerLambert(müFenster, fensterDichte, fensterdicke, phi);
            effizienz *= beerLambert(müDetektor, detektorDichte, totschicht, phi);
            effizienz *= 1 - bedeckungsfaktor
                    + bedeckungsfaktor * beerLambert(müKontakt, kontaktDichte, kontaktmaterialdicke, phi);
            effizienz *= 1 - beerLambert(müDetektor, detektorDichte, activeLayer, phi);



            effizienzArr[i] = effizienz * FilterAbsorption[i];
        }

        return effizienzArr;
    }

    public double [] getDetektorspektrumArray(){return detektorspektrumArray;}
    public double [] getEnergieArray(){return energieArray;}
}