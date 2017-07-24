package nars.task;

import jcog.math.Interval;
import nars.$;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static jcog.Util.lerp;
import static nars.time.Tense.DTERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

    @Nullable
    public static Truth revise(@NotNull Truthed a, @NotNull Truthed b, float factor, float minEvi) {
        float w1 = a.evi() * factor;
        float w2 = b.evi() * factor;
        float w = (w1 + w2);
        return w <= minEvi ?
                null :
                new PreciseTruth(
                        (w1 * a.freq() + w2 * b.freq()) / w,
                        w,
                        false
                );
    }

    @Nullable
    public static Truth revise(@NotNull Iterable<? extends Truthed> aa, float minConf) {
        float f = 0;
        float w = 0;
        for (Truthed x : aa) {
            float e = x.evi();
            w += e;
            f += x.freq() * e;
        }
        if (w <= 0)
            return null;

        float c = w2c(w);
        return c < minConf ? null :
                $.t(
                        (f) / w,
                        c
                );
    }

    public static Truth merge(@NotNull Truth newTruth, @NotNull Truthed a, float aFrequencyBalance, @NotNull Truthed b, float minConf, float confMax) {
        float w1 = a.evi();
        float w2 = b.evi();
//        float w = (w1 + w2) * evidenceFactor;

////        if (w2c(w) >= minConf) {
//            //find the right balance of frequency
//            float w1f = aFrequencyBalance * w1;
//            float w2f = (1f - aFrequencyBalance) * w2;
//            float p = w1f / (w1f + w2f);
//
//            float af = a.freq();
//            float bf = b.freq();
//            float f = lerp(p, bf, af);

//            //compute error (difference) in frequency TODO improve this
//            float fError =
//                    Math.abs(f - af) * w1f +
//                    Math.abs(f - bf) * w2f;
//
//            w -= fError;

//            float c = w2c(w);
//            if (c >= minConf) {
//                return $.t(f, Math.min(confMax, c));
//            }

//        }

        return null;
    }


    public static Truth revise(@NotNull Truthed a, @NotNull Truthed b) {
        return revise(a, b, 1f, 0f);
    }


    public static Task mergeInterpolate(@NotNull Task a, @NotNull Task b, long start, long end, long now, Truth newTruth, boolean mergeOrChoose, Random rng) {
        assert (a.punc() == b.punc());

        float aw = a.isQuestOrQuestion() ? 0 : a.evi(); //question
        float bw = b.evi();

        float aProp = aw / (aw + bw);

        boolean negated = false;
        Term cc = null;

        for (int i = 0; i < Param.MAX_TERMPOLATE_RETRIES; i++) {
            ObjectBooleanPair<Term> ccp = Task.tryContent(
                    intermpolate(a.term(), b.term(), aProp, new MutableFloat(0), 1f, rng, mergeOrChoose),
                    a.punc(), $.terms, true);


            if (ccp != null) {

                cc = ccp.getOne();
                assert (cc.isNormalized());

                negated = ccp.getTwo();
                break;
            }
        }

        if (cc == null)
            return null;


        if (negated) {
            newTruth = newTruth.negated();
        }


        NALTask t = new NALTask(cc, a.punc(),
                newTruth,
                now, start, end,
                Stamp.zip(a.stamp(), b.stamp(), aProp) //get a stamp collecting all evidence from the table, since it all contributes to the result
        );
        t.setPri(a.priElseZero() + b.priElseZero());
        t.cause = Cause.zip(a, b);
        //t.log("Revection Merge");
        return t;
    }


    @NotNull
    public static Term intermpolate(@NotNull Term a, @NotNull Term b, float aProp, @NotNull MutableFloat accumulatedDifference, float curDepth, @NotNull Random rng, boolean mergeOrChoose) {
        if (a.equals(b)) {
            return a;
        } else if (a instanceof Compound) {
            boolean sameOp = a.op() == b.op();
            int len = a.size();
            boolean sameSize = (len == b.size());
            if (sameSize && sameOp) {
                Compound ca = (Compound) a;
                Compound cb = (Compound) b;
                if (a.op().temporal && len == 2) {
                    return dtMergeTemporal(ca, cb, aProp, accumulatedDifference, curDepth / 2f, rng, mergeOrChoose);
                } else {
                    //assert(ca.dt()== cb.dt());

                    //Term[] x = choose(ca.terms(), cb.terms(), aProp, rng)

                    Term[] x = new Term[len];
                    for (int i = 0; i < len; i++) {
                        x[i] = intermpolate(ca.sub(i), cb.sub(i), aProp, accumulatedDifference, curDepth / 2f, rng, mergeOrChoose);
                    }

                    return ca.op().the( ca.dt(), x );
                }
            }
        }

        return choose(a, b, aProp, rng);

    }

    @NotNull
    private static Term dtMergeTemporal(@NotNull Compound a, @NotNull Compound b, float aProp, @NotNull MutableFloat accumulatedDifference, float depth, @NotNull Random rng, boolean mergeOrChoose) {

        int adt = a.dt();

        int bdt = b.dt();


        depth /= 2f;

        int dt;
        if (adt == DTERNAL)
            dt = bdt;
        else if (bdt == DTERNAL)
            dt = adt;
        else {
            dt = mergeOrChoose ?
                    (lerp(aProp, bdt, adt)) :
                    ((choose(a, b, aProp, rng) == a) ? adt : bdt);
        }

        Term a0, a1, b0, b1;
        if ((adt >= 0) || (adt == DTERNAL)) {
            a0 = a.sub(0);
            a1 = a.sub(1);
        } else {
            a0 = a.sub(1);
            a1 = a.sub(0);
        }
        if ((bdt >= 0) || (bdt == DTERNAL)) {
            b0 = b.sub(0);
            b1 = b.sub(1);
        } else {
            b0 = b.sub(1);
            b1 = b.sub(0);
        }
        return a.op().the(dt,
                intermpolate(a0, b0, aProp, accumulatedDifference, depth, rng, mergeOrChoose),
                intermpolate(a1, b1, aProp, accumulatedDifference, depth, rng, mergeOrChoose));

    }

    public static Term choose(Term a, Term b, float aBalance, @NotNull Random rng) {
        return (rng.nextFloat() < aBalance) ? a : b;
    }

    @NotNull
    public static Term[] choose(@NotNull Term[] a, Term[] b, float aBalance, @NotNull Random rng) {
        int l = a.length;
        Term[] x = new Term[l];
        for (int i = 0; i < l; i++) {
            x[i] = choose(a[i], b[i], aBalance, rng);
        }
        return x;
    }

    /**
     * WARNING: this assumes the task's terms are already
     * known to be equal.
     */
    public static boolean isRevisible(@NotNull Task newBelief, @NotNull Task oldBelief) {
        //Term t = newBelief.term();
        return


                //!(t.op().isConjunctive() && t.hasVarDep()) &&  // t.hasVarDep());

                //!newBelief.equals(oldBelief) &&  //if it overlaps it will be equal, so just do overlap test
                !Stamp.overlapping(newBelief, oldBelief);
    }


    @NotNull
    public static Task chooseByConf(@NotNull Task t, @Nullable Task b, @NotNull Derivation p) {

        if ((b == null) || !b.isBeliefOrGoal())
            return t;

        //int dur = p.nar.dur();
        float tw = t.conf();
        float bw = b.conf();

        //randomize choice by confidence
        return p.random.nextFloat() < tw / (tw + bw) ? t : b;

    }

    public static Term intermpolate(@NotNull Term a, @NotNull Term b, float aProp, @NotNull Random rng, boolean mergeOrChoose) {
        return intermpolate(a, b, aProp, /* unused: */ new MutableFloat(), 1, rng, mergeOrChoose);
    }

    /**
     * t is the target time of the new merged task
     */
    public static Task merge(@NotNull Task a, @NotNull Task b, long now, float confMin, Random rng) {


        Interval ai = new Interval(a.start(), a.end());
        Interval bi = new Interval(b.start(), b.end());

        Interval timeOverlap = ai.intersection(bi);

        long start, end;
        @Nullable Truth newTruth;

//            float ae = a.evi();
//            float aa = ae * (1 + ai.length());
//            float be = b.evi();
        //float bb = be * (1 + bi.length());
        //float p = aa / (aa + bb);

//            //relate high frequency difference with low confidence
//            float freqDiscount =
//                    (1f - 0.5f * Math.abs(a.freq() - b.freq()));
//
            float stampDiscount =
//                //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
//                //TODO weight the contributed overlap amount by the relative confidence provided by each task
                    1f - Stamp.overlapFraction(a.stamp(), b.stamp()) / 2f;
//
////            //relate to loss of stamp when its capacity to contain the two incoming is reached
////            float stampCapacityDiscount =
////                    Math.min(1f, ((float) Param.STAMP_CAPACITY) / (a.stamp().length + b.stamp().length));
//
//
//            float temporalOverlap = timeOverlap==null || timeOverlap.length()==0 ? 0 : timeOverlap.length()/((float)Math.min(ai.length(), bi.length()));
//            float confMax = Util.lerp(temporalOverlap, Math.max(w2c(ae),w2c(be)),  1f);
//
//
//            float timeDiscount = 1f;
//            if (timeOverlap == null) {
//                long separation = Math.max(a.timeDistance(b.start()), a.timeDistance(b.end()));
//                if (separation > 0) {
//                    long totalLength = ai.length() + bi.length();
//                    timeDiscount =
//                            (totalLength) /
//                                    (separation + totalLength)
//                    ;
//                }
//            }


        //width will be the average width
        long width = (ai.length() + bi.length()) / 2; //TODO weight
        long mid = (ai.mid() + bi.mid()) / 2;  //TODO weight

//            Truth expected = table.truth(mid, now, dur);
//            if (expected == null)
//                return null;


        start = mid - width / 2;
        end = mid + width / 2;

        long u = ai.union(bi).length();
        long s = ai.length() + bi.length();

//            Truth startTruth = table.truth(start, now, dur);
//            if (startTruth == null)
//                return null;
//
//            Truth endTruth = table.truth(end, now, dur);
//            if (endTruth == null)
//                return null;

        float factor = 1f * stampDiscount;

//            //the degree to which start truth and endtruth deviate from a horizontal line is the evidence reduction factor
//            //this is because the resulting task is analogous to the horizontal line the endpoint values deviate from
//            float diff = Math.abs(startTruth.freq() - endTruth.freq());
//            if (diff > 0)
//                factor *= (1f - diff);


        if (timeOverlap == null) {
            factor *= ((float) s) / (s + u);
        }

        newTruth = revise(a, b, factor, c2w(confMin));


//            float conf = w2c(expected.evi() * factor);
//            if (conf >= Param.TRUTH_EPSILON)
//                newTruth = new PreciseTruth(expected.freq(), conf);
//            else
//                newTruth = null;


        if (newTruth != null) {
            return mergeInterpolate(a, b, start, end, now, newTruth, true, rng);
        }

        return null;
    }
}

