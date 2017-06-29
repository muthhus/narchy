package de.julian.baehr.es.commaAndPlus.type;

import de.julian.baehr.es.commaAndPlus.Individual;

import java.util.List;

public interface IType {

	List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children);
}
