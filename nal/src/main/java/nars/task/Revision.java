package nars.task;

import com.gs.collections.api.tuple.primitive.FloatObjectPair;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.budget.merge.BudgetMerge;
import nars.nal.UtilityFunctions;
import nars.term.Compound;
import nars.truth.*;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.StrictMath.abs;
import static nars.nal.Tense.DTERNAL;
import static nars.truth.TruthFunctions.c2w;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {



    /**
     * Revision for Eternal tasks
     */
    @Nullable
    public static Truth revisionEternal(@NotNull Truthed a, @NotNull Truthed b, float match, float minConf) {
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

    @Nullable
    public static Truth revision(@NotNull Truthed a, @NotNull Truthed b) {
        return revisionEternal(a, b, 1f, 0f);
    }


    @Nullable public static Task merge(Task a, Task b, long now, long newOcc, Truth newTruth) {


        float aw = a.isQuestOrQuestion() ? 0 : c2w(a.conf()); //question
        float bw = c2w(b.conf());

        long[] newEv = Stamp.zip(a.evidence(), b.evidence(), aw/(aw+bw));

        FloatObjectPair<Compound> c = Revision.dtMerge(a.term(), b.term());

        return new MutableTask(c.getTwo(),
                a, b, now, newOcc, newEv,
                newTruth.confMult(c.getOne()),
                BudgetMerge.plusDQBlend)
                .log("Revection Merge");
    }


    public static float truthProjection(long sourceTime, long targetTime, long currentTime) {
        if (sourceTime == targetTime) {
            return 1f;
        } else {
            long den = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
            return den == 0 ? 1f : (abs(sourceTime - targetTime)) / (float) den;
        }
    }

    /**
     *
     * heuristic which evaluates the semantic similarity of two terms
     * returning 1f if there is a complete match, 0f if there is
     * a totally separate meaning for each, and in-between if
     * some intermediate aspect is different (ex: temporal relation dt)
     *
     * evaluates the terms recursively to compare internal 'dt'
     * produces a tuple (merged, difference amount), the difference amount
     * can be used to attenuate truth values, etc.
     *
     * TODO threshold to stop early
     */
    public static FloatObjectPair<Compound> dtMerge(@NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b)) {
            return PrimitiveTuples.pair(1f, a);
        }
//        Compound c = a.term();
//        if (c.op().isTemporal()) {
//
//            //TODO interpolate the dt() and penalize confidence in proportion to the difference
//            int newDT;
//            Compound bt = b.term();
//            int adt = c.dt();
//            int bdt = bt.dt();
//            if (adt!=DTERNAL && bdt!=DTERNAL)
//                newDT = Math.round(Util.lerp(adt, bdt, aw/(aw+bw)));
//            else if (bdt != DTERNAL)
//                newDT = bt.dt();
//            else if (adt != DTERNAL)
//                newDT = c.dt();
//            else
//                newDT = DTERNAL;
//
//            c = c.dt(newDT);
//        }

        return null;


//            int at = a.dt();
//            int bt = b.dt();
//            if ((at != bt) && (at!=DTERNAL) && (bt!=DTERNAL)) {
////                if ((at == DTERNAL) || (bt == DTERNAL)) {
////                    //either is atemporal but not both
////                    return 0.5f;
////                }
//
////                boolean symmetric = aop.isCommutative();
////
////                if (symmetric) {
////                    int ata = Math.abs(at);
////                    int bta = Math.abs(bt);
////                    return 1f - (ata / ((float) (ata + bta)));
////                } else {
////                    boolean ap = at >= 0;
////                    boolean bp = bt >= 0;
////                    if (ap ^ bp) {
////                        return 0; //opposite direction
////                    } else {
////                        //same direction
//                        return 1f - (Math.abs(at - bt) / (1f + Math.abs(at + bt)));
////                    }
////                }
//            }
//        }
//        return 1f;
    }
}




