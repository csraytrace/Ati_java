package org.example;

public class probieren1 {

    public static void main(String[] args) {
        Funktionen f = new FunktionenImpl();
        double Emin = 0;
        double Emax = 35;
        double step = 5;
        String ver1 = "Ti";
        String ver2 = " TiO2";
        //String ver3 = "1 Al2O3 + 1 TiO2";
        String ver3 = "Al2O5Ti";


        Verbindung v1 = f.parseVerbindung(ver1,Emin, Emax, step, "McMaster.txt");
        Verbindung v2 = f.parseVerbindung(ver2,Emin, Emax, step, "McMaster.txt");
        Verbindung v3 = f.parseVerbindung(ver3,Emin, Emax, step, "McMaster.txt");
        //System.out.println(v1.getDichte()+", ");
        System.out.println(v3.getWeight()+", ");
        System.out.println(v2.getWeight() / v1.getWeight() );
        double[] konz = v3.getKonzentrationen();
        String[] symbol = v3.getSymbole();
        for (int i = 0; i < konz.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol[i], konz[i]);
        }
/*
        double[] konz1 = v1.getKonzentrationen();
        String[] symbol1 = v1.getSymbole();
        for (int i = 0; i < konz1.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol1[i], konz1[i]);
        }

        double[] konz2 = v2.getKonzentrationen();
        String[] symbol2 = v2.getSymbole();
        for (int i = 0; i < konz2.length; i++) {
            System.out.printf("Konzentration Komponente %s: %.6f%n", symbol2[i], konz2[i]);
        }
*/




    }



}

