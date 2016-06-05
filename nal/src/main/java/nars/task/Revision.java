package nars.task;

import com.gs.collections.api.tuple.primitive.FloatObjectPair;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.Global;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.nal.UtilityFunctions;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermVector;
import nars.truth.*;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.StrictMath.abs;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;
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


    @Nullable
    public static Task merge(@NotNull Task a, @NotNull Task b, long now, long newOcc, @NotNull Truth newTruth) {

        if (Compound.atemporallyEqual(a.term(), b.term())) {
            float aw = a.isQuestOrQuestion() ? 0 : c2w(a.conf()); //question
            float bw = c2w(b.conf());

            float aProp = aw / (aw + bw);

            FloatObjectPair<Compound> c = Revision.dtMerge(a.term(), b.term(), aProp);
            float adjustedDifference = c.getOne();

            float confScale;
            if (adjustedDifference > 0) {
                //normalize relative to the total difference involved
                long aocc = a.occurrence();
                if (aocc == ETERNAL) aocc = newOcc;
                long bocc = b.occurrence();
                if (bocc == ETERNAL) bocc = newOcc;
                confScale = (1f - (adjustedDifference /
                        (1 + Math.abs(aocc - newOcc) + Math.abs(bocc - newOcc))));
            } else {
                confScale = 1f;
            }

            float newConf = newTruth.conf() * confScale;
            if (newConf < Global.TRUTH_EPSILON) {
                //too weak
                return null;
            }

            long[] newEv = Stamp.zip(a.evidence(), b.evidence(), aProp);

            return new MutableTask(c.getTwo(),
                    a, b, now, newOcc, newEv,
                    newTruth.withConf(newConf),
                    BudgetMerge.plusDQBlend)
                    .log("Revection Merge");

        } else {
            //just project 'b' to 'a' time

            //    @Nullable
//    default Task answerProjected(@NotNull Task question, @NotNull Memory memory) {
//
//        float termRelevance = Terms.termRelevance(term(), question.term());
//        if (termRelevance == 0)
//            return null;
//
//        long now = memory.time();
//
//        //TODO avoid creating new Truth instances
//        Truth solTruth = projectTruth(question.occurrence(), now, true);
//        if (solTruth == null)
//            return null;
//
//        //if truth instanceof ProjectedTruth, use its attached occ time (possibly eternal or temporal), otherwise assume it is this task's occurence time
//        long solutionOcc = solTruth instanceof ProjectedTruth ?
//                ((ProjectedTruth)solTruth).when : occurrence();
//
//        if (solTruth.conf() < conf()) return this;
//
//        solTruth = solTruth.confMult(termRelevance);
//                //* BeliefTable.relevance(this, solutionOcc, memory.duration()));
//                //solTruth.withConf( w2c(solTruth.conf())* termRelevance );
//
//        if (solTruth.conf() < conf())
//            return this;
//
//        Budget solutionBudget = solutionBudget(question, this, solTruth, memory);
//        if (solutionBudget == null)
//            return null;
//
            //if ((!truth().equals(solTruth)) || (!newTerm.equals(term())) || (solutionOcc!= occCurrent)) {
            @NotNull Budget bb = b.budget();

            if (bb.isDeleted()) return null;

            Task solution = new MutableTask(b.term() /* question term in case it has different temporality */,
                    b.punc(), newTruth)
                    .time(now, newOcc)
                    .parent(b, a)
                    .budget(bb)
                    //.state(state())
                    //.setEvidence(evidence())
                    .log("Projected Answer")
                    //.log("Projected from " + this)
                    ;


            ////TODO avoid adding repeat & equal Solution instances
            //solution.log(new Solution(question));

            return solution;

        }
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
     * heuristic which evaluates the semantic similarity of two terms
     * returning 1f if there is a complete match, 0f if there is
     * a totally separate meaning for each, and in-between if
     * some intermediate aspect is different (ex: temporal relation dt)
     * <p>
     * evaluates the terms recursively to compare internal 'dt'
     * produces a tuple (merged, difference amount), the difference amount
     * can be used to attenuate truth values, etc.
     * <p>
     * TODO threshold to stop early
     */
    public static FloatObjectPair<Compound> dtMerge(@NotNull Compound a, @NotNull Compound b, float aProp) {
        if (a.equals(b)) {
            return PrimitiveTuples.pair(0f, a);
        }

        MutableFloat accumulatedDifference = new MutableFloat(0);
        Compound cc = dtMerge(a, b, aProp, accumulatedDifference, 1f);


        //how far away from 0.5 the weight point is, reduces the difference value because less will have changed
        float weightDivergence = 1f - (Math.abs(aProp - 0.5f) * 2f);

        return PrimitiveTuples.pair(accumulatedDifference.floatValue() * weightDivergence, cc);


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

    @NotNull
    private static Compound dtMerge(@NotNull Compound a, @NotNull Compound b, float balance, @NotNull MutableFloat accumulatedDifference, float depth) {
        int newDT;
        int adt = a.dt();
        if (a.size() != 2) {
            if (b.size() != a.size())
                throw new RuntimeException("err");
            return a;
        }

        if (adt != b.dt()) {

            int bdt = b.dt();
            if (adt != DTERNAL && bdt != DTERNAL) {
                newDT = Math.round(Util.lerp(adt, bdt, balance));
                accumulatedDifference.add(Math.abs(adt - bdt) * depth);
            } else if (bdt != DTERNAL) {
                newDT = bdt;
            }
            else if (adt != DTERNAL) {
                newDT = adt;
            }
            else {
                throw new RuntimeException();
            }
        } else {
            newDT = adt;
        }


        Term a0 = a.term(0);
        Term a1 = a.term(1);
        if (a0.op() != b.term(0).op() || (a1.op() != b.term(1).op())) {
            throw new RuntimeException();
        }

        return (Compound)$.compound(a.op(), a.relation(), newDT, new TermVector(
                (a0 instanceof Compound) ? dtMerge((Compound) a0, (Compound) (b.term(0)), balance, accumulatedDifference, depth / 2f) : a0,
                (a1 instanceof Compound) ? dtMerge((Compound) a1, (Compound) (b.term(1)), balance, accumulatedDifference, depth / 2f) : a1
        ));
        //if (a.op().temporal) //when would it not be temporal? this happens though
            //d = d.dt(newDT);
        //return d;
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