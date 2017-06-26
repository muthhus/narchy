package de.julian.baehr.es.basic.oneplusone.recombination;

import java.util.List;

import de.julian.baehr.es.basic.oneplusone.Individual;

public interface IRecombinationOperator {

	Individual recombine(List<Individual> parents);
}
