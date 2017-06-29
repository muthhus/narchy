package de.julian.baehr.es.basic.oneplusone.recombination;

import de.julian.baehr.es.basic.oneplusone.Individual;

import java.util.List;

public interface IRecombinationOperator {

	Individual recombine(List<Individual> parents);
}
