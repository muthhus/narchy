package nars.budget.policy;

import nars.Global;
import nars.bag.BLink;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.concept.ConceptProcess;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {

    public static @Nullable Budget budgetInference(@NotNull Budget target, float qualRaw, @NotNull Termed derived, @NotNull ConceptProcess nal) {

        //BLink<? extends Task> taskLink = nal.taskLink;


        BLink<? extends Task> taskLink = nal.taskLink;
        Task task = nal.task();



        BLink<? extends Termed> termLink = nal.termLink;
        assert(!termLink.isDeleted());
        //if (task.isDeleted()) return null;
        //Task task = nal.task();


        //(taskLink !=null) ? taskLink :  nal.task().budget();

        //Task task = taskLink.get();






        float volRatioScale = 1f / derived.term().volume();

        /*
        int tasktermVol = nal.task().term().volume();
        float volRatioScale =
            Math.min(1f, tasktermVol / ((float)( tasktermVol + derived.volume() )));
        */


        //originally was OR, but this can explode because the result of OR can exceed the inputs
        //float priority = or(taskLink.pri(), termLink.pri());
        //float priority = and(taskLink.pri(), termLink.pri());
        float priority = task.priIfFiniteElseZero();
        if (priority > Global.BUDGET_EPSILON)
            priority *= UtilityFunctions.or(taskLink.priIfFiniteElseZero(), termLink.priIfFiniteElseZero());


        //originaly was 'AND'
        float durability = UtilityFunctions.and(taskLink.dur() * volRatioScale, termLink.dur());

        float quality = qualRaw * volRatioScale;


        //Strengthen the termlink by the quality and termlink's & tasklink's concept priorities

        //https://groups.google.com/forum/#!topic/open-nars/KnUA43B6iYs
        if (!termLink.isDeleted()) {
            final float targetActivation = nal.nar.conceptPriority(nal.termLink.get());
            final float sourceActivation = nal.nar.conceptPriority(nal.task);

            termLink.orPriority(quality,
                    UtilityFunctions.and(sourceActivation, targetActivation)
                    //or(sourceActivation, targetActivation)
            ); //was: termLink.orPriority(or(quality, targetActivation));
            termLink.orDurability(quality);
        }



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
    public static Budget compoundBackward(@NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(new UnitBudget(), 1.0f, content, nal);
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
}
