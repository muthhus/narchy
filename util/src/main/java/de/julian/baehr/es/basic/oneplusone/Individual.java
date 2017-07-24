package de.julian.baehr.es.basic.oneplusone;


public class Individual {

	private final double[] objectiveParameterVector;
	
	private double fitness;
	
	public Individual(double[] objectiveParameterVector){
		this.objectiveParameterVector = objectiveParameterVector;
	}
	
	public void calculateFitness(IObjectiveFunction objectiveFunction){
		fitness = objectiveFunction.getFitness(this);
	}
	
	public double getFitness(){
		return fitness;
	}
	
	public int getVectorSize(){
		return objectiveParameterVector.length;
	}

	public double getObjectiveParameter(int index) {
		return objectiveParameterVector[index];
	}
	
	public void setObjectiveParameter(int index, double value){
		objectiveParameterVector[index] = value;
	}
}
