package nars.budget.policy;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.RawBudget;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.UtilityFunctions.and;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {


//    /** combines the tasklinks and termlink budgets to arrive at a Premise budget, used in budgeting its derivations */
//    public static void premise(@NotNull Budget p, @NotNull BLink<Task> taskLink, @NotNull BLink<Term> termLink) {
//        try {
//            if (taskLink.isDeleted())
//                return;
//            p.setBudget(taskLink);
//            if (termLink.isDeleted())
//                return;
//            BudgetMerge.
//                    orBlend
//                    //plusBlend
//                    //avgBlend
//                    //andBlend
//                    .apply(p, termLink, 1f);
//            p.setDurability( );
//        } catch (Budget.BudgetException e) {
//            //HACK - this isnt a full solution, but it should work temporarily
//            return;
//        }
//    }

    public static @Nullable Budget derivation(float derivationQuality, @NotNull Termed derived, @NotNull PremiseEval p, float minDur) {

        Premise baseBudget = p.premise;



        //Penalize by complexity: RELATIVE SIZE INCREASE METHOD
        /** occam factor */
        float occam = occamBasic(derived, baseBudget);
                //occamSquareWithDeadzone(derived, pp);

        //volRatioScale = volRatioScale * volRatioScale; //sharpen
        //volRatioScale = (float) Math.pow(volRatioScale, 2);


        final float durability = baseBudget.dur() * occam * derivationQuality;
        if (durability < minDur)
            return null;

        float baseQuality = baseBudget.qua();

        final float quality = /*and(baseQuality, */derivationQuality/*)*/;


        float priority =
                //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                baseBudget.pri()
                        * occam
                        * derivationQuality
                        //;
        ;
        //* occam //priority should be reduced as well as durability, because in the time between here and the next forgetting it should not have similar priority as parent in cases like Belief:Identity truth function derivations
        //* qual
        //if (priority * durability < Param.BUDGET_EPSILON)
        //return null;

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

    /** occam's razor: penalize complexity - returns a value between 0 and 1 that priority will be scaled by */
    public static float occamBasic(@NotNull Termed derived, @NotNull Premise pp) {
        Task parentTask = pp.task;
        float parentComplexity = parentTask.complexity();
        Task parentBelief = pp.belief;
        if (parentBelief!=null) // && parentBelief.complexity() > parentComplexity)
            parentComplexity += parentBelief.complexity();

        int derivedComplexity = derived.complexity();
        return (1f - (derivedComplexity / (derivedComplexity + parentComplexity)));
    }

//    /** occam's razor: penalize complexity - returns a value between 0 and 1 that priority will be scaled by
//     *  NOT TESTED may have math problems
//     * */
//    public static float occamSquareWithDeadzone(@NotNull Termed derived, Premise pp) {
//        Task parentTask = pp.task;
//        float parentComplexity = parentTask.volume();
//        Task parentBelief = pp.belief;
//
//        int derivedComplexity = derived.volume();
//
//        //choose the task or belief (if present) which has the complexity nearest the derived (optimistic)
//        Term parentTerm;
//        float delta = Math.abs(derivedComplexity - parentComplexity);
//        if (parentBelief!=null && delta > Math.abs(derivedComplexity - parentBelief.volume())) {
//            parentTerm = parentBelief.term();
//            parentComplexity = parentTerm.volume();
//            delta = Math.abs(derivedComplexity - parentComplexity); //recompute delta for new value
//        } else {
//            parentTerm = parentTask.term();
//        }
//
//        if (Math.max(derived.term().vars(), parentTerm.vars()) >= delta) {
//            //no penalty if the difference in complexity is within a range bounded by the number of variables in the derived.
//            //this is the 'dead-zone' of the curve which encourages variable introduction and substitution
//            return 1f;
//        } else {
//            //otherwise, the penalty is proportional to the absolute change in complexity
//            //this includes whether the complexity decreased (ex: decomposition/substition) or increased (composition)
//            //the theory behind penalizing decrease in complexity is to marginalize
//            //potentially useless runaway decomposition results that could be considered noisy echos or residue of
//            //its premise components
//
//            float relDelta = delta /
//                    aveAri(parentComplexity); //TODO could be LERP based on compared confidence of premise to derived (if belief/goal)
//
//            //the decay rate is given a polynomial boost here to further enforce the
//            //need to avoid complexity
//            float divisor = 1f + relDelta;
//            float scale = 1f / (divisor*divisor);
//
//            //System.out.println(parentTerm + " :: " + derived + "  -- " + n2(scale) + " vs " + n2(occamBasic(derived, pp)));
//
//            return scale;
//        }
//    }

    /**
     * Backward logic with CompoundTerm conclusion, stronger case
     */
    @Nullable
    public static Budget derivationBackward(@NotNull Termed content, @NotNull PremiseEval premise, float minDur) {
        return derivation(premise.nar.qualityDefault(Symbols.QUESTION), content, premise, minDur);
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
