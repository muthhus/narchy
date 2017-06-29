package de.julian.baehr.es.basic.oneplusone.type;

import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.List;

public interface IType {

	List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children);
}
