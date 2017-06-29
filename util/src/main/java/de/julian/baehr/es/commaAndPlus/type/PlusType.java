package de.julian.baehr.es.commaAndPlus.type;

import de.julian.baehr.es.commaAndPlus.Individual;

import java.util.ArrayList;
import java.util.List;

public class PlusType implements IType{

	@Override
	public List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children) {
		
		List<Individual> possibleParents = new ArrayList<>(parents);
		
		possibleParents.addAll(children);
		
		return possibleParents;
	}


}
