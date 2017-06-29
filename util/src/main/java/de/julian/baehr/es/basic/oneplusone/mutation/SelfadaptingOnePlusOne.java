package de.julian.baehr.es.basic.oneplusone.mutation;

import de.julian.baehr.es.basic.oneplusone.IObjectiveFunction;
import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.Random;

public class SelfadaptingOnePlusOne implements IMutationOperation{

	private Random random = new Random();
	
	private double std = 1;
	private int times = 0;
	int better = 0;
	
	@Override
	public void mutate(Individual individual, IObjectiveFunction objectiveFunction) {

		individual.calculateFitness(objectiveFunction);
		double f = individual.getFitness();
		for(int i = 0; i < individual.getVectorSize(); i++){
			
			individual.setObjectiveParameter(i, individual.getObjectiveParameter(i) + random.nextGaussian() * std);
		}

		individual.calculateFitness(objectiveFunction);
		if(f > individual.getFitness()){
			better++;
		}
		
		times ++;
		
		if(times % 100 == 0){
			
			
			times = 0;
			
			if(better / 10d > 1/5d){
				std *= 1d/0.82;
			}else if(better / 10d < 1/5d){
				std *= 0.82;
			}
			
			better = 0;
		}
	}

}
