package nars.task;

import nars.$;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.nal.Stamp;
import nars.nal.UtilityFunctions;
import nars.nal.meta.PremiseEval;
import nars.table.DefaultBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.ProjectedTruth;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.*;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

    /**
     * Revision for Eternal tasks
     */
    @Nullable
    public static Truth revise(@NotNull Truthed a, @NotNull Truthed b, float factor, float minConf) {
        float w1 = a.confWeight() * factor;
        float w2 = b.confWeight() * factor;
        float w = (w1 + w2);
        float c = UtilityFunctions.w2c(w);
        if (c < minConf)
            return null;

        return $.t(
            (w1 * a.freq() + w2 * b.freq()) / w,
            c
        );
    }

    @Nullable
    public static Truth revise(@NotNull Truthed a, @NotNull Truthed b) {
        return revise(a, b, 1f, 0f);
    }


//    public static Task merge(@NotNull Task a, @NotNull Task b, long when, long now, @NotNull Truth newTruth, Concept concept) {
//
//        if (a.isBeliefOrGoal() && b.isBeliefOrGoal() && Term.equalAtemporally(a.term(), b.term())) {
//            return mergeInterpolate(a, b, when, now, newTruth, concept);
//
//
//        } else {
//            return mergeSolution(a, b, when, now, newTruth);
//
//        }
//    }

    public static Task mergeSolution(@NotNull Task a, @NotNull Task b, long when, long now, @NotNull Truth newTruth) {
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

        Task solution = new AnswerTask(b.term() /* question term in case it has different temporality */,
                b, a, newTruth, now, when, 0.5f)
                .log("Projected Answer")
                .budget(bb)
                //.state(state())
                //.setEvidence(evidence())

                //.log("Projected from " + this)
                ;


        ////TODO avoid adding repeat & equal Solution instances
        //solution.log(new Solution(question));

        return solution;
    }

    @Nullable public static Task mergeInterpolate(@NotNull Task a, @NotNull Task b, long when, long now, @NotNull Truth newTruth, Concept concept) {
        assert (a.punc() == b.punc());

        float aw = a.isQuestOrQuestion() ? 0 : a.confWeight(); //question
        float bw = b.confWeight();

        float aProp = aw / (aw + bw);

        //HACK create a temorary RNG because pulling one up through the method calls would be a mess
        Random rng = new XorShift128PlusRandom(Util.hashCombine(a.hashCode(), b.hashCode()) << 32 + Util.hashCombine((int) when, (int) now) * 31 + newTruth.hashCode());

        MutableFloat accumulatedDifference = new MutableFloat(0);
        Term cc = intermpolate(a.term(), b.term(), aProp, accumulatedDifference, 1f, rng);
        if (!(cc instanceof Compound))
            return null;

        //get a stamp collecting all evidence from the table, since it all contributes to the result
        //TODO weight by the relative confidence of each so that more confidence contributes more evidence data to the stamp
        long[] evidence = Stamp.zip(((DefaultBeliefTable) concept.tableFor(a.punc())).temporal);

        RevisionTask t = new RevisionTask(cc, a.punc(),
                newTruth,
                now, when,
                evidence
        );

        t.budget(a, b, aProp);

        if (Param.REVECTION_PRIORITY_ZERO)
            t.setPriority(0);

        t.log("Revection Merge");
        return t;
    }


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
    @NotNull
    public static Term intermpolate(@NotNull Term a, @NotNull Term b, float aProp, @NotNull MutableFloat accumulatedDifference, float depth, Random rng) {
        if (a.equals(b)) {
            return a;
        } else if (a instanceof Compound) {
            boolean sameOp = a.op() == b.op();
            boolean sameSize = a.size() == b.size();
            if (sameSize && sameOp) {
                if (a.op().temporal && a.size() == 2) {
                    return dtMergeTemporal((Compound)a, (Compound)b, aProp, accumulatedDifference, depth/2f, rng);
                } else {
                    if (!a.op().image) //dont fuck with images here
                        return dtMergeGeneric((Compound)a, (Compound)b, aProp, rng);
                }
            }
        }

        return choose(a, b, aProp, rng);

    }

    /** a and b must have same operator and size */
    @NotNull private static Term dtMergeGeneric(@NotNull Compound a, @NotNull Compound b, float aProp, Random rng) {
        return $.compound(a.op(), DTERNAL, /* although parallel could be maintained if this happens by choosing dt between a and b */
            choose(a.terms(), b.terms(), aProp, rng)
        );
    }

    @NotNull
    private static Term dtMergeTemporal(@NotNull Compound a, @NotNull Compound b, float aProp, @NotNull MutableFloat accumulatedDifference, float depth, Random rng) {

        Term a0, a1, b0, b1;
        int adt = a.dt();
        if ((adt >= 0) || (adt == DTERNAL)) {
            a0 = a.term(0); a1 = a.term(1);
        } else {
            a0 = a.term(1); a1 = a.term(0); adt = -adt;
        }
        int bdt = b.dt();
        if ((bdt >= 0) || (bdt == DTERNAL)) {
            b0 = b.term(0); b1 = b.term(1);
        } else {
            b0 = b.term(1); b1 = b.term(0); bdt = -bdt;
        }

        depth/=2f;
        return $.compound(a.op(), (choose(a, b, aProp, rng) == a) ? adt : bdt,
                intermpolate(a0, b0, aProp, accumulatedDifference, depth, rng),
                intermpolate(a1, b1, aProp, accumulatedDifference, depth, rng));

    }

    private static void failIntermpolation(@NotNull Compound a, @NotNull Compound b) {
        throw new RuntimeException("interpolation failure: different or invalid internal structure and can not be compared:\n\t" + a + "\n\t" + b);
    }

    private static int dtCompare(@NotNull Compound a, @NotNull Compound b, float aProp, @NotNull MutableFloat accumulatedDifference, float depth, Random rng) {
        int newDT;
        int adt = a.dt();
        if (adt != b.dt()) {

            int bdt = b.dt();
            if (adt != DTERNAL && bdt != DTERNAL) {

                accumulatedDifference.add(Math.abs(adt - bdt) * depth);

                //newDT = Math.round(Util.lerp(adt, bdt, aProp));
                if (rng != null)
                    newDT = choose(adt, bdt, aProp, rng);
                else
                    newDT = aProp > 0.5f ? adt : bdt;


            } else if (bdt != DTERNAL) {
                newDT = bdt;
                //accumulatedDifference.add(bdt * depth);

            } else if (adt != DTERNAL) {
                newDT = adt;
                //accumulatedDifference.add(adt * depth);
            } else {
                throw new RuntimeException();
            }
        } else {
            newDT = adt;
        }
        return newDT;
    }

    static int choose(int x, int y, float xProp, Random random) {
        return random.nextFloat() < xProp ? x : y;
    }

