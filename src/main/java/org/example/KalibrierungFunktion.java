package org.example;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.mariuszgromada.math.mxparser.*;



// imports (oben in der Datei):
//import org.hipparchus.analysis.MultivariateJacobianFunction;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresBuilder;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
//import org.hipparchus.optim.nonlinear.vector.leastsquares.SimpleVectorValueChecker;
import org.hipparchus.util.Pair;



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


public class KalibrierungFunktion {

    private final List<Verbindung> filterRöhre;
    private final List<Verbindung> filterDet;
    private final double geo;



    //static { License.iConfirmNonCommercialUse("Julia – Masterarbeit"); }

    /* ---------- Param-Erkennung ---------- */
    private static final Set<String> KNOWN_VARS = Set.of("x");
    private static final Set<String> STOPWORDS = Set.of(
            "sin","cos","tan","asin","acos","atan","atan2",
            "sinh","cosh","tanh","asinh","acosh","atanh",
            "ln","log","log10","exp","sqrt","abs","sign",
            "floor","ceil","round","min","max","if","mod","pow",
            "pi","π","e","phi","nan","inf","infinity"
    ).stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    private static final Pattern IDENT = Pattern.compile("\\b[\\p{L}_][\\p{L}\\p{N}_]*\\b");

    private static List<String> findParams(String expr) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Matcher m = IDENT.matcher(expr);
        while (m.find()) {
            String tok = m.group();
            String low = tok.toLowerCase(Locale.ROOT);
            if (KNOWN_VARS.contains(low)) continue;
            if (STOPWORDS.contains(low)) continue;
            // keine Zahl, kein Funktionsname => Parameter
            out.add(tok);
        }
        return new ArrayList<>(out); // Reihenfolge der ersten Vorkommen
    }

    // 1) Default: leere Filter
    public KalibrierungFunktion(double geo) {
        this.filterRöhre = new ArrayList<>();
        this.filterDet   = new ArrayList<>();
        this.geo   = geo;
    }

    // 2) Mit zwei Listen initialisieren
    public KalibrierungFunktion(List<Verbindung> filterRoehre, List<Verbindung> filterDet,double geo) {
        // defensiv kopieren + null-sicher
        this.filterRöhre = (filterRoehre == null) ? new ArrayList<>() : new ArrayList<>(filterRoehre);
        this.filterDet   = (filterDet   == null) ? new ArrayList<>() : new ArrayList<>(filterDet);
        this.geo = geo;
    }

    static DecimalFormat sciFormat = new DecimalFormat("0.0000E00");

    /** Wert einer Funktion expr bei optionalem x und Parametern {a,b,c,...}. */
    private static double evalFunction(String expr, Map<String, Double> params, Double xOpt) {
        if (expr == null || expr.isBlank()) return Double.NaN;

        // Argumente bauen
        List<Argument> args = new ArrayList<>();
        Argument xArg = null;
        if (xOpt != null) {
            xArg = new Argument("x", xOpt);
            args.add(xArg);
        }
        if (params != null) {
            for (Map.Entry<String, Double> e : params.entrySet()) {
                String name = e.getKey();
                double val  = e.getValue();
                args.add(new Argument(name, val));
            }
        }

        Expression e = new Expression(expr, args.toArray(new Argument[0]));
        if (!e.checkSyntax()) return Double.NaN;

        double y = e.calculate();
        return Double.isFinite(y) ? y : Double.NaN;
    }


    private static String substituteParams(String expr, Map<String, Double> params) {
        if (expr == null || expr.isBlank() || params == null || params.isEmpty()) return expr;

        // längere Namen zuerst (rein vorsichtshalber)
        List<String> names = new ArrayList<>(params.keySet());
        names.sort((a,b) -> Integer.compare(b.length(), a.length()));

        String out = expr;
        for (String name : names) {
            double val = params.get(name);
            String num = String.format(Locale.US, "%.15g", val); // DezimalPUNKT
            // nur ganze Tokens ersetzen: kein Teil in Funktionsnamen etc.
            out = out.replaceAll("(?<![\\p{L}\\p{N}_])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}_])", num);
        }
        return out;
    }

    private static List<Verbindung> copyFilterList(List<Verbindung> src) {
        List<Verbindung> out = new ArrayList<>(src.size());
        for (Verbindung v : src) out.add(v.copy());
        return out;
    }




    private static void applyModulation(Verbindung fil,
                                        String exprTemplate,
                                        Map<String, Double> params,
                                        double Emin, double Emax) {
        // 1 = neutrales Multiplizieren außerhalb der Intervalle
        fil.clearModulation(1.0);

        // Parameter einsetzen -> reiner Zahlen/„x“-Ausdruck
        String expr = substituteParams(exprTemplate, params);

        // Als Modulationssegment über das gesamte Fenster hinzufügen
        fil.addModulationSegment(expr, Emin, true, Emax, true);
    }




    public double[] berechneResiduen(
            double[] params, List<String[]> para_var, List<Probe> proben,
            boolean bedingung, String para,        String funktion,
            Map<String, Double> funktionsParameter,
            double x1, double x2    //Schranken
            //Double xWert  bekomme von Probe
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
            List<Verbindung> fr = copyFilterList(filterRöhre);  //Ohne Modulation!
            List<Verbindung> fd = copyFilterList(filterDet);


            for (Verbindung fil : fr) {
                applyModulation(fil, funktion, funktionsParameter, x1, x2); //derzeit nur 1 Segment möglich, apply muss sonst geändert werden
            }

            builder.setFilterRöhre(fr);
            builder.setFilterDet(fd);
            CalcI calc = builder.build();
            PreparedValues pv = calc.werteVorbereitenAlle();


            double[] konz = probe.getElemente().size() == 1 ? new double[]{1.0}
                    : calc.berechneRelKonzentrationen(calc, pv, 100);
            double[] intensitaeten = calc.berechneSummenintensitaetMitKonz(konz);
            double[] geo = calc.geometriefaktor(probe.getIntensitäten(), intensitaeten);

            geo_ele[j] = Arrays.stream(geo).average().orElse(Double.NaN);

        }

        double geo_mittel = Arrays.stream(geo_ele).average().orElse(Double.NaN);
        double[] residuen = new double[geo_ele.length];
        for (int k = 0; k < residuen.length; k++) {
            //residuen[k] = (geo_ele[k] - geo_mittel) / geo_mittel;
            residuen[k] = (geo_ele[k] - geo) ;// absolut
        }

        final int n = residuen.length;
        double rss = 0.0, l1 = 0.0;
        for (double r : residuen) { rss += r*r; l1 += Math.abs(r); }
        double rmse = Math.sqrt(rss / n);

        System.out.printf(java.util.Locale.US,
                "[LS] RSS=%.6e, RMSE=%.6e, L1=%.6e%n", rss, rmse, l1);
        String funcLine =
                funktionsParameter.entrySet().stream()
                        .map(e -> e.getKey() + "=" + String.format(java.util.Locale.US, "%.6g", e.getValue()))
                        .collect(java.util.stream.Collectors.joining(", "));

        System.out.println("[func] " + funcLine);
        System.out.println(mittlereAbweichung(geo_ele));


        // --- NEU: transformierte Residuen für L1-Minimierung zurückgeben ---
        double eps = 1e-12;                          // kleine Glättung
        double[] transformed = new double[n];
        for (int i = 0; i < n; i++) {
            transformed[i] = Math.sqrt(Math.abs(residuen[i]) + eps);
        }
        return transformed;

        //return residuen;
    }




    public double[] berechneGeo(
            double[] params, List<String[]> para_var, List<Probe> proben,
            boolean bedingung, String para,        String funktion,
            Map<String, Double> funktionsParameter,
            double x1, double x2    //Schranken
            //Double xWert  bekomme von Probe
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
            List<Verbindung> fr = copyFilterList(filterRöhre);  //Ohne Modulation!



            for (Verbindung fil : fr) {
                applyModulation(fil, funktion, funktionsParameter, x1, x2); //derzeit nur 1 Segment möglich, apply muss sonst geändert werden x1,x2
            }

            builder.setFilterRöhre(fr);
            builder.setFilterDet(filterDet);
            CalcI calc = builder.build();
            PreparedValues pv = calc.werteVorbereitenAlle();


            double[] konz = probe.getElemente().size() == 1 ? new double[]{1.0}
                    : calc.berechneRelKonzentrationen(calc, pv, 100);
            double[] intensitaeten = calc.berechneSummenintensitaetMitKonz(konz);
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

    public static double[] computeAbsoluteStep2Point(double[] x0) {
        return computeAbsoluteStep2Point(x0, null);
    }


    /** Optimiert NUR die Funktions-Parameter (a,b,c,...) der Modulationsfunktion mit LM (ohne Bounds). */
    public Map<String, Double> fitFunktionsParameterLM(
            double[] fixedParams,                 // bleibt fix
            List<String[]> para_var,
            List<Probe> proben,
            boolean bedingung,
            String para,
            String funktion,                      // z.B. "a*x^2 + b*x + c"
            LinkedHashMap<String, Double> startMap, // Startwerte + Reihenfolge
            double x1, double x2                  // Modulationsfenster
    ) {
        // 0) Namen & Startvektor aus der Map ziehen (stabile Reihenfolge)
        final List<String> names = new ArrayList<>(startMap.keySet());
        final int p = names.size();
        final double[] theta0 = new double[p];
        for (int k = 0; k < p; k++) theta0[k] = startMap.get(names.get(k));

        // 1) Residuen-Länge am Start
        final int m = proben.size();

        // 2) Hilfsfunktionen: Vektor<->Map und FD-Schrittweiten
        java.util.function.Function<double[], Map<String,Double>> vecToMap = vec -> {
            LinkedHashMap<String,Double> mapp = new LinkedHashMap<>();
            for (int k = 0; k < p; k++) mapp.put(names.get(k), vec[k]);
            return mapp;
        };
        java.util.function.Function<double[], double[]> fdStep = vec -> {
            double[] h = new double[vec.length];
            for (int k=0;k<vec.length;k++) {
                double v = vec[k];
                h[k] = 1e-6 * Math.max(1.0, Math.abs(v)); // vorwärtsdifferenz
            }
            return h;
        };

        // 3) Modell: gibt Residuen und Jacobian wrt. Funktionsparametern zurück
        MultivariateJacobianFunction model = (RealVector point) -> {
            double[] theta = point.toArray();
            Map<String,Double> thetaMap = vecToMap.apply(theta);

            double[] r0 = berechneResiduen(
                    fixedParams, para_var, proben, bedingung, para,
                    funktion, thetaMap, x1, x2
            );

            //int rows = r0.length;
            int rows = proben.size();
            double[][] J = new double[rows][p];
            //double[] h = fdStep.apply(theta);

            final double [] h = computeAbsoluteStep2Point(theta0);


            for (int k = 0; k < p; k++) {
                double old = theta[k];
                theta[k] = old + h[k];
                Map<String,Double> thetaP = vecToMap.apply(theta);
                double[] rP = berechneResiduen(
                        fixedParams, para_var, proben, bedingung, para,
                        funktion, thetaP, x1, x2
                );
                theta[k] = old;

                for (int i = 0; i < rows; i++) {
                    double d = (rP[i] - r0[i]) / h[k];
                    if (!Double.isFinite(d)) d = 0.0;
                    J[i][k] = d;
                }
            }

            RealVector value = new ArrayRealVector(r0, false);
            RealMatrix jac   = new Array2DRowRealMatrix(J, false);
            return new Pair<>(value, jac);
        };

        // 4) Least-Squares Problem: Ziel = Nullvektor (Residuen minimieren)
        LeastSquaresBuilder b = new LeastSquaresBuilder()
                .start(new ArrayRealVector(theta0, false))
                .target(new double[m])                 // Nullvektor
                .model(model)
                .maxEvaluations(10000)
                .maxIterations(10000)
                .checkerPair(new SimpleVectorValueChecker(1e-10, 1e-10));

        LeastSquaresOptimizer.Optimum opt =
                new LevenbergMarquardtOptimizer().optimize(b.build());

        // 5) Ergebnis zurück in Map
        double[] sol = opt.getPoint().toArray();
        LinkedHashMap<String,Double> out = new LinkedHashMap<>();
        for (int k = 0; k < p; k++) out.put(names.get(k), sol[k]);
        return out;
    }






}
