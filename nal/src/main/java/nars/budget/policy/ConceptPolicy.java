package nars.budget.policy;

import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;
import nars.util.data.MutableInteger;


public interface ConceptPolicy  {

    int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal);
    int linkCap(AbstractConcept compoundConcept, boolean termOrTask);

}
