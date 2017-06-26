package de.julian.baehr.es.basic.oneplusone.type;

import java.util.List;

import de.julian.baehr.es.basic.oneplusone.Individual;

public class CommaType implements IType{

	@Override
	public List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children) {
		return children;
	}

}
