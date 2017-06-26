package de.julian.baehr.es.basic.oneplusone.mutation;

import de.julian.baehr.es.basic.oneplusone.IObjectiveFunction;
import de.julian.baehr.es.basic.oneplusone.Individual;

public interface IMutationOperation {

	void mutate(Individual individual, IObjectiveFunction objectiveFunction);
}
