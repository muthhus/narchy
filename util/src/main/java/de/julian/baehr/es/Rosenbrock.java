package de.julian.baehr.es;

/**
 * programmed after: https://en.wikipedia.org/wiki/Rosenbrock_function
 * @author Julian Sven Baehr
 *
 */
public class Rosenbrock implements IObjectiveFunction{

	@Override
	public double evaluate(float[] solution) {
		double fitness = 0;
		
		for(int i = 0; i < solution.length -1; i++)
			fitness += 100*Math.pow(solution[i+1] - Math.pow(solution[i], 2), 2) + Math.pow(1 - solution[i], 2);
		
		return fitness;
	}

	
}
