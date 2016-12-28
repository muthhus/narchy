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
        return new DefaultPremise(c, task, beliefTerm, belief, pri, qua);
    }

    static class DefaultPremise extends Premise {
        public DefaultPremise(@NotNull Concept c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
            super(c, task, beliefTerm, belief, pri, qua);
        }

        @Override
        public @Nullable Budget budget(@NotNull Term conclusion, @Nullable Truth truth, @NotNull Derivation conclude) {
            float derivationQuality;
            if (truth == null) {
                //question or quest:
                derivationQuality = conclude.nar.qualityDefault(Op.QUESTION);
            } else {
                derivationQuality = truth.conf() / w2c(conclude.premiseEvidence);
            }


            Premise baseBudget = conclude.premise;

            //Penalize by complexity: RELATIVE SIZE INCREASE METHOD
            /** occam factor */
            float occam = BudgetFunctions.occamComplexityGrowthRelative(conclusion, baseBudget, 1);

            final float quality1 =
                    Util.clamp(baseBudget.qua() * occam * derivationQuality, 0f, 1f- Param.BUDGET_EPSILON);

            if (quality1 < conclude.quaMin)
                return null;

            float priority1 =
                    //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                    //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                    //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                    baseBudget.pri() * occam * derivationQuality
                    //;
                    ;

            //and(baseQuality, factor);
            //baseBudget.qua();


            //* occam //priority should be reduced as well as durability, because in the time between here and the next forgetting it should not have similar priority as parent in cases like Belief:Identity truth function derivations
            //* qual
            //if (priority * durability < Param.BUDGET_EPSILON)
            //return null;

            return $.b(priority1, quality1);


        /* ORIGINAL: https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/inference/BudgetFunctions.java
            Item t = memory.currentTaskLink;
            if (t == null) {
                t = memory.currentTask;
            }
            float priority = t.getPriority();
            float durability = t.getDurability() / complexity;
            float quality = qual / complexity;
            TermLink termLink = memory.currentBeliefLink;
            if (termLink != null) {
                priority = or(priority, termLink.getPriority());
                durability = and(durability, termLink.getDurability());
                float targetActivation = memory.getConceptActivation(termLink.getTarget());
                termLink.incPriority(or(quality, targetActivation));
                termLink.incDurability(quality);
            }
            return new BudgetValue(priority, durability, quality);
         */
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

