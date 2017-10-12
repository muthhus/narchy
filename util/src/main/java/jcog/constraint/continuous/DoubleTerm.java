package jcog.constraint.continuous;

/**
 * Created by alex on 30/01/15.
 */
public class DoubleTerm {

    public final DoubleVar var;
    double coefficient;

    public DoubleTerm(DoubleVar var, double coefficient) {
        this.var = var;
        this.coefficient = coefficient;
    }

    public DoubleTerm(DoubleVar var) {
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
