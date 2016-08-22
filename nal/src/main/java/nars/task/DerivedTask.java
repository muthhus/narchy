package nars.task;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.link.BLink;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


abstract public class DerivedTask extends MutableTask {


    @Nullable
    public transient Premise premise;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, @Nullable Truth truth, @NotNull PremiseEval p, long[] evidence) {
        super(tc, punct, truth);

        evidence(evidence);

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


    @Override
    public boolean delete() {

        if (!Param.DEBUG) {
            this.premise = null;
        }
        return super.delete();
    }


    public static class DefaultDerivedTask extends DerivedTask {

        public DefaultDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, char punct, long[] evidence, @NotNull PremiseEval premise) {
            super(tc, punct, truth, premise, evidence);
        }


        @Override
        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
            float boost = Math.abs(deltaConfidence) * Math.abs(deltaSatisfaction);
            if (!Util.equals(boost, 0, Param.TRUTH_EPSILON ) ) {

                Premise p;
                synchronized (this.truth()) {
                    p = this.premise;
                    if (p != null) {
                        this.premise = null;
                    }
                }

                //apply feedback outside of synchronized
                if (p!=null) {
                    feedback(1f + boost, nar);
                }
            } else {
                this.premise = null;
            }

        }

        void feedback(float score, NAR nar) {

            float feedbackRate = 0.25f;

            @Nullable Premise premise = this.premise;
            Concept c = nar.concept(premise.term);
            if (c!=null) {

                //TODO make a Bag method specifically for this (modifying the priority only, if the link exists)

                BLink<? extends Task> tasklink = premise.tasklink(c);
                if (tasklink != null && !tasklink.isDeleted()) {
                    float dp = tasklink.priLerpMult(score, feedbackRate);
                    ((ArrayBag)c.tasklinks()).pressure += dp; //HACK cast
                }


                BLink<? extends Termed> termlink = premise.termlink(c);
                if (termlink != null && !termlink.isDeleted()) {
                    float dp = termlink.priLerpMult(score, feedbackRate);
                    ((ArrayBag)c.termlinks()).pressure += dp; //HACK cast
                }

            }

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
