package nars.task;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/** TODO extend an ImmutableTask class */
abstract public class DerivedTask extends MutableTask {


    @Nullable
    public volatile transient Premise premise;

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
        public void feedback(@Nullable TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {

            if (delta == null) {

                negativeFeedback(nar);

            } else {

                feedbackToPremiseConcepts(nar);
                feedbackToPremiseLinks(delta, deltaConfidence, deltaSatisfaction, nar);

            }

            if (!Param.DEBUG) {
                this.premise = null;
            }
        }

        private void negativeFeedback(@NotNull NAR nar) {
            feedback(1f - priIfFiniteElseZero() /* HEURISTIC */, nar);
            //delete(); //delete will happen soon after this
        }

        private void feedbackToPremiseConcepts(@NotNull NAR nar) {
            Concept thisConcept = concept(nar);
            if (thisConcept!=null) {
                Premise p;
                if ((p = premise) != null)
                    feedbackToPremiseConcepts(thisConcept, nar, p.concept);

                feedbackToPremiseConcepts(thisConcept, nar, getParentTask());
                feedbackToPremiseConcepts(thisConcept, nar, getParentBelief());
            }
        }

        private void feedbackToPremiseConcepts(@NotNull Concept thisConcept, @NotNull NAR nar, @Nullable Termed p) {
            if (p!=null) {
                Concept parentConcept = nar.concept(p);
                if (parentConcept!=null) {

                    //TODO use CrossLink or other Activation's here?

                    if (Param.DERIVATION_TASKLINKED) {
                        parentConcept.tasklinks().put(this);
                    }

                    if (Param.DERIVATION_TERMLINKED && !thisConcept.equals(parentConcept)) {
                        parentConcept.termlinks().put(thisConcept.term(), budget());
                        thisConcept.termlinks().put(parentConcept.term(), budget());
                    }

                }
            }
        }

        public void feedbackToPremiseLinks(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {


                /* HEURISTIC */
                float confBoost = Math.abs(deltaConfidence);
                float satisBoost =
                        //Math.abs(deltaSatisfaction);
                        Math.abs(deltaSatisfaction);

                float boost =
                        //1f + or(Math.abs(deltaConfidence), Math.abs(deltaSatisfaction));
                        //1f + deltaConfidence * Math.abs(deltaSatisfaction);
                        //1f + Math.max(deltaConfidence, deltaSatisfaction);
                        1f + confBoost/2f + satisBoost/2f;

                feedback(boost, nar);


        }

        void feedback(float score, @NotNull NAR nar) {

            //reduce the score factor intensity by lerping it closer to 1.0
            score = Util.lerp(score, 1f, nar.linkFeedbackRate.floatValue());

            if (!Util.equals(score, 1f, Param.BUDGET_EPSILON)) {

                @Nullable Premise premise = this.premise;
                if (premise != null) {

                    float b = score;

                    Concept c = nar.concept(premise.concept, b);

                    if (c != null) {
                        c.termlinks().mul(premise.term, b);
                        //c.tasklinks().boost(premise.task, score);
                        nar.concept(c.term(), b);
                    }



                }

                //budget().priMult(score);

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
