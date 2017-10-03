package no.birkett.kiwi;

import jcog.list.FasterList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alex on 30/01/15.
 */
public class Constraint {

    public final Expression expression;
    private double strength;
    public final RelationalOperator op;

    public Constraint(Expression expr, RelationalOperator op) {
        this(expr, op, Strength.REQUIRED);
    }

    public Constraint(Expression expr, RelationalOperator op, double strength) {
        this.expression = reduce(expr);
        this.op = op;
        this.strength = Strength.clip(strength);
    }

    public Constraint(Constraint other, double strength) {
        this(other.expression, other.op, strength);
    }

    private static Expression reduce(Expression expr){

        Map<Variable, Double> vars = new LinkedHashMap<>();
        for(Term term: expr.terms){
            vars.merge(term.var, term.coefficient, (vv, val)-> val + vv);
        }

        List<Term> reducedTerms = new FasterList<>(vars.size());
        for(Map.Entry<Variable, Double> variableDoubleEntry : vars.entrySet()){
            reducedTerms.add(new Term(variableDoubleEntry.getKey(), variableDoubleEntry.getValue()));
        }

        return new Expression(reducedTerms, expr.getConstant());
    }

    public double getStrength() {
        return strength;
    }

    public Constraint setStrength(double strength) {
        this.strength = strength;
        return this;
    }

    @Override
    public String toString() {
        return "expression: (" + expression + ") strength: " + strength + " operator: " + op;
    }

}