//    public static float temporalIntersection(long now, long at, long bt, float window) {
//        return window == 0 ? 1f : BeliefTable.relevance(Math.abs(now-at) + Math.abs(now-bt), window);
//    }

//    @Nullable
//    public static Truth revisionTemporalOLD(@NotNull Task ta, @NotNull Task tb, long target, float match, float confThreshold) {
//        Truth a = ta.truth();
//        Truth b = tb.truth();
//
//        long at = ta.occurrence();
//        long bt = tb.occurrence();
//
//        //temporal proximity balancing metric (similar to projection)
//        long adt = 1 + Math.abs(at-target);
//        long bdt = 1 + Math.abs(bt-target);
//        float closeness = (adt!=bdt) ? (bdt/(float)(adt+bdt)) : 0.5f;
//
//        //float w1 = c2w(a.conf()) * closeness;
//        //float w2 = c2w(b.conf()) * (1-closeness);
//        float w1 = a.conf() * closeness;
//        float w2 = b.conf() * (1-closeness);
//
//        final float w = (w1 + w2);
////        float newConf = w2c(w) * match *
////                temporalIntersection(target, at, bt,
////                    Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
////                );
////                //* TruthFunctions.temporalProjectionOld(at, bt, now)
//
//        float newConf = UtilityFunctions.or(w1,w2) * match *
//                temporalIntersection(target, at, bt,
//                        Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
//                );
//
//        if (newConf < confThreshold)
//            return null;
//
//
//        float f1 = a.freq();
//        float f2 = b.freq();
//        return new DefaultTruth(
//                (w1 * f1 + w2 * f2) / w,
//                newConf
//        );
//    }


//    @Nullable
//    public static Budget budgetRevision(@NotNull Truth revised, @NotNull Task newBelief, @NotNull Task oldBelief, @NotNull NAR nar) {
//
//        final Budget nBudget = newBelief.budget();
//
//
////        Truth bTruth = oldBelief.truth();
////        float difT = revised.getExpDifAbs(nTruth);
////        nBudget.andPriority(1.0f - difT);
////        nBudget.andDurability(1.0f - difT);
//
////        float cc = revised.confWeight();
////        float proportion = cc
////                / (cc + Math.min(newBelief.confWeight(), oldBelief.confWeight()));
//
////		float dif = concTruth.conf()
////				- Math.max(nTruth.conf(), bTruth.conf());
////		if (dif < 0) {
////			String msg = ("Revision fault: previous belief " + oldBelief
////					+ " more confident than revised: " + conclusion);
//////			if (Global.DEBUG) {
////				throw new RuntimeException(msg);
//////			} else {
//////				System.err.println(msg);
//////			}
//////			dif = 0;
////		}
//
//        float priority =
//                proportion * nBudget.pri();
//                //or(dif, nBudget.pri());
//        float durability =
//                //aveAri(dif, nBudget.dur());
//                proportion * nBudget.dur();
//        float quality = BudgetFunctions.truthToQuality(revised);
//
//		/*
//         * if (priority < 0) { memory.nar.output(ERR.class, new
//		 * RuntimeException(
//		 * "BudgetValue.revise resulted in negative priority; set to 0"));
//		 * priority = 0; } if (durability < 0) { memory.nar.output(ERR.class,
//		 * new RuntimeException(
//		 * "BudgetValue.revise resulted in negative durability; set to 0; aveAri(dif="
//		 * + dif + ", task.getDurability=" + task.getDurability() +") = " +
//		 * durability)); durability = 0; } if (quality < 0) {
//		 * memory.nar.output(ERR.class, new RuntimeException(
//		 * "BudgetValue.revise resulted in negative quality; set to 0"));
//		 * quality = 0; }
//		 */
//
//        if (BudgetFunctions.valid(durability, nar)) {
//            return new UnitBudget(priority, durability, quality);
//        }
//        return null;
//    }