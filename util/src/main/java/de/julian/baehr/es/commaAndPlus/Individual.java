package de.julian.baehr.es.commaAndPlus;

public class Individual {

	double[] objectVariable, standardDeviations;
	double fitness;
	
	public int getVectorSize(){
		return objectVariable.length;
	}
	
	public void calculateFitness(IObjectiveFunction objectiveFunction){
		fitness = objectiveFunction.calculateFitness(this);
	}
}
