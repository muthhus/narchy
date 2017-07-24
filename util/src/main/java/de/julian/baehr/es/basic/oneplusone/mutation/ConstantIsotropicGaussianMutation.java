package de.julian.baehr.es.basic.oneplusone.mutation;

import de.julian.baehr.es.basic.oneplusone.IObjectiveFunction;
import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.Random;

public class ConstantIsotropicGaussianMutation implements IMutationOperation{

	private final Random random = new Random();
	
	private final double std = 1;
	
	@Override
	public void mutate(Individual individual, IObjectiveFunction _null) {
		
		for(int i = 0; i < individual.getVectorSize(); i++){
			
			individual.setObjectiveParameter(i, individual.getObjectiveParameter(i) + random.nextGaussian() * std);
		}
	}

}
