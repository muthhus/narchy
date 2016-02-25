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

import nars.Memory;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static float truthToQuality(@NotNull Truth t) {
        float exp = t.expectation();

        //ORIGINAL: Mainly decided by confidence, though binary judgment is also preferred
        return Math.max(exp, (1.0f - exp) * 0.75f);

        //return Math.max(exp, (1.0f - exp)); //balanced, allows negative frequency equal opportunity
    }

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     * <p>
     * The truth value of the judgment in the task of the premise
     * The truth value of the previously existing belief
     * The truth value of the conclusion of revision
     *
     * @return The budget for the new task
     */
    @NotNull
    public static void budgetRevision(@NotNull Task conclusion, @NotNull Task newBelief, @NotNull Task oldBelief) {

        Truth nTruth = newBelief.truth();
        final Budget nBudget = newBelief.budget();

        Truth concTruth = conclusion.truth();
        Truth bTruth = oldBelief.truth();
        float difT = concTruth.getExpDifAbs(nTruth);

        nBudget.andPriority(1.0f - difT);
        nBudget.andDurability(1.0f - difT);

        float cc = concTruth.conf();
        float proportion = cc
                / (cc + Math.min(nTruth.conf(), bTruth.conf()));

//		float dif = concTruth.conf()
//				- Math.max(nTruth.conf(), bTruth.conf());
//		if (dif < 0) {
//			String msg = ("Revision fault: previous belief " + oldBelief
//					+ " more confident than revised: " + conclusion);
////			if (Global.DEBUG) {
//				throw new RuntimeException(msg);
////			} else {
////				System.err.println(msg);
////			}
////			dif = 0;
//		}

        float priority =
                proportion * nBudget.pri();
        //or(dif, nBudget.pri());
        float durability =
                //aveAri(dif, nBudget.dur());
                proportion * nBudget.dur();
        float quality = truthToQuality(concTruth);

		/*
         * if (priority < 0) { memory.nar.output(ERR.class, new
		 * RuntimeException(
		 * "BudgetValue.revise resulted in negative priority; set to 0"));
		 * priority = 0; } if (durability < 0) { memory.nar.output(ERR.class,
		 * new RuntimeException(
		 * "BudgetValue.revise resulted in negative durability; set to 0; aveAri(dif="
		 * + dif + ", task.getDurability=" + task.getDurability() +") = " +
		 * durability)); durability = 0; } if (quality < 0) {
		 * memory.nar.output(ERR.class, new RuntimeException(
		 * "BudgetValue.revise resulted in negative quality; set to 0"));
		 * quality = 0; }
		 */

        conclusion.budget().budget(priority, durability, quality);

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

	/* ----- Task derivation in LocalRules and SyllogisticRules ----- */

    /**
     * Forward logic result and adjustment
     *
     * @param truth The truth value of the conclusion
     * @return The budget value of the conclusion
     */
    @NotNull
    public static Budget forward(@NotNull Truth truth, @NotNull ConceptProcess nal) {
        return budgetInference(truthToQuality(truth), 1, nal);
    }

    /**
     * Backward logic result and adjustment, stronger case
     *
     * @param truth The truth value of the belief deriving the conclusion
     * @param nal   Reference to the memory
     * @return The budget value of the conclusion
     */
    @NotNull
    public static Budget backward(@NotNull Truth truth, @NotNull ConceptProcess nal) {
        return budgetInference(truthToQuality(truth), 1, nal);
    }

    /**
     * Backward logic result and adjustment, weaker case
     *
     * @param truth The truth value of the belief deriving the conclusion
     * @param nal   Reference to the memory
     * @return The budget value of the conclusion
     */
    @NotNull
    public static Budget backwardWeak(@NotNull Truth truth, @NotNull ConceptProcess nal) {
        return budgetInference(w2c(1) * truthToQuality(truth), 1, nal);
    }

	/* ----- Task derivation in CompositionalRules and StructuralRules ----- */

    @Nullable
    public static Budget compoundForward(@NotNull Budget target, @NotNull Truth truth,
                                         @NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(
                target,
                truthToQuality(truth),
                content.term().complexity(),
                nal);
    }

    /**
     * Backward logic with CompoundTerm conclusion, stronger case
     *
     * @param content The content of the conclusion
     * @return The budget of the conclusion
     */
    @NotNull
    public static Budget compoundBackward(@NotNull Termed content, @NotNull ConceptProcess nal) {
        return budgetInference(1.0f, content.term().complexity(), nal);
    }

    /**
     * Backward logic with CompoundTerm conclusion, weaker case
     *
     * @param content The content of the conclusion
     * @param nal     Reference to the memory
     * @return The budget of the conclusion
     */
    @NotNull
    public static Budget compoundBackwardWeak(@NotNull Termlike content,
                                              @NotNull ConceptProcess nal) {
        return budgetInference(w2c(1), content.complexity(), nal);
    }

    @Nullable
    static Budget budgetInference(float qual, int complexity, @NotNull ConceptProcess nal) {
        return budgetInference(new UnitBudget(), qual, complexity, nal);
    }

    /**
     * Common processing for all logic step
     *
     * @param qual       Quality of the logic
     * @param complexity Syntactic complexity of the conclusion
     * @param nal        Reference to the memory
     * @return Budget of the conclusion task
     */
    @Nullable
    static Budget budgetInference(@NotNull Budget target, float qual, int complexity,
                                  @NotNull ConceptProcess nal) {
        //float complexityFactor = complexity > 1 ?

        // sqrt factor (experimental)
        // (float) (1f / Math.sqrt(Math.max(1, complexity))) //experimental,
        // reduces dur and qua by sqrt of complexity (more slowly)

        // linear factor (original)
        //(1.0f / Math.max(1, complexity))

        //: 1.0f;
        float complexityFactor = 1f / complexity;

        return budgetInference(target, qual, complexityFactor, nal);
    }

    @Nullable
    static Budget budgetInference(@NotNull Budget target, float qualRaw, float complexityFactor, @NotNull ConceptProcess nal) {

        Term taskTerm = nal.taskLink.get().term();
        Budgeted task = nal.taskLink;
        if (task.isDeleted()) {
            task = nal.taskLink.get();
        }


        float p, d;
        if (!task.isDeleted()) {
            p = task.pri();
            d = task.dur();
        } else {
            return null;
        }

        //(taskLink !=null) ? taskLink :  nal.task().budget();

        //Task task = taskLink.get();

        float priority = p;
        float durability = d * complexityFactor;
        final float quality  = qualRaw  * complexityFactor /** task.qua()*/;//originally: not multiplying task

        target.budget(priority, durability, quality);

        BLink<? extends Termed> termLink = nal.termLink;
        if (/*(termLink != null) && */(!termLink.isDeleted())) {
            //priority = or(priority, termLink.pri()); //originally was OR, but this can explode because the result of OR can exceed the inputs
            //durability = and(durability, termLink.dur()); //originaly was 'AND'


            BudgetMerge.avgDQBlend.merge(target, termLink);

            NAR nar = nal.nar;

            final float targetActivation = nal.conceptLink.pri();

            if (targetActivation >= 0) {

                float sourceActivation =
                        nar.conceptPriority(taskTerm, 0);
                //taskLink != null ? nar.conceptPriority(taskLink.get().concept(), 0) : 1f;

                //https://groups.google.com/forum/#!topic/open-nars/KnUA43B6iYs
                termLink.orPriority(or(quality, and(sourceActivation, targetActivation)));
                //was
                //termLink.orPriority(or(quality, targetActivation));
                termLink.orDurability(quality);
            }
        }



        return target; //target.budget(priority, durability, quality);


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

    // may be more efficient than the for-loop version above, for 2 params
    public static float aveAri(float a, float b) {
        return (a + b) / 2.0f;
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
    public static boolean valid(@NotNull Budget budget, @NotNull Memory m) {
        return //!budget.isDeleted() &&
                budget.dur() >= m.derivationDurabilityThreshold.floatValue();
    }
}
