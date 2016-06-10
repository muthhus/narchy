package nars.budget.policy;

import nars.Memory;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.nal.ConceptProcess;
import nars.nal.Tense;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.UtilityFunctions.and;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {

    public static @Nullable Budget budgetInference(@NotNull Budget target, float qualRaw, @NotNull Termed derived, @NotNull ConceptProcess nal) {

        //BLink<? extends Task> taskLink = nal.taskLink;


        //if (task.isDeleted()) return null;


        //(taskLink !=null) ? taskLink :  nal.task().budget();

        //Task task = taskLink.get();


//        BLink<? extends Termed> termLink = nal.termLink;
//        BLink<? extends Task> taskLink = nal.taskLink;


        //originally was OR, but this can explode because the result of OR can exceed the inputs
        //float priority = or(taskLink.pri(), termLink.pri());
        //float priority = and(taskLink.pri(), termLink.pri());

        Task task = nal.task();
        if (task == null)
            return null;

//        float priority = //UtilityFunctions.aveGeo(
//                //task!=null ? task.priIfFiniteElseZero() : 0,
//                (aveGeo(taskLink.priIfFiniteElseZero(),
//                    termLink.priIfFiniteElseZero()));


        //Penalize by complexity
        float volRatioScale;
        //ORIGINAL METHOD
        //volRatioScale = 1f / derived.term().volume();
        //RELATIVE SIZE INCREASE METHOD
        int tasktermVol = task.volume();
        volRatioScale =
            Math.min(1f, tasktermVol / ((float)( tasktermVol + derived.volume() )));

        float priority =
                //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                and(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                        //* volRatioScale
        ;

        final float durability =
                task.dur() * volRatioScale;
                //UtilityFunctions.and(taskLink.dur() * volRatioScale, termLink.dur());

        final float quality = qualRaw * volRatioScale;


        //Strengthen the termlink by the quality and termlink's & tasklink's concept priorities

        //https://groups.google.com/forum/#!topic/open-nars/KnUA43B6iYs
//
//        if (!termLink.isDeleted()) {
//            final float targetActivation = nal.nar.conceptPriority(nal.termLink.get());
//            final float sourceActivation = nal.nar.conceptPriority(nal.task());
//
//            termLink.orPriority(quality,
//                    and(sourceActivation, targetActivation)
//                    //or(sourceActivation, targetActivation)
//            ); //was: termLink.orPriority(or(quality, targetActivation));
//            termLink.orDurability(quality);
//        }

//        termLink.orPriority(quality);
//        termLink.orDurability(quality);




        return target.budget(priority, durability, quality);


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

    @Nullable
    public static Budget compoundForward(@NotNull Budget target, @NotNull Truth truth,
                                         @NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(
                target,
                BudgetFunctions.truthToQuality(truth),
                content,
                nal);
    }

    /**
     * Backward logic with CompoundTerm conclusion, stronger case
     *
     * @param content The content of the conclusion
     * @return The budget of the conclusion
     */
    @Nullable
    public static Budget compoundQuestion(@NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(new UnitBudget(),
                //1.0f
                1.0f/content.term().complexity(),
                content, nal);
    }

    /**
     * Forward logic with CompoundTerm conclusion
     *
     * @param truth   The truth value of the conclusion
     * @param content The content of the conclusion
     * @param nal     Reference to the memory
     * @return The budget of the conclusion
     */
    @Nullable
    public static Budget compoundForward(@NotNull Truth truth, @NotNull Termed content, @NotNull ConceptProcess nal) {
        return compoundForward(new UnitBudget(), truth, content, nal);
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
