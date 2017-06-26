package de.julian.baehr.es.basic.oneplusone.type;

import java.util.List;

import de.julian.baehr.es.basic.oneplusone.Individual;

public interface IType {

	List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children);
}
