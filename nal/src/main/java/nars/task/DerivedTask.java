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

import static nars.nal.UtilityFunctions.or;


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
        if (super.delete()) {
            if (!Param.DEBUG) { //keep premise information in DEBUG mode for analysis
                this.premise = null;
            }
            return true;
        }
        return false;
    }


    public static class DefaultDerivedTask extends DerivedTask {

        public DefaultDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, char punct, long[] evidence, @NotNull PremiseEval premise) {
            super(tc, punct, truth, premise, evidence);
        }


        @Override
        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

            if (delta == null) {

                feedback(1f - pri() /* HEURISTIC */, nar);

            } else {

                float boost = or(Math.abs(deltaConfidence), Math.abs(deltaSatisfaction)); /* HEURISTIC */

                if (!Util.equals(boost, 0, Param.TRUTH_EPSILON)) {
                    feedback(1f + boost, nar);
                }

            }

            if (!Param.DEBUG) {
                this.premise = null;
            }
        }

        void feedback(float score, NAR nar) {

            @Nullable Premise premise = this.premise;
            Concept c = nar.concept(premise.term);
            if (c!=null) {

                //TODO make a Bag method specifically for this (modifying the priority only, if the link exists)

                BLink<? extends Task> tasklink = premise.tasklink(c);
                System.out.println("feedback " + score + " to " + tasklink);
                if (tasklink != null && !tasklink.isDeleted()) {
                    float dp = tasklink.priLerpMult(score, Param.LINK_FEEDBACK_RATE);
                    ((ArrayBag)c.tasklinks()).pressure += dp * tasklink.dur(); //HACK cast
                }


                BLink<? extends Termed> termlink = premise.termlink(c);
                System.out.println("feedback " + score + " to " + termlink);
                if (termlink != null && !termlink.isDeleted()) {
                    float dp = termlink.priLerpMult(score, Param.LINK_FEEDBACK_RATE);
                    ((ArrayBag)c.termlinks()).pressure += dp * termlink.dur(); //HACK cast
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
