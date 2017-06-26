package de.julian.baehr.es;

@FunctionalInterface
public interface IObjectiveFunction {

	double evaluate(float[] solution);
}