//    private static Compound failStrongest(Compound a, Compound b, float aProp) {
//        //logger.warn("interpolation failure: {} and {}", a, b);
//        return strongest(a, b, aProp);
//    }

    public static Compound choose(Compound a, Compound b, float balance) {
        return (balance >= 0.5f) ? a : b;
    }

    public static Term choose(Term a, Term b, float aBalance, Random rng) {
        return (rng.nextFloat() < aBalance) ? a : b;
    }
    public static Term[] choose(Term[] a, Term[] b, float aBalance, Random rng) {
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
                newBelief != oldBelief &&

                        //!(t.op().isConjunctive() && t.hasVarDep()) &&  // t.hasVarDep());

                        //!newBelief.equals(oldBelief) &&  //if it overlaps it will be equal, so just do overlap test
                        !Stamp.overlapping(newBelief, oldBelief);
    }

    /**
     * assumes the compounds are the same except for possible numeric metadata differences
     */
    public static @NotNull Compound intermpolate(@NotNull Termed<Compound> a, @NotNull Termed<Compound> b, float aConf, float bConf) {
        @NotNull Compound aterm = a.term();
        if (a.equals(b))
            return aterm;

        float aWeight = c2w(aConf);
        float bWeight = c2w(bConf);
        float aProp = aWeight / (aWeight + bWeight);

        @NotNull Compound bterm = b.term();

        int dt = DTERNAL;
        int at = aterm.dt();
        if (at != DTERNAL) {
            int bt = bterm.dt();
            if (bt != DTERNAL) {
                dt = Math.round(Util.lerp(at, bt, aProp));
            }
        }

        Term r = $.compound(a.op(), dt, aterm.terms());
        return !(r instanceof Compound) ? choose(aterm, bterm, aProp) : (Compound) r;
    }

    @Nullable
    public static ProjectedTruth project(@NotNull Truth t, long target, long now, long occ, boolean eternalizeIfWeaklyTemporal) {

        if (occ == target)
            return new ProjectedTruth(t, target);

        float conf = t.conf();

        float nextConf;


        float projConf = nextConf = conf * projection(target, occ, now);

        if (eternalizeIfWeaklyTemporal) {
            float eternConf = eternalize(conf);

            if (projConf < eternConf) {
                nextConf = eternConf;
                target = ETERNAL;
            }
        }

        if (nextConf < Param.TRUTH_EPSILON)
            return null;

        float maxConf = 1f - Param.TRUTH_EPSILON;
        if (nextConf > maxConf) //clip at max conf
            nextConf = maxConf;

        return new ProjectedTruth(t.freq(), nextConf, target);
    }

    @NotNull
    public static Task chooseByConf(@NotNull Task t, @Nullable Task b, @NotNull PremiseEval p) {

        if (b == null)
            return t;

        long to = t.occurrence();
        long bo = b.occurrence();

        if (to != ETERNAL && bo != ETERNAL) {

            //randomize choice by confidence
            float tc = t.confWeight();
            float tbc = tc + b.confWeight();

            return p.random.nextFloat() < tc/tbc ? t : b;

        } else {
            return bo != ETERNAL ? b : t;
        }
    }

    public static Term intermpolate(Term a, Term b, float aProp, Random rng) {
        return intermpolate(a, b, aProp, new MutableFloat(),1,rng);
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