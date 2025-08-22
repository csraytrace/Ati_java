package org.example;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.*;


/*
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
*/


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



public class Kalibrierung {

    static DecimalFormat sciFormat = new DecimalFormat("0.0000E00");

    public static void kalibrierung(
            List<String[]> para_var,    //String [] hätte gereicht
            List<double[]> grenzen,
            Object stepanzahl,
            List<Probe> proben,
            boolean bedingung,
            String speicherort,
            String para     //para zu einem String [] machen, damit nicht alle Messungen bei denselben Bedingungen stattfinden müssen ToDO
            //macht aber wahrscheinlich keinen Sinn, weil man für jede Parametereinstellung eigentlich eine Kalibrierung möchte
            //daher sollten alle Proben unter derselben Bedingung gemessen werden
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


                    CalcIBuilder builder = new CalcIBuilder()
                            .setProbe(probe);
                    applySettingsToBuilder(builder, einstellung);
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




    public static double[] kalibrierungNLLSHipparchus(
            List<String[]> para_var,
            List<double[]> grenzen,
            double[] gemesseneIntensitaet,   // nur für Ziel-Länge; Ziel ist 0-VEktor
            List<Probe> proben,
            boolean bedingung,
            String para,
            double[] startwerte
    ) {
        final int n = para_var.size();

        // Startwerte
        double[] start = (startwerte != null && startwerte.length == n)
                ? startwerte.clone()
                : new double[n];
        if (startwerte == null) {
            for (int i = 0; i < n; i++) {
                start[i] = (grenzen.get(i)[0] + grenzen.get(i)[1]) / 2.0;
            }
        }

        // Bounds
        double[] lower = new double[n], upper = new double[n];
        for (int i = 0; i < n; i++) {
            lower[i] = grenzen.get(i)[0];
            upper[i] = grenzen.get(i)[1];
        }

        // Ziel-Länge (Residuenlänge) anhand Startpunkt bestimmen
        double[] rStart = berechneResiduen(start, para_var, proben, bedingung, para);
        final int m = rStart.length;

        // Modell: gibt Residuen UND Jacobi zurück (Vorwärtsdifferenzen)
        MultivariateJacobianFunction model = (RealVector point) -> {
            double[] p = point.toArray();
            double[] r0 = berechneResiduen(p, para_var, proben, bedingung, para);

            int rows = r0.length;
            int cols = p.length;
            double[][] J = new double[rows][cols];

            double[] h = computeAbsoluteStep2Point(p, null); // dein Step

            for (int j = 0; j < cols; j++) {
                double orig = p[j];
                double step = h[j];
                if (!Double.isFinite(step) || step == 0.0) {
                    step = Math.sqrt(Math.ulp(1.0)) * (orig >= 0.0 ? 1.0 : -1.0) * Math.max(1.0, Math.abs(orig));
                }

                p[j] = orig + step;
                double[] rp = berechneResiduen(p, para_var, proben, bedingung, para);
                p[j] = orig;

                for (int i = 0; i < rows; i++) {
                    double d = (rp[i] - r0[i]) / step;   // F(x+h)-F(x) / h
                    if (!Double.isFinite(d)) d = 0.0;
                    J[i][j] = d;
                }
            }

            RealVector value = new ArrayRealVector(r0, false);
            RealMatrix jac   = new Array2DRowRealMatrix(J, false);
            return new Pair<>(value, jac);
        };

        LeastSquaresBuilder b = new LeastSquaresBuilder()
                .start(new ArrayRealVector(start, false))
                .target(new double[m]) // Ziel = Nullvektor
                .model(model)
                .maxEvaluations(2000)
                .maxIterations(2000)
                .checkerPair(new SimpleVectorValueChecker(1e-10, 1e-10))
                .parameterValidator(vec -> {
                    double[] p = vec.toArray();
                    for (int i = 0; i < p.length; i++) {
                        p[i] = Math.max(lower[i], Math.min(upper[i], p[i]));
                    }
                    return new ArrayRealVector(p, false);
                });

        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum opt = optimizer.optimize(b.build());
        return opt.getPoint().toArray();
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
                double dx = (x0[i] + a) - x0[i]; // vermeidet Cancellation
                absStep[i] = (dx == 0.0)
                        ? rstep * sign[i] * Math.max(1.0, Math.abs(x0[i]))
                        : a;
            }
        }
        return absStep;
    }







