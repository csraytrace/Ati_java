package org.example;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class XrfFixture {
    private XrfFixture() {}

    //Berechente Int sollen 1,12 sein


    public static final double[] DARK_MATRIX = { 1.0 };
    public static final double   Z_MITTEL    = 17.0;

    /** Sucht zuerst auf dem Classpath, dann im CWD und bis zu 3 Elternordner höher. */
    public static String resolveFile(String name) {
        try {
            // 1) Classpath versuchen
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL url = cl.getResource(name);
            if (url != null) {
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    return Paths.get(url.toURI()).toString();
                }
                try (InputStream in = cl.getResourceAsStream(name)) {
                    Path tmp = Files.createTempFile("cp-" + name.replaceAll("[^a-zA-Z0-9._-]", "_") + "-", "");
                    Files.copy(in, tmp, REPLACE_EXISTING);
                    tmp.toFile().deleteOnExit();
                    return tmp.toAbsolutePath().toString();
                }
            }

            // 2) Working Directory + Elternordner durchsuchen
            Path dir = Paths.get(System.getProperty("user.dir"));
            for (int i = 0; i < 4 && dir != null; i++) {
                Path candidate = dir.resolve(name);
                if (Files.exists(candidate)) return candidate.toAbsolutePath().toString();
                dir = dir.getParent();
            }

            throw new IllegalStateException("Weder auf dem Classpath noch im Working Directory gefunden: " + name);
        } catch (Exception e) {
            throw new IllegalStateException("Kann Datei nicht auflösen: " + name, e);
        }
    }

    public static String resolveMCMASTER() {
        return resolveFile("MCMASTER.TXT");
    }

    // Beispiel-Fabriken (falls du sie nutzt):
    public static Probe newProbeDark() {
        String mcmasterPath = resolveMCMASTER();
        var elementSymboleDark = java.util.List.of("Si","Cu","O");
        var elementIntDark     = java.util.List.of(1,12,55);
        double Emin = 0, Emax = 30, step = 0.01;

        Probe p = new Probe(elementSymboleDark, mcmasterPath, Emin, Emax, step, elementIntDark);
        p.setzeUebergangAktivFuerElementKAlpha(0);
        p.setzeUebergangAktivFuerElementKAlpha(1);
        return p;
    }

    public static CalcIDark newCalcIDark() {
        String mcmasterPath = resolveMCMASTER();
        Probe probeDark = newProbeDark();
        return new CalcIDark(
                mcmasterPath,  // <— hier auch den absoluten Pfad übergeben
                probeDark,
                "widerschwinger",
                "Rh",
                20, 70,
                0,
                1,
                1,
                "Be",
                125,
                1,
                0.01,
                0, 30,
                0.01,
                30,
                1,
                "Be",
                7.62,
                0,
                "Au",
                50,
                1,
                45,
                45,
                "Si",
                0.05,
                3,
                null,
                null
        );
    }
}
