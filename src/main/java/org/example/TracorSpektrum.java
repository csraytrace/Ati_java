package org.example;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TracorSpektrum {

    // -------------------------
    // Public API (ähnlich Python)
    // -------------------------
    public static Result tracorDatenToSpe(String filePath, boolean info, boolean saveSpe, String outDirOrPrefix) throws IOException {
        byte[] content = readFile(filePath);

        // 1) Spektrum extrahieren je nach Dateigröße
        int[] ySpec;
        double[] xValues;

        if (content.length == 8424) {
            IntDoublePair pair = convertToInt(content, 184, 8376);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else if (content.length == 4328) {
            IntDoublePair pair = convertToInt(content, 184, 4280);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else if (content.length == 2280) {
            IntDoublePair pair = convertToInt(content, 184, 2232);
            ySpec = pair.ints;
            xValues = pair.xs;
        } else {
            throw new IllegalArgumentException("Unerwartete Dateigröße: " + content.length + " bytes (erwartet: 8424, 4328, 2280)");
        }

        // 2) Header-Infos (exakt gleiche Offsets wie Python)
        int measTimeSec = convertToUShort(content, content.length - 30, content.length - 28).get(0);
        int voltageKv    = convertToUShort(content, content.length - 48, content.length - 46).get(0);
        int currentRaw   = convertToUShort(content, content.length - 46, content.length - 44).get(0); // *1e-2 mA
        double currentmA = currentRaw * 1e-2;

        String name   = convertToCharFiltered(content, 22, 35);
        String filter = convertToCharFiltered(content, content.length - 44, content.length - 35);
        String lv     = convertToCharFiltered(content, content.length - 14, content.length - 8);

        if (info) {
            System.out.println("Messzeit: " + measTimeSec + " sec, Spannung: " + voltageKv + " kV, Strom: " + currentmA + " mA");
            System.out.println("Name: " + name + ", Filter: " + filter + ", L/V: " + lv);
        }

        if (saveSpe) {
            String outPath = outDirOrPrefix + name + ".spe";
            writeSpeFile(outPath, name, measTimeSec, voltageKv, currentmA, filter, lv, ySpec, xValues);
        }

        return new Result(ySpec, xValues, name, measTimeSec, voltageKv, currentmA, filter, lv);
    }

    // -------------------------
    // Result DTO
    // -------------------------
    public static class Result {
        public final int[] ySpec;
        public final double[] xValues;
        public final String name;
        public final int measTimeSec;
        public final int voltageKv;
        public final double currentmA;
        public final String filter;
        public final String lv;

        public Result(int[] ySpec, double[] xValues, String name,
                      int measTimeSec, int voltageKv, double currentmA,
                      String filter, String lv) {
            this.ySpec = ySpec;
            this.xValues = xValues;
            this.name = name;
            this.measTimeSec = measTimeSec;
            this.voltageKv = voltageKv;
            this.currentmA = currentmA;
            this.filter = filter;
            this.lv = lv;
        }
    }

    // -------------------------
    // File IO
    // -------------------------
    private static byte[] readFile(String filePath) throws IOException {
        try (InputStream in = new FileInputStream(filePath)) {
            return in.readAllBytes();
        }
    }

    // -------------------------
    // Conversions (1:1)
    // -------------------------

    // Python: Convert_to_int(binary): unpack('i') in 4-byte chunks, x=i*0.02
    // Implementiert als: Bereich [from, to) aus content (wie Python slicing)
    private static IntDoublePair convertToInt(byte[] content, int from, int toExclusive) {
        int len = toExclusive - from;
        if (len % 4 != 0) {
            throw new IllegalArgumentException("Int-Bereich ist nicht durch 4 teilbar: " + len);
        }

        int n = len / 4;
        int[] ints = new int[n];
        double[] xs = new double[n];

        ByteBuffer bb = ByteBuffer.wrap(content, from, len).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < n; i++) {
            ints[i] = bb.getInt();      // wie struct.unpack('i', ...)
            xs[i] = i ;           // exakt wie Python
        }
        return new IntDoublePair(ints, xs);
    }

    private static class IntDoublePair {
        final int[] ints;
        final double[] xs;
        IntDoublePair(int[] ints, double[] xs) {
            this.ints = ints;
            this.xs = xs;
        }
    }

    // Python: Convert_to_short(binary): unpack('H') -> unsigned short little-endian
    private static List<Integer> convertToUShort(byte[] content, int from, int toExclusive) {
        int len = toExclusive - from;
        if (len % 2 != 0) {
            throw new IllegalArgumentException("UShort-Bereich ist nicht durch 2 teilbar: " + len);
        }

        int n = len / 2;
        List<Integer> out = new ArrayList<>(n);

        ByteBuffer bb = ByteBuffer.wrap(content, from, len).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            int unsigned = Short.toUnsignedInt(bb.getShort()); // entspricht 'H'
            out.add(unsigned);
        }
        return out;
    }

    // Python: Convert_to_char(binary):
    // liest byteweise char und nimmt nur alpha/space/digit
    private static String convertToCharFiltered(byte[] content, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            // Python decode("utf-8") pro Byte ist faktisch: ASCII subset; wir machen das gleich
            char c = (char) (content[i] & 0xFF);

            // Python: buchstabe.isalpha() or isspace() or isdigit()
            if (Character.isLetter(c) || Character.isWhitespace(c) || Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // -------------------------
    // SPE Writer (exakt nach Python)
    // -------------------------

    private static void writeSpeFile(String outPath,
                                     String name,
                                     int measTimeSec,
                                     int voltageKv,
                                     double currentmA,
                                     String filter,
                                     String lv,
                                     int[] ySpec,
                                     double[] xValues) throws IOException {

        // Python: format_number(num, leerzeichen=8) => str(num).rjust(leerzeichen)
        // Für $DATA start/end wird leerzeichen=10 verwendet.
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.UTF_8))) {

            w.write("$SPEC_ID:\n");
            w.write(name);
            w.write("\n");

            w.write("$DATE_MEA:\n");
            // Python: datetime.now().strftime("%m-%d-%Y %H:%M:%S")
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
            w.write(LocalDateTime.now().format(fmt));
            w.write("\n");

            w.write("$MEAS_TIM:\n");
            w.write(rjust(Integer.toString(measTimeSec), 8));
            w.write("\n");

            w.write("$MCA_CAL:\n");
            w.write("3\n");
            w.write("0.000000e+000 2.000000e-002 0.000000e+000\n");

            w.write("$Info:\n");
            w.write("Messzeit:" + measTimeSec + "sec, Spannung:" + voltageKv + "kV, Strom:" + currentmA + "mA\n");
            w.write("Name:" + name + ", Filter:" + filter + ", L/V:" + lv + "\n");

            w.write("$DATA:\n");
            w.write(rjust(Integer.toString(0), 10));
            w.write(rjust(Integer.toString(xValues.length - 1), 10));
            w.write("\n");

            for (int i = 0; i < xValues.length; i++) {
                w.write(rjust(Integer.toString(ySpec[i]), 8));
                // Python: if i % 10 == 0: newline
                // Hinweis: Das macht schon bei i=0 einen Zeilenumbruch (exakt übernehmen!)
                if (i % 10 == 0) {
                    w.write("\n");
                }
            }
        }
    }

    private static String rjust(String s, int width) {
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }



    public static void main(String[] args) {
        String inFile = "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\JAVASPE\\SPECTRUM.118";
        boolean info = true;

        boolean saveSpe = true;
        String outPrefix = "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\JAVASPE\\";

        try {
            // Ordner sicherstellen
            new File(outPrefix).mkdirs();

            Result res = tracorDatenToSpe(inFile, info, saveSpe, outPrefix);
            System.out.println("SPE geschrieben für: " + res.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


