package nars.budget.policy;

import nars.$;
import nars.Memory;
import nars.Param;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Premise;
import nars.nal.Tense;
import nars.nal.UtilityFunctions;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.UtilityFunctions.aveAri;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {

    public static @Nullable Budget budgetInference(float qual, @NotNull Termed derived, @NotNull PremiseEval p, float minDur) {


        Task parentTask = p.task;

        //Penalize by complexity: RELATIVE SIZE INCREASE METHOD
        float parentVol = aveAri(parentTask.complexity(), p.beliefTerm.complexity());
        //float volRatioScale = parentVol / ((float) (parentVol + derived.complexity()));
        float volRatioScale = Math.min(1, parentVol / derived.complexity() );
        //volRatioScale = volRatioScale * volRatioScale; //sharpen


        Premise pp = p.premise;

        Concept c = pp.concept(p.nar);
        if (c == null)
            return null;

        BLink<? extends Task> taskLink = pp.tasklink(c);
        if (taskLink == null)
            return null;

        BLink<? extends Termed> termLink = pp.termlink(c);
        if (termLink == null)
            return null;

        float linkDur = aveAri( taskLink.dur(), termLink.dur() );
        final float durability = linkDur * volRatioScale;
        if (durability < minDur)
            return null;

        float priority =
                //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                aveAri(taskLink.priIfFiniteElseZero(), termLink.priIfFiniteElseZero())
                    * volRatioScale
        ;
        if (priority * durability < Param.BUDGET_EPSILON)
            return null;

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
     *
     * @param content The content of the conclusion
     * @return The budget of the conclusion
     */
    @Nullable
    public static Budget compoundQuestion(@NotNull Termed content, @NotNull PremiseEval premise, float minDur) {
        return budgetInference(1.0f, content, premise, minDur);
    }

    /**
     * Forward logic with CompoundTerm conclusion
     *
     * @param truth   The truth value of the conclusion
     * @param content The content of the conclusion
     * @param premise     Reference to the memory
     * @return The budget of the conclusion
     */
    @Nullable
    public static Budget compoundForward(@NotNull Truth truth, @NotNull Termed content, @NotNull PremiseEval premise, float minDur) {
        return budgetInference(
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
