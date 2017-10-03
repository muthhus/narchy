package no.birkett.kiwi;

/**
 * Created by alex on 30/01/15.
 */
public class Variable {

    public final String name;

    private double value;

    public Variable(String name) {
        this.name = name;
    }

    public double value() {
        return value;
    }

    public void value(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
