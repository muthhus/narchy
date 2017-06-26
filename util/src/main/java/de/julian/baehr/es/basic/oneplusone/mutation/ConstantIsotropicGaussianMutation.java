package de.julian.baehr.es.basic.oneplusone.mutation;

import java.util.Random;

import de.julian.baehr.es.basic.oneplusone.IObjectiveFunction;
import de.julian.baehr.es.basic.oneplusone.Individual;

public class ConstantIsotropicGaussianMutation implements IMutationOperation{

	private Random random = new Random();
	
	private double std = 1;
	
	@Override
	public void mutate(Individual individual, IObjectiveFunction _null) {
		
		for(int i = 0; i < individual.getVectorSize(); i++){
			
			individual.setObjectiveParameter(i, individual.getObjectiveParameter(i) + random.nextGaussian() * std);
		}
	}

}
