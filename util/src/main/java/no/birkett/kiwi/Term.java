package no.birkett.kiwi;

/**
 * Created by alex on 30/01/15.
 */
public class Term {

    public final Variable var;
    double coefficient;

    public Term(Variable var, double coefficient) {
        this.var = var;
        this.coefficient = coefficient;
    }

    public Term(Variable var) {
        this(var, 1.0);
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public double value() {
        return coefficient * var.value();
    }

    @Override
    public String toString() {
        return "variable: (" + var + ") coefficient: "  + coefficient;
    }
}
