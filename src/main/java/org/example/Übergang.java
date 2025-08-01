package org.example;

public class Übergang {

    private boolean aktiv = false;

    public Übergang(Schale schale_von, Schale schale_zu, double energy, double rate) {
        this(schale_von, schale_zu, energy, rate, false);
    }

    //rate ist die Übergangswahrscheinlichkeit, aber es kann auch stattedessen die Intensität des Übergangs sein, rate oder intensität

    public Übergang(Schale schale_von, Schale schale_zu, double energy, double rate, boolean aktiv) {
        this.schale_von = schale_von;
        this.schale_zu = schale_zu;
        this.energy = energy;
        this.rate = rate;
        this.aktiv = aktiv;
    }

    private final Schale schale_von;
    public Enum getSchale_von(){return schale_von;}
    private final Schale schale_zu;
    public Enum getSchale_zu(){return schale_zu;}
    public String getÜbergang() {return schale_von + "-"+this.schale_zu;}
    private double energy;
    public double getEnergy() { return energy; }
    private double rate;
    public double getRate() { return rate; }

    @Override
    public String toString() {
        return this.schale_von + "-"+this.schale_zu+", " + this.energy + ", " + this.rate;
    }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }


}
