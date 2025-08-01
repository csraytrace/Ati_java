import java.util.Arrays;
import java.util.List;

public class Daten {
    private String symbol;
    private int atomicNumber;
    private double atomicWeight;
    private double density;
    private double cm2g;
    private List<Kante> kanten;
    private List<Übergang> ubergange;
    private double[] jumps;
    private List<double[]> mcMaster;
    private List<String[]> omega;
    private List<Double> costa;

    public Daten(String symbol, int atomicNumber, double atomicWeight, double density,
                 double cm2g, List<Kante> kanten, List<Übergang> ubergange,
                 double[] jumps, List<double[]> mcMaster, List<String[]> omega, List<Double> costa) {
        this.symbol = symbol;
        this.atomicNumber = atomicNumber;
        this.atomicWeight = atomicWeight;
        this.density = density;
        this.cm2g = cm2g;
        this.kanten = kanten;
        this.ubergange = ubergange;
        this.jumps = jumps;
        this.mcMaster = mcMaster;
        this.omega = omega;
        this.costa = costa;
    }

    // Hilfsmethoden zur Ausgabe von Listen von Arrays
    private String listOfObjectArraysToString(List<Object[]> list) {
        StringBuilder sb = new StringBuilder();
        for (Object[] arr : list) {
            sb.append(Arrays.toString(arr)).append(" ");
        }
        return sb.toString();
    }

    private String listOfDoubleArraysToString(List<double[]> list) {
        StringBuilder sb = new StringBuilder();
        for (double[] arr : list) {
            sb.append(Arrays.toString(arr)).append(" ");
        }
        return sb.toString();
    }

    private String listOfStringArraysToString(List<String[]> list) {
        StringBuilder sb = new StringBuilder();
        for (String[] arr : list) {
            sb.append(Arrays.toString(arr)).append(" ");
        }
        return sb.toString();
    }

    // Getter-Methoden:
    public String getSymbol() {
        return symbol;
    }

    public int getAtomicNumber() {
        return atomicNumber;
    }

    public double getAtomicWeight() {
        return atomicWeight;
    }

    public double getDensity() {
        return density;
    }

    public double getCm2g() {
        return cm2g;
    }

    public List<Kante> getKanten() {
        return kanten;
    }

    public List<Übergang> getUbergange() {
        return ubergange;
    }

    public double[] getJumps() {
        return jumps;
    }

    public List<double[]> getMcMaster() {
        return mcMaster;
    }

    public List<String[]> getOmega() {
        return omega;
    }

    public List<Double> getCosta() {
        return costa;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol: ").append(symbol).append("\n");
        sb.append("Atomic Number: ").append(atomicNumber).append("\n");
        sb.append("Atomic Weight: ").append(atomicWeight).append("\n");
        sb.append("Density: ").append(density).append("\n");
        sb.append("cm2g: ").append(cm2g).append("\n");
        sb.append("Kanten: ").append(Arrays.toString(kanten.toArray())).append("\n");
        sb.append("Übergange: ").append(Arrays.toString(ubergange.toArray())).append("\n");
        sb.append("Jumps: ").append(Arrays.toString(jumps)).append("\n");
        sb.append("mcMaster: ").append(listOfDoubleArraysToString(mcMaster)).append("\n");
        sb.append("Omega: ").append(listOfStringArraysToString(omega)).append("\n");
        sb.append("Costa: ").append(costa.toString()).append("\n");
        return sb.toString();
    }
}