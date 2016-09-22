package nars.budget.policy;

import nars.concept.CompoundConcept;
import nars.concept.Concept;

/** interface for a management model responsible for concept resource allocation:
 *      --budget (time)
 *      --memory (space)
 */
public interface ConceptPolicy {

    int linkCap(Concept compoundConcept, boolean termOrTask);

    int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal);

    int questionCap(boolean questionOrQuest);
}
