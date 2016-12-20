package nars.budget.policy;

import jcog.Util;
import nars.*;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.util.BudgetFunctions;
import nars.nal.Premise;
import nars.nal.meta.Derivation;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2c;
import static nars.util.UtilityFunctions.and;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {

    public static @Nullable Budget derivation(@Nullable Truth truth, @NotNull Termed derived, @NotNull Derivation p) {
        float derivationQuality;
        if (truth == null) {
            //question or quest:
            derivationQuality = p.nar.qualityDefault(Symbols.QUESTION);
        } else {
            derivationQuality = truth.conf() / w2c(p.premiseEvidence);
        }


        Premise baseBudget = p.premise;

        //Penalize by complexity: RELATIVE SIZE INCREASE METHOD
        /** occam factor */
        float occam = occamComplexityGrowthRelative(derived, baseBudget);

        final float quality =
                Util.clamp(baseBudget.qua() * occam * derivationQuality, 0f, 1f- Param.BUDGET_EPSILON);

        if (quality < p.quaMin)
            return null;

        float priority =
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

        return $.b(priority, quality);


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

    /** occam's razor: penalize relative complexity growth
     * @return a value between 0 and 1 that priority will be scaled by */
    public static float occamComplexityGrowthRelative(@NotNull Termed derived, @NotNull Premise pp) {
        Task parentBelief = pp.belief;
        int parentComplexity;
        int taskCompl = pp.task.volume();
        if (parentBelief!=null) // && parentBelief.complexity() > parentComplexity)
            parentComplexity =
                Math.min(taskCompl, parentBelief.volume());
                //Math.max(taskCompl, parentBelief.volume());
        else
            parentComplexity = taskCompl;

        int derivedComplexity = derived.volume();
        //return parentComplexity / (1f + Math.max(parentComplexity, derivedComplexity));
        //return Math.max(1f, (parentComplexity/derivedComplexity));
        return Math.max(1f, (parentComplexity/(derivedComplexity)));
    }




    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param question  The problem (question or goal) to be solved
     * @param solution The belief as solution
     * @param question     The task to be immediately processed, or null for continued
     *                 process
     * @return The budget for the new task which is the belief activated, if
     * necessary
     */
    public static Budget solutionBudget(@NotNull Task question, @NotNull Task solution, @NotNull Truth projectedTruth, @NotNull NAR m) {
        //boolean feedbackToLinks = false;
        /*if (task == null) {
            task = nal.getCurrentTask();
            feedbackToLinks = true;
        }*/


        boolean judgmentTask = question.isBelief();
        //float om = orderMatch(problem.term(), solution.term(), duration);
        //if (om == 0) return 0f;
        float quality = Tense.solutionQuality(question, solution, projectedTruth, m.time());
        if (quality <= 0)
            return null;

        Budget budget = null;
        if (judgmentTask) {
            question.budget().orPriority(quality);
        } else {
            float taskPriority = question.pri();

            budget = new RawBudget(
                    and(taskPriority, quality),
                    //UtilityFunctions.or(taskPriority, quality),
                    BudgetFunctions.truthToQuality(solution.truth()));
            question.budget().setPriority(Math.min(1 - quality, taskPriority));
        }
        /*
        if (feedbackToLinks) {
            TaskLink tLink = nal.getCurrentTaskLink();
            tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
            TermLink bLink = nal.getCurrentBeliefLink();
            bLink.incPriority(quality);
        }*/
        return budget;
    }


//    public static float solutionQuality(Task problem, Task solution, Truth truth, long time) {
//        return Tense.solutionQuality(problem.hasQueryVar(), problem.getOccurrenceTime(), solution, truth, time);
//    }

}