//    /** get the task which occurrs nearest to the target time */
//    @NotNull public static Task closestTo(@NotNull Task[] t, long when) {
//        Task best = t[0];
//        long bestDiff = Math.abs(when - best.occurrence());
//        for (int i = 1; i < t.length; i++) {
//            Task x = t[i];
//            long o = x.occurrence();
//            long diff = Math.abs(when - o);
//            if (diff < bestDiff) {
//                best = x;
//                bestDiff = diff;
//            }
//        }
//        return best;
//    }


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
//        int durability =
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

//    /**
//     * assumes the compounds are the same except for possible numeric metadata differences
//     */
//    public static @NotNull Compound intermpolate(@NotNull Termed<Compound> a, @NotNull Termed<Compound> b, float aConf, float bConf, @NotNull TermIndex index) {
//        @NotNull Compound aterm = a.term();
//        if (a.equals(b))
//            return aterm;
//
//        float aWeight = c2w(aConf);
//        float bWeight = c2w(bConf);
//        float aProp = aWeight / (aWeight + bWeight);
//
//        @NotNull Compound bterm = b.term();
//
//        int dt = DTERNAL;
//        int at = aterm.dt();
//        if (at != DTERNAL) {
//            int bt = bterm.dt();
//            if (bt != DTERNAL) {
//                dt = lerp(at, bt, aProp);
//            }
//        }
//
//
//        Term r = index.the(a.op(), dt, aterm.terms());
//        return !(r instanceof Compound) ? choose(aterm, bterm, aProp) : (Compound) r;
//    }

