package de.julian.baehr.es.basic.oneplusone.recombination;

import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.List;

public class IntermediateRecombination implements IRecombinationOperator{
	
	@Override
	public Individual recombine(List<Individual> parents) {
		
		if(parents == null || parents.isEmpty())
			throw new RuntimeException("Parents cannot be null (" + (parents == null) + ") or empty (" + (parents != null) + ")!");
		
		double[] childVector = new double[parents.get(0).getVectorSize()];
		
		//choose i-th gene as mean of all parents
		for(int i = 0; i < childVector.length; i++){
			
			//calculate mean value for i-th gene of all parents
			double mean = 0;
			for(int n = 0; n < parents.size(); n++)
				mean += parents.get(n).getObjectiveParameter(i);
			mean /= parents.size();
			
			childVector[i] = mean;
		}
		
		return new Individual(childVector);
	}

}
