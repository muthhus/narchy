/*
 * BudgetFunctions.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.budget;

import nars.Global;
import nars.Memory;
import nars.bag.BLink;
import nars.budget.merge.BudgetMerge;
import nars.concept.ConceptProcess;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Budget functions for resources allocation
 */
public final class BudgetFunctions extends UtilityFunctions {

	/* ----------------------- Belief evaluation ----------------------- */

    /**
     * Determine the quality of a judgment by its truth value alone
     * <p>
     *
     * @param t The truth value of a judgment
     * @return The quality of the judgment, according to truth value only
     */
    public static float truthToQuality(@NotNull Truthed t) {
        float exp = t.expectation();

        //ORIGINAL: Mainly decided by confidence, though binary judgment is also preferred
        //return Math.max(exp, (1.0f - exp) * 0.75f);

        return Math.max(exp, (1.0f - exp)); //balanced, allows negative frequency equal opportunity
    }

    // /**
    // * Update a belief
    // *
    // * @param task The task containing new belief
    // * @param bTruth Truth value of the previous belief
    // * @return Budget value of the updating task
    // */
    // static Budget update(final Task task, final Truth bTruth) {
    // final Truth tTruth = task.getTruth();
    // final float dif = tTruth.getExpDifAbs(bTruth);
    // final float priority = or(dif, task.getPriority());
    // final float durability = aveAri(dif, task.getDurability());
    // final float quality = truthToQuality(bTruth);
    // return new Budget(priority, durability, quality);
    // }

    // /* ----------------------- Links ----------------------- */
    // /**
    // * Distribute the budget of a task among the links to it
    // *
    // * @param b The original budget
    // * @param factor to scale dur and qua
    // * @return Budget value for each tlink
    // */
    // public static UnitBudget clonePriorityMultiplied(Budgeted b, float
    // factor) {
    // float newPriority = b.getPriority() * factor;
    // return new UnitBudget(newPriority, b.getDurability(), b.getQuality());
    // }

    // /**
    // */
    // public static void activate(final Budget receiver, final Budget amount,
    // Activating mode) {
    // activate(receiver, amount, mode, 1f);
    // }

    // /* ---------------- Bag functions, on all Items ------------------- */
    // /**
    // * Decrease Priority after an item is used, called in Bag.
    // * After a constant time, p should become d*p. Since in this period, the
    // * item is accessed c*p times, each time p-q should multiple d^(1/(c*p)).
    // * The intuitive meaning of the parameter "forgetRate" is: after this
    // number
    // * of times of access, priority 1 will become d, it is a system parameter
    // * adjustable in run time.
    // *
    // * @param budget The previous budget value
    // * @param forgetCycles The budget for the new item
    // * @param relativeThreshold The relative threshold of the bag
    // */
    // @Deprecated public static float forgetIterative(Budget budget, float
    // forgetCycles, float relativeThreshold) {
    // float newPri = budget.getQuality() * relativeThreshold; // re-scaled
    // quality
    // float dp = budget.getPriority() - newPri; // priority above quality
    // if (dp > 0) {
    // newPri += (float) (dp * pow(budget.getDurability(), 1.0f / (forgetCycles
    // * dp)));
    // } // priority Durability
    // budget.setPriority(newPri);
    // return newPri;
    // }
    //
    //
    //
    // /** forgetting calculation for real-time timing */
    // public static float forgetPeriodic(Budget budget, float forgetPeriod /*
    // cycles */, float minPriorityForgettingCanAffect, long currentTime) {
    //
    // float currentPriority = budget.getPriority();
    // long forgetDelta = budget.setLastForgetTime(currentTime);
    // if (forgetDelta == 0) {
    // return currentPriority;
    // }
    //
    // minPriorityForgettingCanAffect *= budget.getQuality();
    //
    // if (currentPriority < minPriorityForgettingCanAffect) {
    // //priority already below threshold, don't decrease any further
    // return currentPriority;
    // }
    //
    // float forgetProportion = forgetDelta / forgetPeriod;
    // if (forgetProportion <= 0) return currentPriority;
    //
    // //more durability = slower forgetting; durability near 1.0 means
    // forgetting will happen slowly, near 0.0 means will happen at a max rate
    // forgetProportion *= (1.0f - budget.getDurability());
    //
    // float newPriority = forgetProportion > 1.0f ?
    // minPriorityForgettingCanAffect : currentPriority * (1.0f -
    // forgetProportion) + minPriorityForgettingCanAffect * (forgetProportion);
    //
    //
    // budget.setPriority(newPriority);
    //
    // return newPriority;
    //
    //
    // /*if (forgetDelta > 0)
    // System.out.println("  " + currentPriority + " -> " +
    // budget.getPriority());*/
    //
    // }

	/*
	 * public final static float abs(final float a, final float b) { float c =
	 * (a - b); return (c >= 0) ? c : -c; }
	 */

//	/* ----- Task derivation in LocalRules and SyllogisticRules ----- */
//
//    /**
//     * Forward logic result and adjustment
//     *
//     * @param truth The truth value of the conclusion
//     * @return The budget value of the conclusion
//     */
//    @Nullable
//    public static Budget forward(@NotNull Truth truth, @NotNull ConceptProcess nal) {
//        return budgetInference(truthToQuality(truth), 1, nal);
//    }

//    /**
//     * Backward logic result and adjustment, stronger case
//     *
//     * @param truth The truth value of the belief deriving the conclusion
//     * @param nal   Reference to the memory
//     * @return The budget value of the conclusion
//     */
//    @Nullable
//    public static Budget backward(@NotNull Truth truth, @NotNull ConceptProcess nal) {
//        return budgetInference(truthToQuality(truth), 1, nal);
//    }

//    /**
//     * Backward logic result and adjustment, weaker case
//     *
//     * @param truth The truth value of the belief deriving the conclusion
//     * @param nal   Reference to the memory
//     * @return The budget value of the conclusion
//     */
//    @Nullable
//    public static Budget backwardWeak(@NotNull Truth truth, @NotNull ConceptProcess nal) {
//        return budgetInference(w2c(1) * truthToQuality(truth), 1, nal);
//    }

