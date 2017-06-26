package de.julian.baehr.es.commaAndPlus.type;

import java.util.List;

import de.julian.baehr.es.commaAndPlus.Individual;

public interface IType {

	List<Individual> getPossibleParents(List<Individual> parents, List<Individual> children);
}
