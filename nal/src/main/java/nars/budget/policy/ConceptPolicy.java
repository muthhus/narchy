package nars.budget.policy;

import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;

/** interface for a management model responsible for concept resource allocation:
 *      --budget (time)
 *      --memory (space)
 */
public interface ConceptPolicy {

    int linkCap(AbstractConcept compoundConcept, boolean termOrTask);

    int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal);

    int questionCap(boolean questionOrQuest);
}
