public class Kante {

    public Kante(Schale schale, double energy, double jump) {
        this.shell = schale;
        this.energy = energy;
        this.jump = jump;
    }

    private final Schale shell;
    public Schale getShell() { return shell; };

    private double energy;
    public double getEnergy() { return energy; }

    private double jump;
    public double getJump() { return jump; }

    @Override
    public String toString() {
        return this.shell + ", " + this.energy + ", " + this.jump;
    }
}
