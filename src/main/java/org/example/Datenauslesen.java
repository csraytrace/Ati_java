package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Datenauslesen {

    // Feld zum Speichern der verarbeiteten Daten
    private List<String[]> daten;
    private List<double[]> mcMaster;
    private List<Kante> kantenarray;


    // Hilfsmethode: entfernt Leerzeichen im übergebenen String
    public static String removeSpaces(String input) {
        return input.replace(" ", "");
    }

    // Methode, zur Datenerstellung
    public void datenerstellen(String dateipfad, String element) {
        // Versuch, den Elementnamen richtig zu formatieren, z.B. Elementname = "H" oder "1":
        String elementName;
        if (element != null && element.length() > 0) {
            elementName = element.substring(0, 1).toUpperCase() + element.substring(1).toLowerCase();
        } else {
            elementName = element;
        }

        // Liste zum Zwischenspeichern der Zeilen (jede Zeile als String-Array)
        List<String[]> liste = new ArrayList<>();

        // Datei einlesen
        try (BufferedReader br = new BufferedReader(new FileReader(dateipfad))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Zeile trimmen (entfernt führende und endende Leerzeichen)
                line = line.trim();
                // Entferne abschließendes Komma (falls vorhanden)
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
                // Zeile anhand von ":" aufteilen
                String[] parts = line.split(":");
                liste.add(parts);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Suche nach dem passenden Index: index_i
        int index_i = 0;
        for (int index = 0; index < liste.size(); index++) {
            String[] value = liste.get(index);
            // Sicherstellen, dass es mindestens zwei Teile gibt
            if (value.length >= 2) {
                if ("ELEMENT SYMBOL".equals(value[0]) && removeSpaces(value[1]).equals(elementName)) {
                    index_i = index;
                    break;
                }
                if ("ATOMIC NUMBER".equals(value[0]) && removeSpaces(value[1]).equals(elementName)) {
                    index_i = index - 1;
                    break;
                }
            }
        }

        // Ermitteln, wie viele Zeilen (i) ab index_i verarbeitet werden sollen,
        // bis der Eintrag mit "---" im ersten Teil gefunden wird.
        int i = 0;
        for (; (index_i + i) < liste.size(); i++) {
            String[] v = liste.get(index_i + i);
            if (v.length > 0 && v[0].length() >= 3 && v[0].substring(0, 3).equals("---")) {
                break;
            }
        }

        // Speichern des relevanten Ausschnitts in dem Feld "daten".
        // Hier wird ein neuer ArrayList erstellt, der den relevanten Teil der Liste enthält.
        this.daten = new ArrayList<>(liste.subList(index_i, index_i + i));
        this.mcMasterVorbereiten();
        this.kantenVorbereiten();
    }

    // Getter-Methoden

    // Gibt das gesamte Daten-Array zurück
    public List<String[]> getDaten() {
        return daten;
    }

    // Gibt das Element-Symbol zurück
    public String getElementsymbol() {
        return removeSpaces(daten.get(0)[1]);
    }

    // Gibt die Atomnummer als int zurück
    public int getAtomicnumber() {
        return Integer.parseInt(removeSpaces(daten.get(1)[1]));
    }

    // Gibt das Atomgewicht als double zurück
    public double getAtomicweight() {
        return Double.parseDouble(removeSpaces(daten.get(2)[1]));
    }

    // Gibt die Dichte als double zurück
    public double getDensity() {
        return Double.parseDouble(removeSpaces(daten.get(3)[1]));
    }

    // Berechnet und gibt den Wert von cm2g zurück
    public double getCm2g() {
        return 0.602214179 / getAtomicweight();
    }

    // Methode zum Umformen einer String-Liste in ein double-Array
    public static double[] umformen(List<String> liste) {
        double[] arr = new double[liste.size()];
        for (int i = 0; i < liste.size(); i++) {
            try {
                arr[i] = Double.parseDouble(liste.get(i));
            } catch (NumberFormatException e) {
                System.out.println(liste + " ist kein Float");
                return new double[0];
            }
        }
        return arr;
    }


    // Methode zum Vorbereiten der McMaster-Daten
    public int mcMasterVorbereiten() {
        // 1. Finde den Index i, ab dem "MCMASTER" in der ersten Spalte auftaucht.
        int i = 0;
        for (int index = 0; index < daten.size(); index++) {
            String[] row = daten.get(index);
            if (row.length > 0 && row[0].length() >= 8 && row[0].substring(0, 8).equals("MCMASTER")) {
                i = index;
                break;
            }
        }

        // 2. Erstelle die Zwischenliste: Für i2 von 0 bis 5, jeweils eine Liste von 4 Werten.
        List<List<String>> mcmasterIntermediate = new ArrayList<>();
        for (int i2 = 0; i2 < 6; i2++) {
            List<String> liste = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                int rowIndex = i + 1 + j;
                String[] row = daten.get(rowIndex);
                if (row.length >= 2) {
                    String colStr = row[1];
                    String[] parts = colStr.split(" ");
                    List<String> filtered = new ArrayList<>();
                    for (String part : parts) {
                        if (!part.equals("")) {
                            filtered.add(part);
                        }
                    }
                    if (i2 < filtered.size()) {
                        liste.add(filtered.get(i2));
                    } else {
                        liste.add("0");
                    }
                } else {
                    liste.add("0");
                }
            }
            mcmasterIntermediate.add(liste);
        }

        // 3. Wandle jede innere Liste mit der Umformen-Methode in ein double[] um
        this.mcMaster = new ArrayList<>();
        for (List<String> inner : mcmasterIntermediate) {
            this.mcMaster.add(umformen(inner));
        }

        return 0;
    }

    // Getter für McMaster
    public List<double[]> getMcMaster() {
        return mcMaster;
    }

    // Sie gibt ein Array der Länge 21 zurück, welches standardmäßig mit 1.0 befüllt wird.
    // Wird der Bereich zwischen " K-EDGE JUMP" und "MCMASTER" gefunden, werden dort die Werte aus den Daten eingesetzt.
    public double[] jumps() {
        // Erstelle ein Array mit 21 Elementen, initial alle Einsen
        double[] array = new double[21];
        for (int k = 0; k < array.length; k++) {
            array[k] = 1.0;
        }

        int index_i = 0;
        int index_j = 0;

        // Suche in der Liste 'daten' nach den relevanten Zeilen
        for (int i = 0; i < daten.size(); i++) {

            String[] row = daten.get(i);
            //System.out.println(Arrays.toString(row));
            if (row.length > 0) {
                if (row[0].equals("K-EDGE JUMP")) {
                    index_i = i;
                }
                if (row[0].length() >= 8 && row[0].substring(0, 8).equals("MCMASTER")) {
                    index_j = i;
                }
            }
        }

        // Falls kein K-EDGE JUMP gefunden wurde, wird das Array unverändert (alle Einsen) zurückgegeben.
        if (index_i == 0) {
            return array;
        } else {
            // Bestimme die Länge des Ausschnitts: in Python entspricht len(self.Daten[index_i:index_j])
            int sliceLength = index_j - index_i;
            for (int i = 0; i < sliceLength && i < array.length; i++) {
                try {
                    array[i] = Double.parseDouble(removeSpaces(daten.get(index_i + i)[1]));
                } catch (NumberFormatException e) {
                    // Bei Umwandlungsfehlern bleibt der Standardwert 1.0
                    array[i] = 1.0;
                }
            }
            return array;
        }
    }

    // Methode zum Vorbereiten der Kanten
    public int kantenVorbereiten() {
        // Suche nach dem Index in 'daten', bei dem der Eintrag in der ersten Spalte mit "K/" beginnt.
        int index_i = -1;
        for (int i = 0; i < daten.size(); i++) {
            String[] row = daten.get(i);
            if (row.length > 0 && row[0].length() >= 3 && row[0].substring(0, 2).equals("K/")) {
                index_i = i;
                //System.out.print(Arrays.toString(row));
                //System.out.print(row.length);
                break;
            }
        }

        if (index_i == -1) {
            System.out.println("Keine Zeile gefunden, die mit \"K/\" beginnt.");
            return -1;
        }

        // Rufe die Methode jumps() auf, die ein double[] der Länge 21 zurückgibt.
        double[] jumpsArray = jumps();

        // Erstelle die Liste, in der jedes Element ein Object[] mit drei Werten ist:
        // [0]: Der erste Teil, den wir aus der Zeile extrahieren (aufgeteilt an "/")
        // [1]: Der zweite Teil (als double konvertiert)
        // [2]: Der entsprechende Wert aus jumpsArray
        List<Kante> retList = new ArrayList<>();
        // Wir gehen hier davon aus, dass ab index_i in daten mindestens 21 Zeilen vorhanden sind.
        for (int i = 0; i < 21; i++) {
            // Hole die Zeile
            String[] currentRow = daten.get(index_i + i);
            // Wir zerlegen den Inhalt der ersten Spalte anhand von "/"
            String[] parts = currentRow[0].split("/");

            String firstPart = parts[0];
            double secondPart = 0.0;
            if (parts.length > 1) {
                try {
                    secondPart = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    // Falls das Parsen fehlschlägt, setzen wir den Wert auf 0.
                    secondPart = 0.0;
                }
            }
            double thirdPart = jumpsArray[i];

            // Erstelle ein Object-Array mit den drei Werten
            Kante kantenElement = new Kante(Schale.valueOf(firstPart), secondPart, thirdPart);
            retList.add(kantenElement);
        }

        // Speichere die erstellte Liste in dem Feld kantenarray.
        this.kantenarray = retList;
        return 0;
    }

    public List<Kante> getKanten() {
        return this.kantenarray;
    }

    public List<Übergang> übergange() {
        int anfang = 0;
        int ende1 = 200;
        int ende2 = 200;

        // Durchlaufe die Datenzeilen und bestimme anfang, ende1 und ende2.
        for (int i = 0; i < daten.size(); i++) {
            String[] row = daten.get(i);
            //System.out.print(row[0]+"\n");
            if (row.length > 0) {
                // Prüfe, ob die Zeile mit "SIEGBAHN" beginnt (erste 8 Zeichen)
                if (row[0].length() >= 8 && row[0].substring(0, 8).equals("SIEGBAHN")) {
                    anfang = i + 1;  // ab der nächsten Zeile beginnen
                }
                // Prüfe auf den Beginn mit " K-EDGE" (erste 7 Zeichen)
                if (row[0].length() >= 6 && row[0].substring(0, 6).equals("K-EDGE")) {
                    ende1 = i;
                }
                // Prüfe auf den Beginn mit "MCMASTER" (erste 8 Zeichen)
                if (row[0].length() >= 8 && row[0].substring(0, 8).equals("MCMASTER")) {
                    ende2 = i;
                }
            }
        }
        //System.out.printf("%d %d %d", anfang, ende1, ende2);

        // Falls kein gültiger Startpunkt gefunden wurde, gib eine Liste mit einem leeren Array zurück.
        /*
        if (anfang == 0) {
            List<Object[]> retlist = new ArrayList<>();
            retlist.add(new Object[0]);
            return 0;
        }*/

        // Berechne die Anzahl der relevanten Zeilen, basierend auf dem Minimum der beiden End-Indizes
        int count = Math.min(ende1, ende2) - anfang;
        List<Übergang> retlist = new ArrayList<>();

        // Iteriere über die Zeilen von anfang bis anfang+count
        for (int i = 0; i < count; i++) {
            // Holen Sie sich die Zeile (wir nehmen an, dass der relevante Inhalt in der ersten Spalte steht)
            String currentValue = daten.get(anfang + i)[0];
            // Spalte die Zeile anhand von "/" auf
            String[] parts = currentValue.split("/");

            // Sicherstellen, dass ausreichend Teile vorhanden sind (mindestens 5 Teile: Indizes 0 bis 4)
            if (parts.length > 4) {
                double value4 = 0;
                try {
                    value4 = Double.parseDouble(parts[4]);
                } catch (NumberFormatException e) {
                    value4 = 0;
                }
                // Nur hinzufügen, wenn der Wert an Index 4 ungleich 0 ist
                if (value4 != 0) {
                    String part1 = parts[1];  // zweites Element (Index 1)
                    double part2 = 0;
                    try {
                        part2 = Double.parseDouble(parts[2]);  // drittes Element (Index 2)
                    } catch (NumberFormatException e) {
                        part2 = 0;
                    }

                    Übergang Übergang_Elment = new Übergang(Schale.valueOf(removeSpaces(part1.substring(0, 2))), Schale.valueOf(part1.substring(3, 5)), part2, value4);
                    retlist.add(Übergang_Elment);
                }
            }
        }

        return retlist;
    }

    public List<Übergang> getUbergange() {
        return übergange();
    }

    public List<String[]> omega() {
        // Bestimme den Index i, an dem in der ersten Spalte "Omega-K" vorkommt.
        int i = 0;
        for (int index = 0; index < daten.size(); index++) {
            String[] row = daten.get(index);
            // Sicherstellen, dass die Zeile mindestens 7 Zeichen im ersten Element hat, bevor substring() verwendet wird
            if (row.length > 0 && row[0].length() >= 7 && row[0].substring(0, 7).equals("Omega-K")) {
                i = index;
                break;
            }
        }

        // Erstelle eine Liste, in die 4 aufeinanderfolgende Zeilen ab Index i eingefügt werden
        List<String[]> omega = new ArrayList<>();
        for (int neu_i = 0; neu_i < 4; neu_i++) {
            // Prüfen, ob i + neu_i innerhalb der Grenzen der Liste liegt
            if (i + neu_i < daten.size()) {
                omega.add(daten.get(i + neu_i));
            }
        }

        return omega;
    }

    // Methode Omega_Schale übersetzt den Python-Code:
    public double omegaSchale(String schale) {
        List<String[]> omegas = omega();
        // Wenn Schale (ohne Leerzeichen) nur ein Zeichen lang ist,
        // gebe den Wert aus dem zweiten Element der ersten Omega-Zeile zurück.
        if (schale.replace(" ", "").length() == 1) {
            return Double.parseDouble(omegas.get(0)[1]);
        } else {
            // Andernfalls durchlaufe alle Omega-Zeilen und suche nach einem passenden Eintrag.
            for (String[] row : omegas) {
                if (row[0].length() >= 6 && row[0].substring(6).equals(schale)) {
                    return Double.parseDouble(row[1]);
                }
            }
        }
        return 0.0;
    }


    public List<Double> costaKronig() {
        List<Double> result = new ArrayList<>();
        // Berechne den Startindex, sodass die letzten 5 Elemente ausgewählt werden.
        int start = Math.max(0, daten.size() - 5);
        // Iteriere über die letzten 5 Zeilen der Liste "daten"
        for (int i = start; i < daten.size(); i++) {
            String[] currentRow = daten.get(i);
            if (currentRow.length >= 2) {
                try {
                    // Entferne mögliche Leerzeichen und parse den String als double.
                    double value = Double.parseDouble(removeSpaces(currentRow[1]));
                    result.add(value);
                } catch (NumberFormatException e) {
                    // Falls das Parsen fehlschlägt, können Sie hier mit einem Default-Wert umgehen,
                    // z. B. 0.0 hinzufügen oder den Fehler weiterleiten.
                    result.add(0.0);
                }
            }
        }
        return result;
    }

    public Daten getErgebnis() {
        String symbol = getElementsymbol();
        int number = getAtomicnumber();
        double weight = getAtomicweight();
        double density = getDensity();
        double cm2g = getCm2g();
        List<Kante> kanten = getKanten();
        List<Übergang> ubergange = getUbergange();
        double[] jumps = jumps();
        List<double[]> mcmaster = getMcMaster();
        List<String[]> omega = omega();
        List<Double> costa = costaKronig();

        return new Daten(symbol, number, weight, density, cm2g,
                kanten, ubergange, jumps, mcmaster, omega, costa);
    }


    public static void main(String[] args) {
        Datenauslesen da = new Datenauslesen();
        //String ergebnis = da.ladeUndGibErgebnisAlsString("McMaster.txt", "30");
        //System.out.println(ergebnis);
        da.datenerstellen("McMaster.txt", "Fe");

        Daten ergebnis = da.getErgebnis();
        System.out.println("\n--- Gesamtergebnis ---");
        System.out.println(ergebnis);

        List<String[]> omegaRows = da.omega();


        // --- Test der Kanten ---
        System.out.println("\n--- Kanten-Test ---");
        List<Kante> kanten = ergebnis.getKanten();
        int index = 0;
        for (Kante k : kanten) {
            // Beispiel für Ausgabe: Schale, Wert1, Wert2
            // Falls du Getter hast: k.getSchale(), k.getWert1(), k.getWert2()
            // Falls du public Felder hast: k.schale, k.wert1, k.wert2
            System.out.println(k.toString());
            System.out.printf("Kante %2d: Schale = %s, Wert1 = %.6f, Wert2 = %.6f\n",
                    index,
                    k.getShell(),
                    k.getJump(),
                    k.getEnergy()
            );
            index++;
        }


        System.out.println("Ergebnis der omega()-Methode:");
        for (String[] row : omegaRows) {
            // Verwende Arrays.toString, um den Inhalt des Arrays lesbar auszugeben
            System.out.println(Arrays.toString(row));


        }


    }


/*
    // Testprogramm: Liest die Datei ein, bereitet die Daten vor und gibt das "jumps"-Array aus.
    public static void main(String[] args) {

        Datenauslesen da = new Datenauslesen();


        // Beispiel: Datei "McMaster.txt" einlesen und mit einem Element (z.B. "3") arbeiten.
        da.datenerstellen("McMaster.txt", "30");

        Ergebnis ergebnis = da.getErgebnis();
        System.out.println("\n--- Gesamtergebnis ---");
        System.out.println(ergebnis);
        List<String[]> omegaRows = da.omega();
        List<Double> costaKronigValues = da.costaKronig();

        // Ausgabe der Ergebnisse
        System.out.println("Costa_Kronig Werte (letzte 5 Zeilen):");
        for (Double d : costaKronigValues) {
            System.out.println(d);
        }

        // Ausgabe der zurückgegebenen Zeilen
        System.out.println("Ergebnis der omega()-Methode:");
        for (String[] row : omegaRows) {
            // Verwende Arrays.toString, um den Inhalt des Arrays lesbar auszugeben
            System.out.println(Arrays.toString(row));
        }
        System.out.println("Omega-Zeilen:");
        for (String[] row : omegaRows) {
            System.out.println(Arrays.toString(row));
        }

        // Testaufruf der Methode omegaSchale:
        // Beispiel 1: Wenn Schale nur ein Zeichen ist (z.B. "K") wird der Wert aus der ersten Omega-Zeile genutzt.
        double wertEinzel = da.omegaSchale("K");
        System.out.println("omegaSchale(\"K\") = " + wertEinzel);

        // Beispiel 2: Wenn Schale länger ist (z.B. "KL"), erfolgt die Suche in den Omega-Zeilen.
        double wertMehr = da.omegaSchale("L1");
        System.out.println("omegaSchale(\"L1\") = " + wertMehr);

        List<Object[]> ubergangeList = da.übergange();

        // Ausgabe der Übergänge:
        System.out.println("Übergänge:");
        for (int i = 0; i < ubergangeList.size(); i++) {
            Object[] uebergang = ubergangeList.get(i);
            // Wir erwarten hier: [String, double, double]
            System.out.print("Zeile " + i + ": ");
            if (uebergang.length > 0) {
                System.out.print("Element: " + uebergang[0] + " | ");
            }
            if (uebergang.length > 1) {
                System.out.print("Wert2: " + uebergang[1] + " | ");
            }
            if (uebergang.length > 2) {
                System.out.print("Wert3: " + uebergang[2]);
            }
            System.out.println();
        }

        // Ausgabe des Kantenarrays
        List<Object[]> kantenData = da.getKanten();
        System.out.println("Kantenarray:");
        for (int i = 0; i < kantenData.size(); i++) {
            Object[] element = kantenData.get(i);
            // Annahme: element[0] ist ein String, element[1] und element[2] sind doubles.
            System.out.println("Zeile " + i + ": "
                    + element[0] + " | "
                    + element[1] + " | "
                    + element[2]);
        }
        // Hinweis: Stellen Sie sicher, dass "McMaster.txt" im aktuellen Arbeitsverzeichnis liegt.


        // Aufruf der jumps()-Methode
        double[] jumpsArray = da.jumps();

        // Ausgabe des jumps-Arrays
        System.out.println("Jumps-Array:");
        for (int i = 0; i < jumpsArray.length; i++) {
            System.out.println("Index " + i + ": " + jumpsArray[i]);

        }

        List<double[]> mcMasterData = da.getMcMaster(); // oder getMcMaster()
        for (int idx = 0; idx < mcMasterData.size(); idx++) {
            System.out.print("Zeile " + idx + ": ");
            double[] arr = mcMasterData.get(idx);
            for (double value : arr) {
                System.out.print(value + " ");
            }
            System.out.println();}
    }*/



    public String ladeUndGibErgebnisAlsString(String dateipfad, String element) {
        datenerstellen(dateipfad, element); // lädt und verarbeitet die Datei
        Daten ergebnis = getErgebnis();
        return ergebnis.toString();         // gibt den Ergebnis-String zurück
    }






}


