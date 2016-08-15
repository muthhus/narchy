package nars.budget.policy;

import nars.$;
import nars.Memory;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.nal.Premise;
import nars.nal.Tense;
import nars.nal.UtilityFunctions;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {


    /** combines the tasklinks and termlink budgets to arrive at a Premise budget, used in budgeting its derivations */
    public static void premise(@NotNull Budget p, @NotNull BLink<Task> taskLink, @NotNull BLink<Term> termLink) {
        try {
            if (taskLink.isDeleted())
                return;
            p.budget(taskLink);
            if (termLink.isDeleted())
                return;
            BudgetMerge.plusBlend.apply(p, termLink, 1f);
        } catch (Budget.BudgetException e) {
            //HACK - this isnt a full solution, but it should work temporarily
            return;
        }
    }

    public static @Nullable Budget derivation(float qual, @NotNull Termed derived, @NotNull PremiseEval p, float minDur) {

        Premise pp = p.premise;

        Task parentTask = pp.task;

        //Penalize by complexity: RELATIVE SIZE INCREASE METHOD

        float parentComplexity = Math.max(parentTask.complexity(), pp.term.complexity());
        int derivedComplexity = derived.complexity();

        float volRatioScale = 1f / (1f + (derivedComplexity / (derivedComplexity + parentComplexity)));

        //volRatioScale = volRatioScale * volRatioScale; //sharpen



        final float durability = pp.dur() * volRatioScale;
        if (durability < minDur)
            return null;

        float priority =
                //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                pp.pri()
                    * volRatioScale
        ;
        //if (priority * durability < Param.BUDGET_EPSILON)
            //return null;

        final float quality = qual * volRatioScale;


        return $.b(priority, durability, quality);


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

    /**
     * Backward logic with CompoundTerm conclusion, stronger case
     */
    @Nullable
    public static Budget derivationBackward(@NotNull Termed content, @NotNull PremiseEval premise, float minDur) {
        return derivation(premise.premise.qua(), content, premise, minDur);
    }

    /**
     * Forward logic with CompoundTerm conclusion
     */
    @Nullable
    public static Budget derivationForward(@NotNull Truth truth, @NotNull Termed content, @NotNull PremiseEval premise, float minDur) {
        return derivation(
                BudgetFunctions.truthToQuality(truth),
                content,
                premise, minDur);
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
    public static Budget solutionBudget(@NotNull Task question, @NotNull Task solution, @NotNull Truth projectedTruth, @NotNull Memory m) {
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

            budget = new UnitBudget(
                    UtilityFunctions.and(taskPriority, quality),
                    //UtilityFunctions.or(taskPriority, quality),
                    question.dur(), BudgetFunctions.truthToQuality(solution.truth()));
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
