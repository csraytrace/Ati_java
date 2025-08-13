package org.example;

import java.util.ArrayList;
import java.util.List;

public class LoveScottRöhre extends RöhreBasis {

    public LoveScottRöhre(
            double Einfallswinkelalpha,
            double Einfallswinkelbeta,
            double Fensterwinkel,
            double charzucont,
            double charzucontL,
            double fensterDickeUmikron,
            double raumWinkel,
            double roehrenStrom,
            double Emin,
            double Emax,
            double sigmaConst,
            double step,
            double messZeit,
            String roehrenMaterial,
            String fensterMaterial,
            String folderPath,
            List<Verbindung> Filter
    ) {
        super(
                roehrenMaterial,
                Einfallswinkelalpha,
                Einfallswinkelbeta,
                Fensterwinkel,
                charzucont,
                charzucontL,
                fensterMaterial,
                fensterDickeUmikron,
                raumWinkel,
                roehrenStrom,
                Emin,
                Emax,
                sigmaConst,
                step,
                messZeit,
                folderPath,
                Filter
        );
        prepareData();
    }

    @Override
    protected double getSigma(double E) {
        double Z = roehrenMaterial.Z_gemittelt();
        double U = Emax / E;
        //double exp = 1.109 - 0.00435 * Z + 0.00175 * Emax;
        double exp = sigmaConst - 0.00435 * Z + 0.00175 * Emax;
        return 1.35e9 * Z * Math.pow(U - 1, exp);
    }


    protected  List<Double> RzuS_j(){

        List<Double> listeappends = new ArrayList<>();
        List<List<Double>> energieListe =  getEnergienVonKanten();
        List<Double> Z_liste = roehrenMaterial.getZ_List();

        for (int i = 0; i < energieListe.size(); i++) {
            List<Double> energieProElement = energieListe.get(i);
            for (int j = 0; j < energieProElement.size(); j++) {
                double S_energie = energieProElement.get(j);
                double U = Emax / S_energie;
                double J = 0.0135 * Z_liste.get(i);
                double lnU = Math.log(U);
                // Kanten sind immer K1, L1, L2, L3
                double z, b;
                if (j == 0) {
                    z = 2;
                    b = 0.35;
                } else {
                    z = 8;
                    b = 0.25;
                }
                double klammer = 1 + 15.5 * Math.sqrt(J / S_energie) *
                        (Math.sqrt(U) * lnU + 2 * (1 - Math.sqrt(U))) / (U * lnU + 1 - U);

                double intensitaetsfaktor = z * b / Z_liste.get(i) * (U * lnU + 1 - U) * klammer;

                double R = 1 - 0.008152 * Z_liste.get(i) + 3.613e-5 * Math.pow(Z_liste.get(i), 2) + 0.009582 * Z_liste.get(i) * Math.exp(-U) + 0.00114 * Emax ;

                double listappend = 0;

                if (S_energie > 0 && Emax > S_energie) {
                    listappend = intensitaetsfaktor * R;
                }

                listeappends.add(listappend);
            }
        }
        return listeappends;}


    public static void main(String[] args) {

    }

}
