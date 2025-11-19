package org.example;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.*;



import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;



//EMAX darf nicht mit einem Filter variiert werden

public class Kalibrierung {

    private List<Verbindung> filterRöhre;
    private List<Verbindung> filterDet;

    // 1) Default: leere Filter
    public Kalibrierung() {
        this.filterRöhre = new ArrayList<>();
        this.filterDet   = new ArrayList<>();
    }

    // 2) Mit zwei Listen initialisieren
    public Kalibrierung(List<Verbindung> filterRoehre, List<Verbindung> filterDet) {
        // defensiv kopieren + null-sicher
        this.filterRöhre = (filterRoehre == null) ? new ArrayList<>() : new ArrayList<>(filterRoehre);
        this.filterDet   = (filterDet   == null) ? new ArrayList<>() : new ArrayList<>(filterDet);
    }

    static DecimalFormat sciFormat = new DecimalFormat("0.0000E00");

    public void kalibrierung(
            List<String[]> para_var,    //String [] hätte gereicht
            List<double[]> grenzen,
            Object stepanzahl,
            List<Probe> proben,
            boolean bedingung,
            String speicherort,
            String para     //para zu einem String [] machen, damit nicht alle Messungen bei denselben Bedingungen stattfinden müssen, sollte eigentlich ein react sein
    ) throws IOException {

        List<List<Double>> grenzenNeu = new ArrayList<>();
        List<double[]> liste;

        if (stepanzahl instanceof Number stepsNumber) {
            int steps = stepsNumber.intValue();
            for (double[] grenze : grenzen) {
                double distanz = (grenze[1] - grenze[0]) / (steps - 1);
                List<Double> temp = new ArrayList<>();
                for (int j = 0; j < steps; j++)
                    temp.add(grenze[0] + j * distanz);
                grenzenNeu.add(temp);
            }
        } else if (stepanzahl instanceof int[] steps) {
            for (int i = 0; i < grenzen.size(); i++) {
                double distanz = grenzen.get(i)[1] - grenzen.get(i)[0];
                List<Double> temp = new ArrayList<>();
                for (int x = 0; x < steps[i]; x++)
                    temp.add(grenzen.get(i)[0] + x * distanz / (steps[i] - 1));
                grenzenNeu.add(temp);
            }
        }

        liste = product(grenzenNeu);

        int aufrufe = liste.size() ;
        int zaehler = 0;


        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(speicherort))) {

            for (double[] werte : liste) {
                List<String> parts = new ArrayList<>();
                for (int index = 0; index < werte.length; index++)
                    parts.add(para_var.get(index)[0] + "=" + sciFormat.format(werte[index]));

                if (bedingung) {
                    for (int index = 0; index < werte.length; index++) {
                        if (para_var.get(index)[0].equals("Einfallswinkelalpha")) {
                            parts.add("Einfallswinkelbeta=" + sciFormat.format(90 - werte[index]));
                            break;
                        }
                        if (para_var.get(index)[0].equals("Einfallswinkelbeta")) {
                            parts.add("Einfallswinkelalpha=" + sciFormat.format(90 - werte[index]));
                            break;
                        }
                    }
                }

                String einstellung = parts.stream()
                        .map(s -> s.replace(",", "."))
                        .collect(Collectors.joining(", "));
                if (!para.isBlank()) einstellung += ", " + para;

                writer.write("#Einstellung: " + einstellung + "\n");

                // Jetzt ALLE PROBEN für diese Einstellung!
                zaehler++;
                System.out.println(einstellung + " Anzahl: " + zaehler + "/" + aufrufe);


                for (Probe probe : proben) {


                    CalcIBuilder builder = new CalcIBuilder()   //könnte auf CalcIDark erweitert werden
                            .setProbe(probe);
                    applySettingsToBuilder(builder, einstellung);
                    builder.setFilterRöhre(filterRöhre);
                    builder.setFilterDet(filterDet);
                    CalcI calc = builder.build();

                    PreparedValues pv = calc.werteVorbereitenAlle();

                    double[] konz;
                    if (probe.getElemente().size() == 1)
                        konz = new double[]{1.0};
                    else
                        konz = calc.berechneRelKonzentrationen(calc, pv, 100);  //100 Iterationen

                    double[] intensitaeten = calc.berechneSummenintensitaetMitKonz(konz);
                    double[] geo = CalcI.geometriefaktor(probe.getIntensitäten(), intensitaeten);       //geht mit calc
                    double[] mittlereEnergie = probe.berechneMittlereEnergieProElement();


                    double uEnergie = 0;
                    double geo_mittel = 0;
                    StringBuilder symboleBuilder = new StringBuilder();

                    for (int i = 0; i < geo.length; i++) {
                        uEnergie += mittlereEnergie[i] * konz[i];
                        geo_mittel += geo[i];
                        String symbol = probe.getElemente().get(i).getSymbol();
                        if (i > 0) symboleBuilder.append(", ");
                        symboleBuilder.append(symbol);
                    }
                    geo_mittel /= geo.length;
                    String symbole = symboleBuilder.toString();

                    // Ausgabe für diese Probe
                    writer.write(String.format(Locale.US, "%.8f\t%.16f\t%s\n", uEnergie, geo_mittel, symbole));
                }
                //writer.write("\n");
            }
        }
    }


    public double[] kalibrierungNLLS_bobyqa(
            List<String[]> para_var,
            List<double[]> grenzen,
            double[] gemesseneIntensitaet,
            List<Probe> proben,
            boolean bedingung,
            String para,
            double[] startwerte
    ) {
        int paramCount = para_var.size();

        double[] start = (startwerte != null && startwerte.length == paramCount)
                ? startwerte
                : new double[paramCount];
        if (startwerte == null) {
            for (int i = 0; i < paramCount; i++)
                start[i] = (grenzen.get(i)[0] + grenzen.get(i)[1]) / 2.0;
        }

        double[] lower = new double[paramCount];
        double[] upper = new double[paramCount];
        for (int i = 0; i < paramCount; i++) {
            lower[i] = grenzen.get(i)[0];
            upper[i] = grenzen.get(i)[1];
        }

        // Zielfunktion: Summe der quadrierten Residuen
        ObjectiveFunction objective = new ObjectiveFunction(params -> {
            double[] residuen = berechneResiduen(params, para_var, proben, bedingung, para);
            double sum = 0;
            for (double r : residuen) sum += r * r;
            return sum;
        });

        // Bounds richtig als SimpleBounds übergeben!
        SimpleBounds bounds = new SimpleBounds(lower, upper);

        // BOBYQA braucht mindestens (2*paramCount + 1) Interpolationspunkte!
        int numberOfInterpolationPoints = 2 * paramCount + 1;
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(numberOfInterpolationPoints);

        PointValuePair optimum = optimizer.optimize(
                new MaxEval(10000),
                objective,
                GoalType.MINIMIZE,
                new InitialGuess(start),
                bounds
        );

        return optimum.getPoint();
    }


    // Hilfsmethode für Residuen
    private double[] berechneResiduen(
            double[] params, List<String[]> para_var, List<Probe> proben,
            boolean bedingung, String para
    ) {
        double[] geo_ele = new double[proben.size()];


        String paraClean = filterPara(para, para_var, bedingung);



        for (int j = 0; j < proben.size(); j++) {
            Probe probe = proben.get(j);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < params.length; i++)
                parts.add(para_var.get(i)[0] + "=" + sciFormat.format(params[i]));

            if (bedingung) {
                for (int i = 0; i < params.length; i++) {
                    if (para_var.get(i)[0].equals("Einfallswinkelalpha"))
                        parts.add("Einfallswinkelbeta=" + sciFormat.format(90 - params[i]));
                    else if (para_var.get(i)[0].equals("Einfallswinkelbeta"))
                        parts.add("Einfallswinkelalpha=" + sciFormat.format(90 - params[i]));
                }
            }
            String einstellung = parts.stream()
                    .map(s -> s.replace(",", "."))
                    .collect(Collectors.joining(", "));
            if (!paraClean.isBlank()) einstellung += ", " + paraClean;

            CalcIBuilder builder = new CalcIBuilder().setProbe(probe);
            applySettingsToBuilder(builder, einstellung);
            builder.setFilterRöhre(filterRöhre);
            builder.setFilterDet(filterDet);
            CalcI calc = builder.build();
            PreparedValues pv = calc.werteVorbereitenAlle();

            double[] konz = probe.getElemente().size() == 1 ? new double[]{1.0}
                    : calc.berechneRelKonzentrationen(calc, pv, 100);
            double[] intensitaeten = calc.berechneSummenintensitaetMitKonz(konz);
            //double[] geo = calc.geometriefaktor(konz, intensitaeten);
            double[] geo = calc.geometriefaktor(probe.getIntensitäten(), intensitaeten);

            geo_ele[j] = Arrays.stream(geo).average().orElse(Double.NaN);
        }

        double geo_mittel = Arrays.stream(geo_ele).average().orElse(Double.NaN);
        double[] residuen = new double[geo_ele.length];
        for (int k = 0; k < residuen.length; k++) {
            //residuen[k] = (geo_ele[k] - geo_mittel) / geo_mittel;
            residuen[k] = (geo_ele[k] - geo_mittel) ;// absolut
        }


        return residuen;
    }


    private static String normalizeKey(String key) {
        if (key == null) return "";
        String k = key.trim().toLowerCase(Locale.ROOT);
        k = k.replace("_", ""); // "charzucont_l" == "charzucontl"
        // Aliase vereinheitlichen
        if (k.equals("charzucontl")) k = "charzucontl";
        return k;
    }

    private static String filterPara(String para, List<String[]> para_var, boolean bedingung) {
        if (para == null || para.isBlank()) return para;

        // Alle Optimierungs-Keys sammeln (normalisiert)
        Set<String> optKeys = new HashSet<>();
        for (String[] p : para_var) {
            if (p != null && p.length > 0) optKeys.add(normalizeKey(p[0]));
        }
        boolean hasAlpha = optKeys.contains("einfallswinkelalpha");
        boolean hasBeta  = optKeys.contains("einfallswinkelbeta");

        String[] tokens = para.split(",");
        List<String> kept = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            String[] kv = t.split("=", 2);
            if (kv.length != 2) continue;

            String rawKey = kv[0].trim();
            String key    = normalizeKey(rawKey);

            // 1) Alles entfernen, was in para_var optimiert wird
            if (optKeys.contains(key)) continue;

            // 2) Bei Bedingung: Gegenwinkel aus para entfernen
            if (bedingung) {
                if (hasAlpha && key.equals("einfallswinkelbeta")) continue;
                if (hasBeta  && key.equals("einfallswinkelalpha")) continue;
            }

            // sonst behalten (Original-Schreibweise bewahren)
            kept.add(rawKey + "=" + kv[1].trim());
        }

        return String.join(", ", kept);
    }







    public static double[] berechneGeo(
            double[] params, List<String[]> para_var, List<Probe> proben,
            boolean bedingung, String para
    ) {
        double[] geo_ele = new double[proben.size()];


        String paraClean = filterPara(para, para_var, bedingung);
        System.out.println(para);
        System.out.println(paraClean);

        for (int j = 0; j < proben.size(); j++) {
            Probe probe = proben.get(j);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < params.length; i++)
                parts.add(para_var.get(i)[0] + "=" + sciFormat.format(params[i]));

            if (bedingung) {
                for (int i = 0; i < params.length; i++) {
                    if (para_var.get(i)[0].equals("Einfallswinkelalpha"))
                        parts.add("Einfallswinkelbeta=" + sciFormat.format(90 - params[i]));
                    else if (para_var.get(i)[0].equals("Einfallswinkelbeta"))
                        parts.add("Einfallswinkelalpha=" + sciFormat.format(90 - params[i]));
                }
            }
            String einstellung = parts.stream()
                    .map(s -> s.replace(",", "."))
                    .collect(Collectors.joining(", "));
            if (!paraClean.isBlank()) einstellung += ", " + paraClean;

            CalcIBuilder builder = new CalcIBuilder().setProbe(probe);
            applySettingsToBuilder(builder, einstellung);
            CalcI calc = builder.build();
            PreparedValues pv = calc.werteVorbereitenAlle();

            double[] konz = probe.getElemente().size() == 1 ? new double[]{1.0}
                    : calc.berechneRelKonzentrationen(calc, pv, 100);
            double[] intensitaeten = calc.berechneSummenintensitaetMitKonz(konz);
            //double[] geo = calc.geometriefaktor(konz, intensitaeten);
            double[] geo = calc.geometriefaktor(probe.getIntensitäten(), intensitaeten);

            geo_ele[j] = Arrays.stream(geo).average().orElse(Double.NaN);
        }

        return geo_ele;
    }

    public static double mittelGeo(double [] geo)
    {return Arrays.stream(geo).average().orElse(Double.NaN);}

    public static double mittlereAbweichung(double[] geo) {
        // Nur endliche Werte berücksichtigen
        double[] vals = Arrays.stream(geo).filter(Double::isFinite).toArray();
        if (vals.length == 0) return Double.NaN;

        double mean = Arrays.stream(vals).average().orElse(Double.NaN);
        double sumAbs = 0.0;
        for (double v : vals) sumAbs += Math.abs(v - mean);
        return sumAbs / vals.length / mittelGeo(geo);
    }





    private static List<double[]> product(List<List<Double>> lists) {
        List<double[]> result = new ArrayList<>();
        productHelper(result, lists, 0, new double[lists.size()]);
        return result;
    }

    private static void productHelper(List<double[]> result, List<List<Double>> lists, int depth, double[] current) {
        if (depth == lists.size()) {
            result.add(current.clone());
            return;
        }
        for (double value : lists.get(depth)) {
            current[depth] = value;
            productHelper(result, lists, depth + 1, current);
        }
    }

    // -------- applySettingsToBuilder bleibt wie gehabt --------
    public static void applySettingsToBuilder(CalcIBuilder builder, String einstellung) {
        String[] tokens = einstellung.split(",");
        for (String token : tokens) {
            String[] pair = token.trim().split("=");
            if (pair.length != 2) continue;
            String key = pair[0].trim().toLowerCase();
            String value = pair[1].trim();

            try {
                switch (key) {
                    case "dateipfad":
                        builder.setDateipfad(value);
                        break;
                    case "probe":
                        break;
                    case "röhrentyp":
                        builder.setRöhrenTyp(value);
                        break;
                    case "röhrenmaterial":
                        builder.setRöhrenmaterial(value);
                        break;
                    case "einfallswinkelalpha":
                        builder.setEinfallswinkelalpha(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "einfallswinkelbeta":
                        builder.setEinfallswinkelbeta(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "fensterwinkel":
                        builder.setFensterwinkel(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "sigma":
                        builder.setSigma(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "charzucontl":
                        builder.setCharzucontL(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "fenstermaterialröhre":
                        builder.setFenstermaterialRöhre(value);
                        break;
                    case "fensterdickeröhre":
                        builder.setFensterdickeRöhre(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "raumwinkel":
                        builder.setRaumwinkel(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "röhrenstrom":
                        builder.setRöhrenstrom(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "emin":
                        builder.setEmin(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "emax":
                        builder.setEmax(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "step":
                        builder.setStep(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "messzeit":
                        builder.setMesszeit(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "charzucont":
                        builder.setCharzucont(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "fenstermaterialdet":
                        builder.setFenstermaterialDet(value);
                        break;
                    case "fensterdickedet":
                        builder.setFensterdickeDet(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "phidet":
                        builder.setPhiDet(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "kontaktmaterial":
                        builder.setKontaktmaterial(value);
                        break;
                    case "kontaktmaterialdicke":
                        builder.setKontaktmaterialdicke(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "bedeckungsfaktor":
                        builder.setBedeckungsfaktor(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "palphagrad":
                        builder.setPalphaGrad(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "pbetagrad":
                        builder.setPbetaGrad(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "detektormaterial":
                        builder.setDetektormaterial(value);
                        break;
                    case "totschicht":
                        builder.setTotschicht(Double.parseDouble(value.replace(",", ".")));
                        break;
                    case "activelayer":
                        builder.setActiveLayer(Double.parseDouble(value.replace(",", ".")));
                        break;
                }
            } catch (Exception ex) {
                System.err.println("Konnte Parameter nicht setzen: " + key + "=" + value + " (" + ex.getMessage() + ")");
            }
        }
    }
}

