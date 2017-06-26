package de.julian.baehr.es.commaAndPlus;

import java.util.Random;

public class Mutator {

	public static Random random = new Random();
	
	public static void mutate(Individual individual, int problemDimensions){
		
		double tau = Math.pow(Math.sqrt(2 * Math.sqrt(problemDimensions)), -1);
		double tauDash = Math.pow(Math.sqrt(2*problemDimensions), -1);
//		double beta = 0.0873;
		
		double gaus = random.nextGaussian();
		for(int i = 0; i < individual.getVectorSize(); i++){
			
			individual.standardDeviations[i] = (float) (individual.standardDeviations[i] * Math.exp(tauDash * gaus + tau * random.nextGaussian()));
		}

		double[] randomVector = getRandomVector(individual);
		for(int i = 0; i < randomVector.length; i++)
			individual.objectVariable[i] += randomVector[i];
	}
	
	public static double[] getRandomVector(Individual individual){
		double[] vectorData = new double[individual.getVectorSize()];
		
		for(int i = 0; i < vectorData.length; i++)
			vectorData[i] = random.nextGaussian()*individual.standardDeviations[i];
		
		return vectorData;
	}
}
