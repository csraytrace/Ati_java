package org.example;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DemoAsr {
    public static void main(String[] args) throws Exception {
        Path file = Path.of("C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Atiquant_neck\\BGQXRFPN\\BGQXRFPN\\ASR\\1107-01.asr");

        // Peaks lesen
        List<AsrParser.FitResult> peaks = AsrParser.extractPeaks(file);

        // Gruppieren nach Element
        Map<String, List<AsrParser.FitResult>> grouped = AsrParser.groupByElement(peaks);

        // K/L-Summen (wie bei deinem FIT-Parser)
        Map<String, int[]> counts = AsrParser.getCountsPerElement(grouped);

        // Ausgabe
        counts.forEach((ele, arr) ->
                System.out.printf("%s: K=%d, L=%d%n", ele, arr[0], arr[1]));
    }
}
