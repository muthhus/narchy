package nars.premise;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2c;

/**
 * Created by me on 12/26/16.
 */
public class DefaultPremiseBuilder extends PremiseBuilder {

    @Override
    protected @NotNull Premise newPremise(@NotNull Concept c, @NotNull Task task, Term beliefTerm, Task belief, float qua, float pri) {

        //return new DefaultPremise
        return new PreferConfidencePremise
                (c, task, beliefTerm, belief, pri, qua);

    }

    static class DefaultPremise extends Premise {

        public DefaultPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
            super(c, task, beliefTerm, belief, pri, qua);
        }


        @Override
        public @Nullable Budget budget(@NotNull Term conclusion, @Nullable Truth truth, @NotNull Derivation conclude) {

            float truthFactor = qualityFactor(truth, conclude);
            float complexityFactor =
                    BudgetFunctions.occamComplexityGrowthRelative(conclusion, task, belief, 1);

            final float q = qua() * complexityFactor;

            if (q < conclude.quaMin)
                return null;

            float p = pri() * truthFactor * complexityFactor;


            return $.b(p, Util.clamp(q, 0f, 1f - Param.BUDGET_EPSILON));

        }

        protected float qualityFactor(@Nullable Truth truth, @NotNull Derivation conclude) {
            return 1f;
        }
    }

    /** prioritizes derivations exhibiting confidence increase, relative to the premise's evidence */
    public static class PreferConfidencePremise extends DefaultPremise {

        public PreferConfidencePremise(Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
            super(c, task, beliefTerm, belief, pri, qua);
        }

        @Override protected float qualityFactor(@Nullable Truth truth, @NotNull Derivation conclude) {
            if (truth == null) {
                //question or quest:
                return //1;
                       conclude.nar.qualityDefault(Op.QUESTION);
            } else {
                return truth.conf() / w2c(conclude.premiseEvidence);
            }
        }

    }

}


//    /**
//     * Evaluate the quality of a belief as a solution to a problem, then reward
//     * the belief and de-prioritize the problem
//     *
//     * @param question  The problem (question or goal) to be solved
//     * @param solution The belief as solution
//     * @param question     The task to be immediately processed, or null for continued
//     *                 process
//     * @return The budget for the new task which is the belief activated, if
//     * necessary
//     */
//    public static Budget solutionBudget(@NotNull Task question, @NotNull Task solution, @NotNull Truth projectedTruth, @NotNull NAR m) {
//        //boolean feedbackToLinks = false;
//        /*if (task == null) {
//            task = nal.getCurrentTask();
//            feedbackToLinks = true;
//        }*/
//
//
//        boolean judgmentTask = question.isBelief();
//        //float om = orderMatch(problem.term(), solution.term(), duration);
//        //if (om == 0) return 0f;
//        float quality = Tense.solutionQuality(question, solution, projectedTruth, m.time());
//        if (quality <= 0)
//            return null;
//
//        Budget budget = null;
//        if (judgmentTask) {
//            question.budget().orPriority(quality);
//        } else {
//            float taskPriority = question.pri();
//
//            budget = new RawBudget(
//                    and(taskPriority, quality),
//                    //UtilityFunctions.or(taskPriority, quality),
//                    BudgetFunctions.truthToQuality(solution.truth()));
//            question.budget().setPriority(Math.min(1 - quality, taskPriority));
//        }
//        /*
//        if (feedbackToLinks) {
//            TaskLink tLink = nal.getCurrentTaskLink();
//            tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
//            TermLink bLink = nal.getCurrentBeliefLink();
//            bLink.incPriority(quality);
//        }*/
//        return budget;
//    }


//    public static float solutionQuality(Task problem, Task solution, Truth truth, long time) {
//        return Tense.solutionQuality(problem.hasQueryVar(), problem.getOccurrenceTime(), solution, truth, time);
//    }

