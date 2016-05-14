package nars.budget.policy;

import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;


public interface ConceptPolicy  {

    int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal);
    int linkCap(AbstractConcept compoundConcept, boolean termOrTask);

}
