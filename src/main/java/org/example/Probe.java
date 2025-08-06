package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Probe {
    private final List<Element> elemente;
    private final List<List<Übergang>> uebergaengeProElement;
    private final List<Übergang> alleÜbergänge;
    private final double[]  intensitaeten;
    private final double Emin;
    private final double Emax;
    private final double step;
    private final String dateipfad;

    /**
     * Erstellt eine Probe mit Liste von Elementen (z.B. "Cu", "O", ...)
     * und merkt sich die Übergänge zu jedem Element.
     */
    public Probe(List<?> symboleOderZahlen, String dateipfad, double Emin, double Emax, double step, List<? extends Number> intensitaetenListe) {
        elemente = new ArrayList<>();
        uebergaengeProElement = new ArrayList<>();
        alleÜbergänge = new ArrayList<>();
        for (Object symbolObj : symboleOderZahlen) {
            String symbol = symbolObj.toString();
            Element element = new Element(dateipfad, symbol, Emin, Emax, step);
            elemente.add(element);

            List<Übergang> dieseUebergaenge = element.getÜbergänge();
            uebergaengeProElement.add(dieseUebergaenge);
            alleÜbergänge.addAll(dieseUebergaenge);
        }
        if (intensitaetenListe.size() != elemente.size())
            throw new IllegalArgumentException("Intensitäten-Liste und Elemente-Liste müssen gleich lang sein!");
        intensitaeten = new double[elemente.size()];
        for (int i = 0; i < intensitaeten.length; i++)
            intensitaeten[i] = intensitaetenListe.get(i).doubleValue();
        this.Emin = Emin;
        this.Emax = Emax;
        this.step = step;
        this.dateipfad = dateipfad;
    }

    public double [] getIntensitäten(){return intensitaeten;}

    /** Liste aller Elemente */
    public List<Element> getElemente() {
        return elemente;
    }

    /**
     * Parallele Liste:
     * uebergaengeProElement.get(i) gibt alle Übergänge für elemente.get(i)
     */
    public List<List<Übergang>> getUebergaengeProElement() {
        return uebergaengeProElement;
    }

    /** Flache Gesamtliste aller Übergänge */
    public List<Übergang> getAlleÜbergänge() {
        return alleÜbergänge;
    }

    /** Gibt die Übergänge für ein bestimmtes Element zurück */
    public List<Übergang> getUebergaengeFuerElement(Element element) {
        int idx = elemente.indexOf(element);
        if (idx < 0) return Collections.emptyList();
        return uebergaengeProElement.get(idx);
    }

    public List<Integer> getElementZNumbers() {

        List<Integer> zList = new ArrayList<>();

        for (Element element : elemente) {
            int Z = element.getAtomicNumber(); // Typ: List<String[]>
            zList.add(Z);
        }

        return zList;

    }

    public void setzeUebergangAktivFuerElement(int elementIndex, String schaleVon, String schaleZu) {
        if (elementIndex < 0 || elementIndex >= uebergaengeProElement.size()) return;
        List<Übergang> liste = uebergaengeProElement.get(elementIndex);

        for (Übergang u : liste) {
            if (u.getSchale_von().name().equalsIgnoreCase(schaleVon)
                    && u.getSchale_zu().name().equalsIgnoreCase(schaleZu)) {
                u.setAktiv(true);
            }
        }
    }


    public void setzeUebergangAktivFuerElementKAlpha(int elementIndex) {
        if (elementIndex < 0 || elementIndex >= uebergaengeProElement.size()) return;
        List<Übergang> liste = uebergaengeProElement.get(elementIndex);

        for (Übergang u : liste) {
            String von = u.getSchale_von().name();
            String zu  = u.getSchale_zu().name();
            if (von.equalsIgnoreCase("K") && (
                    zu.equalsIgnoreCase("L1") ||
                            zu.equalsIgnoreCase("L2") ||
                            zu.equalsIgnoreCase("L3"))
            ) {
                u.setAktiv(true);
            }
        }
    }

    public void setzeUebergangAktivFuerElementLAlpha(int elementIndex) {
        if (elementIndex < 0 || elementIndex >= uebergaengeProElement.size()) return;
        List<Übergang> liste = uebergaengeProElement.get(elementIndex);

        for (Übergang u : liste) {
            String von = u.getSchale_von().name();
            String zu  = u.getSchale_zu().name();
            if (von.equalsIgnoreCase("L3") && (
                    zu.equalsIgnoreCase("M4") ||
                            zu.equalsIgnoreCase("M5"))
            ) {
                u.setAktiv(true);
            }
        }
    }

    public double [] berechneMittlereEnergieProElement() {
        double [] result = new double [elemente.size()];

        for (int i = 0; i < elemente.size(); i++) {
            List<Übergang> uebers = uebergaengeProElement.get(i);
            double summeRaten = 0.0;
            double summe = 0.0;
            for (Übergang u : uebers) {
                if (u.isAktiv()) {
                    summeRaten += u.getRate();
                    summe += u.getEnergy() * u.getRate();
                }
            }
            if (summeRaten == 0.0)
                result[i]=0.0;
            else
                result[i] = summe / summeRaten;
        }
        return result;
    }

    public Probe filterByMinZ(int minZ) {
        List<Element> filteredElements = new ArrayList<>();
        List<Double> filteredIntensitaeten = new ArrayList<>();
        List<List<Boolean>> aktiveUebergaengeVorher = new ArrayList<>();

        // Speichere die Aktiv-Status für jeden Übergang jedes Elements
        for (int i = 0; i < elemente.size(); i++) {
            List<Übergang> uebergListe = uebergaengeProElement.get(i);
            List<Boolean> aktivFlags = new ArrayList<>();
            for (Übergang u : uebergListe) {
                aktivFlags.add(u.isAktiv());
            }
            aktiveUebergaengeVorher.add(aktivFlags);
        }

        // Filtere und merke Original-Indizes!
        List<Integer> neueIndizes = new ArrayList<>();
        for (int i = 0; i < elemente.size(); i++) {
            Element el = elemente.get(i);
            if (el.getAtomicNumber() >= minZ) {
                filteredElements.add(el);
                filteredIntensitaeten.add(intensitaeten[i]);
                neueIndizes.add(i);  // Merke alten Index
            }
        }

        // Neue Symbolliste
        List<String> symbole = filteredElements.stream()
                .map(Element::getSymbol)
                .toList();

        // Erstelle die neue Probe
        Probe neueProbe = new Probe(
                symbole,        // neue Symbolliste
                dateipfad,
                Emin, Emax, step,
                filteredIntensitaeten
        );

        // Setze die aktiven Übergänge in der neuen Probe:
        for (int j = 0; j < neueIndizes.size(); j++) {
            int altIndex = neueIndizes.get(j);
            List<Boolean> aktivFlags = aktiveUebergaengeVorher.get(altIndex);
            List<Übergang> neueListe = neueProbe.getUebergaengeProElement().get(j);

            // Setze den Aktiv-Status wie im Original (bis zur Länge der neuen Liste)
            for (int k = 0; k < Math.min(aktivFlags.size(), neueListe.size()); k++) {
                neueListe.get(k).setAktiv(aktivFlags.get(k));
            }
        }
        return neueProbe;
    }





    public double getEmin(){return Emin;}
    public double getEmax(){return Emax;}
    public double getStep(){return step;}
    public String getDateipfad(){return dateipfad;}
    public double [] getIntensitaeten(){return intensitaeten;}





}

