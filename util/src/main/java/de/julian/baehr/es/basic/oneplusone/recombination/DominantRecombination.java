package de.julian.baehr.es.basic.oneplusone.recombination;

import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.List;
import java.util.Random;

public class DominantRecombination implements IRecombinationOperator{

	private final Random random = new Random();
	
	@Override
	public Individual recombine(List<Individual> parents) {
		
		if(parents == null || parents.isEmpty())
			throw new RuntimeException("Parents cannot be null (" + (parents == null) + ") or empty (" + (parents != null) + ")!");
		
		double[] childVector = new double[parents.get(0).getVectorSize()];
		
		//choose i-th gene from random parent
		for(int i = 0; i < childVector.length; i++){
			childVector[i] = parents.get(random.nextInt(parents.size())).getObjectiveParameter(i);
		}
		
		return new Individual(childVector);
	}

}
