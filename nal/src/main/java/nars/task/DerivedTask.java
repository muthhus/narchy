package nars.task;

import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;


abstract public class DerivedTask extends MutableTask {

    @NotNull
    public final Reference<ConceptProcess> premise;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise) {
        super(tc, punct, truth);

        @Nullable long[] pte = premise.task().evidence();
        evidence(
            premise.belief != null ?
                Stamp.zip(pte, premise.belief.evidence()) : //double
                pte //single
        );

        this.premise = new SoftReference(premise);
    }

    @Override @Nullable public final Task getParentTask() {
        ConceptProcess p = this.premise.get();
        return p!=null ? p.task() : null;
    }
    @Override @Nullable public final Task getParentBelief() {
        ConceptProcess p = this.premise.get();
        return p!=null ? p.belief : null;
    }


    //    /** next = the child which resulted from this and another task being revised */
//    @Override public boolean onRevision(@NotNull Task next) {
//
//
//        return true;
//    }

//    public void multiplyPremise(float factor, boolean alsoDurability) {
//        multiply(factor, taskLink, alsoDurability);
//        multiply(factor, termLink, alsoDurability);
//    }

    static void multiply(float factor, @Nullable BLink link, boolean alsoDurability) {
        if (link !=null && !link.isDeleted()) {
            link.andPriority(factor);
            if (alsoDurability)
                link.andDurability(factor);
        }
    }

    public static class DefaultDerivedTask extends DerivedTask {

        public DefaultDerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise) {
            super(tc, punct, truth, premise);
        }
    }

    public static class CompetingDerivedTask extends DerivedTask {


        public CompetingDerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise) {
            super(tc, punct, truth, premise);
        }

        @Override
        public boolean onConcept(@NotNull Concept c) {
            if (super.onConcept(c)) {
                ConceptProcess p = this.premise.get();
                if (p!=null) {
                    Concept pc = p.conceptLink.get();
                    Concept.linkPeer(pc.termlinks(), p.termLink.get(), budget(), qua());
                    Concept.linkPeer(pc.tasklinks(), p.taskLink.get(), budget(), qua());
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                ConceptProcess p = this.premise.get();
                if (p!=null) {
                    Concept pc = p.conceptLink.get();
                    Concept.linkPeer(pc.termlinks(), p.termLink.get(), UnitBudget.Zero, qua());
                    Concept.linkPeer(pc.tasklinks(), p.taskLink.get(), UnitBudget.Zero, qua());
                }

                this.premise.clear();

                return true;
            }
            return false;
        }
    }

}
//scratch
//float deathFactor = 1f - 1f / (1f +(conf()/evidence().length));
//float deathFactor = (1f/(1 + c * c * c * c));
