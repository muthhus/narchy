package nars.task;

import nars.$;
import nars.Param;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;


abstract public class DerivedTask extends MutableTask {

    public transient Premise premise;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, @Nullable Truth truth, PremiseEval p) {
        super(tc, punct, truth);

        evidence(p.evidence());

        if (Param.DEBUG)
            this.premise = p.premise;
    }

    @Override
    public final boolean isInput() {
        return false;
    }

    @Override
    @Nullable
    public final Task getParentTask() {
        Premise p = this.premise;
        return p != null ? p.task() : null;
    }

    @Override
    @Nullable
    public final Task getParentBelief() {
        Premise p = this.premise;
        return p != null ? p.belief : null;
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
        if (link != null && !link.isDeleted()) {
            link.andPriority(factor);
            if (alsoDurability)
                link.andDurability(factor);
        }
    }

    public static class DefaultDerivedTask extends DerivedTask {

        static final float feedbackRate = 0.1f;

        public DefaultDerivedTask(@NotNull Termed<Compound> tc, char punct, @Nullable Truth truth, @NotNull PremiseEval premise) {
            super(tc, punct, truth, premise);
        }

        @Override
        public boolean onConcept(@NotNull Concept c, float score) {
            if (super.onConcept(c, score)) {
                feedback(score);
                return true;
            }
            return false;
        }

        void feedback(float score) {
//            ConceptProcess p = this.premise.get();
//            if (p != null) {
//                BLink<? extends Term> termlink = p.termLink;
//                BLink<? extends Task> tasklink = p.taskLink;
//                //BLink<? extends Concept> pc = p.conceptLink;
//                if (!termlink.isDeleted())
//                    termlink.priLerpMult(score, feedbackRate);
//                if (!tasklink.isDeleted())
//                    tasklink.priLerpMult(score, feedbackRate);
//
//            }
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                feedback(0);
                this.premise = null;
                return true;
            }
            return false;
        }
    }

//    public static class CompetingDerivedTask extends DerivedTask {
//
//
//        public CompetingDerivedTask(@NotNull Termed<Compound> tc, char punct, @Nullable Truth truth, @NotNull PremiseEval premise) {
//            super(tc, punct, truth, premise);
//        }
//
//        @Override
//        public boolean onConcept(@NotNull Concept c, float score) {
//            if (super.onConcept(c, score)) {
//                Premise p = this.premise;
//                if (p != null) {
//                    Concept pc = p.conceptLink;
//                    Concept.linkPeer(pc.termlinks(), p.termLink, budget(), qua());
//                    Concept.linkPeer(pc.tasklinks(), p.taskLink, budget(), qua());
//                }
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public boolean delete() {
//            if (super.delete()) {
//                Premise p = this.premise;
//                if (p != null) {
//                    Concept pc = p.concept();
//                    Concept.linkPeer(pc.termlinks(), p.termLink, UnitBudget.Zero, qua());
//                    Concept.linkPeer(pc.tasklinks(), p.taskLink, UnitBudget.Zero, qua());
//                }
//
//                this.premise = null;
//
//                return true;
//            }
//            return false;
//        }
//    }

}
//scratch
//float deathFactor = 1f - 1f / (1f +(conf()/evidence().length));
//float deathFactor = (1f/(1 + c * c * c * c));