//    @Nullable
//    public static ProjectedTruth project(@NotNull Truth t, long target, long now, long occ, boolean eternalizeIfWeaklyTemporal) {
//
//        if (occ == target)
//            return new ProjectedTruth(t, target);
//
//        float conf = t.conf();
//
//        float nextConf;
//
//
//        float projConf = nextConf = conf * projection(target, occ, now);
//
//        if (eternalizeIfWeaklyTemporal) {
//            float eternConf = eternalize(conf);
//
//            if (projConf < eternConf) {
//                nextConf = eternConf;
//                target = ETERNAL;
//            }
//        }
//
//        if (nextConf < Param.TRUTH_EPSILON)
//            return null;
//
//        float maxConf = 1f - Param.TRUTH_EPSILON;
//        if (nextConf > maxConf) //clip at max conf
//            nextConf = maxConf;
//
//        return new ProjectedTruth(t.freq(), nextConf, target);
//    }
//    private static void failIntermpolation(@NotNull Compound a, @NotNull Compound b) {
//        throw new RuntimeException("interpolation failure: different or invalid internal structure and can not be compared:\n\t" + a + "\n\t" + b);
//    }
//
//    private static int dtCompare(@NotNull Compound a, @NotNull Compound b, float aProp, @NotNull MutableFloat accumulatedDifference, float depth, @Nullable Random rng) {
//        int newDT;
//        int adt = a.dt();
//        if (adt != b.dt()) {
//
//            int bdt = b.dt();
//            if (adt != DTERNAL && bdt != DTERNAL) {
//
//                accumulatedDifference.add(Math.abs(adt - bdt) * depth);
//
//                //newDT = Math.round(Util.lerp(adt, bdt, aProp));
//                if (rng != null)
//                    newDT = choose(adt, bdt, aProp, rng);
//                else
//                    newDT = aProp > 0.5f ? adt : bdt;
//
//
//            } else if (bdt != DTERNAL) {
//                newDT = bdt;
//                //accumulatedDifference.add(bdt * depth);
//
//            } else if (adt != DTERNAL) {
//                newDT = adt;
//                //accumulatedDifference.add(adt * depth);
//            } else {
//                throw new RuntimeException();
//            }
//        } else {
//            newDT = adt;
//        }
//        return newDT;
//    }