	/* ----- Task derivation in CompositionalRules and StructuralRules ----- */

    @Nullable
    public static Budget compoundForward(@NotNull Budget target, @NotNull Truth truth,
                                         @NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(
                target,
                truthToQuality(truth),
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

//    /**
//     * Backward logic with CompoundTerm conclusion, weaker case
//     *
//     * @param content The content of the conclusion
//     * @param nal     Reference to the memory
//     * @return The budget of the conclusion
//     */
//    @Nullable
//    public static Budget compoundBackwardWeak(@NotNull Termlike content,
//                                              @NotNull ConceptProcess nal) {
//        return budgetInference(w2c(1), content.volume(), nal);
//    }


    @Nullable
    static Budget budgetInference(@NotNull Budget target, float qualRaw, @NotNull Termed derived, @NotNull ConceptProcess nal) {

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
            priority *= or(taskLink.priIfFiniteElseZero(), termLink.priIfFiniteElseZero());


        //originaly was 'AND'
        float durability = and(taskLink.dur() * volRatioScale, termLink.dur());

        float quality = qualRaw * volRatioScale;


        //Strengthen the termlink by the quality and termlink's & tasklink's concept priorities
        final float targetActivation = nal.nar.conceptPriority(nal.termLink.get());
        final float sourceActivation = nal.nar.conceptPriority(nal.taskLink.get());

        //https://groups.google.com/forum/#!topic/open-nars/KnUA43B6iYs
        if (!termLink.isDeleted()) {
            termLink.orPriority(quality,
                    and(sourceActivation, targetActivation)
                    //or(sourceActivation, targetActivation)
            ); //was: termLink.orPriority(or(quality, targetActivation));
            termLink.orDurability(quality);
        }


        //BudgetMerge.avgDQBlend.merge(target, termLink);

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
     * tests a budget's validity for a task to be processed by a memory
     */
    @Nullable public static Budget valid(@NotNull Budget budget, @NotNull Memory m) {
        return //!budget.isDeleted() &&
                (/*budget!=null && */valid(budget.dur(), m)) ? budget : null;
    }

    public static boolean valid(/*float p,*/ float d, @NotNull Memory m) {
        return //!budget.isDeleted() &&
                d >= m.derivationDurabilityThreshold.floatValue();
    }

    /** from a to b, LERP of priority according to strength 's' [0 <= s <= 1] */
    public static void transferPri(@Nullable Budget a, @Nullable Budget b, float s) {
        float priToTransfer = a.pri() * s;
        float nextB = b.pri() + priToTransfer;

        //cap at 1, and only transfer what is necessary to reach it
        priToTransfer += Math.min(0f, 1f - nextB);

        b.setPriority(Math.min(nextB, 1f));
        a.priSub(priToTransfer);
    }

    /** TODO guarantee balanced input and output */
    public static Budget taxCollection(Stream<Task> tt, float taskPriMult) {
        UnitBudget u = new UnitBudget();
        tt.forEach(t -> {
            @NotNull Budget tbudget = t.get().budget();
            if (!tbudget.isDeleted()) {
                BudgetMerge.plusDQBlend.merge(u, tbudget, 1f);
                tbudget.priMult(taskPriMult);
            }
        });
        return u;
    }

    /**
     * balance the priorities of 2 existing budgets ('a' and 'b')
     * which transfer some of their budget to the resulting new budget.
     * This new budget will have already been created with a priority (resultPri)
     * of a value less than the existing priority sum.
     * a strength parameter (0 < s < 1) indicates the proportional balance
     * to source the necessary budget from each respective parent. ex: 0.5 is
     * equally balanced, while 0.75f means that the budget discount to 'b' will
     * be 3x higher than that which is subtracted from 'a'.
     *
     * if either input budget is null or deleted (non-exists), the burden will shift
     * to the other budget (if exists). if neither exists, no effect results.
     */
    public static void balancePri(@Nullable Budget a, @Nullable Budget b, float resultPri, float aStrength) {

        boolean aExist = a!=null && !a.isDeleted();
        boolean bExist = b!=null && !b.isDeleted();
        if (!bExist && !aExist) {
            //do nothing, the sources are non-existant
        }
        else if (aExist && bExist) {

            float bPriNext = b.pri() - resultPri * aStrength;
            float aPriNext = a.pri() - resultPri * (1f - aStrength);

            if (aPriNext < 0f) {
                bPriNext -= -aPriNext; //subtract remainder from the other
                aPriNext = 0f;
            }
            if (bPriNext < 0f) {
                aPriNext -= -bPriNext; //subtract remainder from the other
                bPriNext = 0f;
            }

            //assert (!((aPriNext < 0) || (bPriNext < 0))); //throw new RuntimeException("revision budget underflow");

            //apply the changes
            a.setPriority(aPriNext);
            b.setPriority(bPriNext);
        } else if (aExist /*&& !bExist*/) {
            //take from 'a' only
            a.priSub(resultPri);
        } else if (bExist /*&& !aExist*/) {
            //take from 'b' only
            b.priSub(resultPri);
        }
    }

}
