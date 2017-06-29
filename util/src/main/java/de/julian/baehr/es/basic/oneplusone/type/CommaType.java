package de.julian.baehr.es.basic.oneplusone.type;

import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.List;

public class CommaType implements IType{

	@Override
	public List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children) {
		return children;
	}

}
