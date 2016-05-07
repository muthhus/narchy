package nars.task;

import nars.NAR;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.concept.table.BeliefTable;
import nars.nal.LocalRules;
import nars.nal.UtilityFunctions;
import nars.term.Compound;
import nars.term.Terms;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Revision Utilities
 */
public class Revision {



    /**
     * Evaluate the quality of a revision, set its budget, and de-prioritize the premise tasks and associated links
     * <p>
     * The truth value of the judgment in the task of the premise
     * The truth value of the previously existing belief
     * The truth value of the conclusion of revision
     *
     * @return The budget for the new task
     */
    @Nullable
    public static Budget budgetRevision(@NotNull Truth revised, @NotNull Task newBelief, @NotNull Task oldBelief, @NotNull NAR nar) {

        Truth nTruth = newBelief.truth();
        final Budget nBudget = newBelief.budget();


        Truth bTruth = oldBelief.truth();
        float difT = revised.getExpDifAbs(nTruth);

        nBudget.andPriority(1.0f - difT);
        nBudget.andDurability(1.0f - difT);

        float cc = revised.conf();
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
        float quality = BudgetFunctions.truthToQuality(revised);

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

        if (BudgetFunctions.valid(durability, nar)) {
            return new UnitBudget(priority, durability, quality);
        }
        return null;
    }

    @Nullable
    public static Truth revision(@NotNull Task ta, @NotNull Task tb, long target, float match, float confThreshold) {
        Truth a = ta.truth();
        Truth b = tb.truth();

        long at = ta.occurrence();
        long bt = tb.occurrence();

        //temporal proximity balancing metric (similar to projection)
        long adt = 1 + Math.abs(at-target);
        long bdt = 1 + Math.abs(bt-target);
        float closeness = (adt!=bdt) ? (bdt/(float)(adt+bdt)) : 0.5f;

        //float w1 = c2w(a.conf()) * closeness;
        //float w2 = c2w(b.conf()) * (1-closeness);
        float w1 = a.conf() * closeness;
        float w2 = b.conf() * (1-closeness);

        final float w = (w1 + w2);
//        float newConf = w2c(w) * match *
//                temporalIntersection(target, at, bt,
//                    Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
//                );
//                //* TruthFunctions.temporalProjectionOld(at, bt, now)

        float newConf = UtilityFunctions.or(w1,w2) * match *
                TruthFunctions.temporalIntersection(target, at, bt,
                        Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
                );

        if (newConf < confThreshold)
            return null;


        float f1 = a.freq();
        float f2 = b.freq();
        return new DefaultTruth(
            (w1 * f1 + w2 * f2) / w,
            newConf
        );
    }

    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth revision(@NotNull Truth a, @NotNull Truth b, float match, float minConf) {
        float w1 = TruthFunctions.c2w(a.conf());
        float w2 = TruthFunctions.c2w(b.conf());
        float w = (w1 + w2);
        float newConf = UtilityFunctions.w2c(w) * match;
        if (newConf < minConf)
            return null;

        float f1 = a.freq();
        float f2 = b.freq();

        return new DefaultTruth(
            (w1 * f1 + w2 * f2) / w,
            newConf
        );
    }
}