/*

    public static double[] kalibrierungNLLS_bobyqa(
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

*/

    // Hilfsmethode für Residuen
    private static double[] berechneResiduen(
            double[] params, List<String[]> para_var, List<Probe> proben,
            boolean bedingung, String para
    ) {
        double[] geo_ele = new double[proben.size()];

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
            if (!para.isBlank()) einstellung += ", " + para;

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

        double geo_mittel = Arrays.stream(geo_ele).average().orElse(Double.NaN);
        double[] residuen = new double[geo_ele.length];
        for (int k = 0; k < residuen.length; k++) {
            residuen[k] = (geo_ele[k] - geo_mittel) / geo_mittel;
            //residuen[k] = (geo_ele[k] - geo_mittel) ;// absolut
        }


        return residuen;
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

    // Deine Main bleibt wie gehabt, aber du kannst es so testen:
    public static void main(String[] args) throws IOException  {
        List<String[]> paraVar = Arrays.asList(
                //new String[]{"sigma"},
                new String[]{"Einfallswinkelalpha"},
                new String[]{"activeLayer"},
                new String[]{"Totschicht"},
                new String[]{"charzucont_L"},
                new String[]{"charzucont"},
                new String[]{"Emax"},
                new String[]{"Kontaktmaterialdicke"}

        );

        List<double[]> grenzen = Arrays.asList(
                new double[]{15,25},
                new double[]{2, 4},
                new double[]{0.0, 0.2},
                new double[]{0.1, 1.2},
                new double[]{0.8, 1.1},
                new double[]{35, 45},
                new double[]{10, 40}
        );
        //int [] stepAnzahl = new int[]{3, 2,5,3,2};
        int [] stepAnzahl = new int[]{3,3,3,3,3,3,3};

// Deine Liste als Tripel (Intensität, Symbol, Übergang)
        List<Object[]> elementPaare = Arrays.asList(
                new Object[]{140000, "Ag", 0},
                new Object[]{66746,  "SN", 0},
                new Object[]{163000, "TI", 0},
                new Object[]{111267, "CD", 0},
                new Object[]{433090, "CU", 0},
                new Object[]{196370, "V", 0},
                new Object[]{544606, "ZR", 0},
                new Object[]{464025, "ZN", 0},
                new Object[]{498315, "GE", 0},
                new Object[]{10844,  "AL", 0},
                new Object[]{20692,  "SI", 0},
                new Object[]{106785, "BI", 1},
                new Object[]{13396,  "CD", 1},
                new Object[]{17196,  "SN", 1},
                new Object[]{109041, "PB", 1},
                new Object[]{82738,  "TA", 1},
                new Object[]{13240,  "Ag", 1}
        );

        List<Probe> probeliste = new ArrayList<>();
        for (Object[] eintrag : elementPaare) {
            int intensity = (int) eintrag[0];
            String symbol = (String) eintrag[1];
            int uebergang = (int) eintrag[2];

            List<String> elementSymbole = Arrays.asList(symbol);
            List<Integer> elementInt = Arrays.asList(intensity);

            Probe probe = new Probe(elementSymbole, "MCMASTER.TXT", 0, 35, 0.05, elementInt);
            // Übergang aktivieren:
            if (uebergang == 0) {
                probe.setzeUebergangAktivFuerElementKAlpha(0);
            } else if (uebergang == 1) {
                probe.setzeUebergangAktivFuerElementLAlpha(0);
            }
            probeliste.add(probe);
        }


        Kalibrierung.kalibrierung(
                paraVar,
                grenzen,
                stepAnzahl,
                probeliste,
                true,
                "kalibrierung_test_output.txt",
                ""
        );
    }
}