//    static int choose(int x, int y, float xProp, @NotNull Random random) {
//        return random.nextFloat() < xProp ? x : y;
//    }

//    private static Compound failStrongest(Compound a, Compound b, float aProp) {
//        //logger.warn("interpolation failure: {} and {}", a, b);
//        return strongest(a, b, aProp);
//    }


//    /**
//     * heuristic which evaluates the semantic similarity of two terms
//     * returning 1f if there is a complete match, 0f if there is
//     * a totally separate meaning for each, and in-between if
//     * some intermediate aspect is different (ex: temporal relation dt)
//     * <p>
//     * evaluates the terms recursively to compare internal 'dt'
//     * produces a tuple (merged, difference amount), the difference amount
//     * can be used to attenuate truth values, etc.
//     * <p>
//     * TODO threshold to stop early
//     */
//    public static FloatObjectPair<Compound> dtMerge(@NotNull Compound a, @NotNull Compound b, float aProp, Random rng) {
//        if (a.equals(b)) {
//            return PrimitiveTuples.pair(0f, a);
//        }
//
//        MutableFloat accumulatedDifference = new MutableFloat(0);
//        Term cc = dtMerge(a, b, aProp, accumulatedDifference, 1f, rng);
//
//
//        //how far away from 0.5 the weight point is, reduces the difference value because less will have changed
//        float weightDivergence = 1f - (Math.abs(aProp - 0.5f) * 2f);
//
//        return PrimitiveTuples.pair(accumulatedDifference.floatValue() * weightDivergence, cc);
//
//
////            int at = a.dt();
////            int bt = b.dt();
////            if ((at != bt) && (at!=DTERNAL) && (bt!=DTERNAL)) {
//////                if ((at == DTERNAL) || (bt == DTERNAL)) {
//////                    //either is atemporal but not both
//////                    return 0.5f;
//////                }
////
//////                boolean symmetric = aop.isCommutative();
//////
//////                if (symmetric) {
//////                    int ata = Math.abs(at);
//////                    int bta = Math.abs(bt);
//////                    return 1f - (ata / ((float) (ata + bta)));
//////                } else {
//////                    boolean ap = at >= 0;
//////                    boolean bp = bt >= 0;
//////                    if (ap ^ bp) {
//////                        return 0; //opposite direction
//////                    } else {
//////                        //same direction
////                        return 1f - (Math.abs(at - bt) / (1f + Math.abs(at + bt)));
//////                    }
//////                }
////            }
////        }
////        return 1f;
//    }


//    /**
//     * computes a value that indicates the amount of difference (>=0) in the internal 'dt' subterm structure of 2 temporal compounds
//     */
//    @NotNull
//    public static float dtDifference(@Nullable Termed<Compound> a, @NotNull Termed<Compound> b) {
//        if (a == null) return 0f;
//
//        MutableFloat f = new MutableFloat(0);
//        dtDifference(a.term(), b.term(), f, 1f);
//        return f.floatValue();
//    }

//    @NotNull
//    private static void dtDifference(@NotNull Term a, @NotNull Term b, @NotNull MutableFloat accumulatedDifference, float depth) {
//        if (a.op() == b.op()) {
//            if (a.size() == 2 && b.size() == 2) {
//
//                if (a.equals(b))
//                    return; //no difference
//
//                Compound aa = ((Compound) a);
//                Compound bb = ((Compound) b);
//
//                dtCompare(aa, bb, 0.5f, accumulatedDifference, depth, null);
//            }
////            if (a.size() == b.size())
////
////                Term a0 = aa.term(0);
////                if (a.size() == 2 && b0) {
////                    Term b0 = bb.term(0);
////
////                    if (a0.op() == b0.op()) {
////                        dtCompare((Compound) a0, (Compound) b0, 0.5f, accumulatedDifference, depth / 2f, null);
////                    }
////
////                    Term a1 = aa.term(1);
////                    Term b1 = bb.term(1);
////
////                    if (a1.op() == b1.op()) {
////                        dtCompare((Compound) a1, (Compound) b1, 0.5f, accumulatedDifference, depth / 2f, null);
////                    }
////
////                }
////
////            }
//        } /* else: can not be compared anyway */
//    }
//
