package de.julian.baehr.es.commaAndPlus.type;

import de.julian.baehr.es.commaAndPlus.Individual;

import java.util.List;

public class CommaType implements IType{

	@Override
	public List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children) {
		return children;
	}

}
