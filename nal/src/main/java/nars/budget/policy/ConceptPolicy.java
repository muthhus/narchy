package nars.budget.policy;

import nars.Op;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;

/** interface for a management model responsible for concept resource allocation:
 *      --budget (time)
 *      --memory (space)
 */
public abstract class ConceptPolicy extends AtomicStringConstant {

    protected ConceptPolicy(@NotNull String id) {
        super(id);
    }


    @NotNull
    @Override
    public Op op() {
        return Op.ATOM;
    }

    public abstract int linkCap(Concept compoundConcept, boolean termOrTask);

    public abstract int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal);

    public abstract int questionCap(boolean questionOrQuest);


    public static final ConceptPolicy Deleted = new ConceptPolicy("deleted") {


        @Override
        public int linkCap(Concept compoundConcept, boolean termOrTask) {
            return 0;
        }

        @Override
        public int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal) {
            return 0;
        }

        @Override
        public int questionCap(boolean questionOrQuest) {
            return 0;
        }
    };
}
