package nars.nal.meta;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.budget.policy.TaskBudgeting;
import nars.nal.Conclusion;
import nars.nal.Deriver;
import nars.nal.Premise;
import nars.nal.Stamp;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.transform.substitute;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.FindSubst;
import nars.time.Tense;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.*;
import static nars.time.Tense.DTERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class PremiseEval extends FindSubst {


    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public final Premise premise;
    private final float truthResolution;

    public boolean setPunct(@Nullable Truth t, char p, long[] evidence) {

        if (t!=null) {
            t = dither(t);
            assert(t!=null); //confMin should be greater than or equal to truthResolution
        }

        return this.punct.set(new PremiseEval.TruthPuncEvidence(t, p, evidence))!=null;
    }

    @Nullable public Truth dither(@NotNull Truth t) {
        float res = this.truthResolution;
        return res == Param.TRUTH_EPSILON ? t : DefaultTruth.ditherOrNull(t, res);
    }

    public static final class TruthPuncEvidence {
        public final Truth truth;
        public final char punc;
        public final long[] evidence;

        public TruthPuncEvidence(Truth truth, char punc, long[] evidence) {
            this.truth = truth;
            this.punc = punc;
            this.evidence = evidence;
        }

    }

    @NotNull
    public final Versioned<TruthPuncEvidence> punct;


    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Nullable
    @Deprecated
    public BoolCondition forEachMatch;


    /**
     * run parameters
     */
    int termutesRemain;
    private int termutesMax;


    /**
     * cached value
     */
    public float confMin = Param.TRUTH_EPSILON;

    /** op ordinals, 0=task, 1=belief */
    public int termSub0op, termSub1op;

    /** op bits, 0=task, 1=belief */
    public int termSub0opBit, termSub1opBit;

    /** structs, 0=task, 1=belief */
    public int termSub0Struct, termSub1Struct;

    public boolean overlap;

    @Nullable
    public Truth taskTruth, beliefTruth;

    public Compound taskTerm;
    public Term beliefTerm;
    public final NAR nar;

    public Task task;
    @Nullable
    public Task belief;
    public char taskPunct;

    /**
     * whether the premise involves temporality that must be calculated upon derivation
     */
    public boolean temporal;
    @Nullable
    private long[] evidenceDouble, evidenceSingle;

    @Nullable
    public Conclusion conclusion;
    private boolean cyclic;

    public PremiseEval(@NotNull NAR nar) {
        this(nar, null);
    }

    public PremiseEval(@NotNull NAR nar, @Nullable Premise p) {
        super(nar.index, VAR_PATTERN, nar.random);

        this.nar = nar;
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());

        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        this.punct = new Versioned(versioning, 2);

        replace(new substitute(this));
        replace(new substituteIfUnifiesDep(this));
        replace(new substituteOnlyIfUnifiesDep(this));
        replace(new substituteIfUnifiesIndep(this));
        replace(new substituteIfUnifiesIndepForward(this));
        replace(new substituteOnlyIfUnifiesIndep(this));

        this.premise = p;
    }

    public PremiseEval(@NotNull NAR nar, @NotNull Deriver deriver, @NotNull Premise p, @NotNull Conclusion c) {
        this(nar, p);

        Task task;
        this.task = task = p.task();
        Compound tt = task.term();
        Term taskTerm = this.taskTerm = tt;

        Task belief;
        this.belief = belief = p.belief();
        Term beliefTerm = this.beliefTerm = p.beliefTerm().term();


        this.taskTruth = task.truth();
        this.taskPunct = task.punc();
        this.beliefTruth = belief != null ? belief.truth() : null;
        this.termutesMax = matchesMax(task.summary());

//        //normalize to positive truth
//        if (taskTruth != null && Global.INVERT_NEGATIVE_PREMISE_TASK && taskTruth.isNegative()) {
//            this.taskInverted = true;
//            this.taskTruth = this.taskTruth.negated();
//        } else {
//            this.taskInverted = false;
//        }
//
//        //normalize to positive truth
//        if (beliefTruth!=null && Global.INVERT_NEGATIVE_PREMISE_TASK && beliefTruth.isNegative()) {
//            this.beliefInverted = true;
//            this.beliefTruth = this.beliefTruth.negated();
//        } else {
//            this.beliefInverted = false;
//        }

        //this.cyclic = task.cyclic();
        this.cyclic = task.cyclic() || (belief != null && belief.cyclic());

        this.overlap = belief != null && Stamp.overlapping(task, belief);

        this.termSub0Struct = taskTerm.structure();

        @NotNull Op tOp = taskTerm.op();
        this.termSub0op = tOp.ordinal();
        this.termSub0opBit = tOp.bit;

        this.termSub1Struct = beliefTerm.structure();

        @NotNull Op bOp = beliefTerm.op();
        this.termSub1op = bOp.ordinal();
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);


        this.conclusion = c;

        //revert(start); //do this before starting in case the last execution was interrupted

        deriver.run(this);

        //this.conclusion = null; //forget a reference to the local field copy but return this instance
        //this.evidenceDouble = this.evidenceSingle = null;

    }

    protected final void put(@NotNull Term t) {
        putXY(t, t);
    }
    protected final void replace(@NotNull Term t) {
        replaceXY(t, t);
    }

    public static int matchesMax(float p) {
        final float min = Param.matchTermutationsMin, max = Param.matchTermutationsMax;
        return (int) Math.ceil(p * (max - min) + min);
    }


    /**
     * only one thread should be in here at a time
     */
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable BoolCondition eachMatch, @Nullable MatchConstraint constraints, int matchFactor) {

        int t = now();

        this.forEachMatch = eachMatch; //to notify of matches
        boolean finish;
        if (eachMatch != null) {
            //set the # of matches according to the # of conclusions in this branch
            //each matched termutation will be used to derive F=matchFactor conclusions,
            //so divide the premiseMatches value by it to equalize the derivation quantity
            this.termutesRemain = Math.max(1, termutesMax / matchFactor);
            finish = true;
        } else {
            this.termutesRemain = -1; //will not apply unless eachMatch!=null (final step)
            finish = false;
        }

        if (constraints != null) {
            if (this.constraints.set(constraints)==null)
                return;
        }

        unify(x, y, !finish, finish);

        this.forEachMatch = null;

        if (finish) {
            versioning.revert(t);
        } //else: allows the set constraints to continue


    }

    @Override
    public boolean onMatch() {
        if (termutesRemain-- < 0) {
            return false;
        }
        try {
            return forEachMatch.booleanValueOf(this, now());
        } catch (RuntimeException e) {
            if (Param.DEBUG_DERIVER)
                Conclude.logger.warn("{}\n\tderiving {}", e, ((Conclude)forEachMatch).rule.source);
            return false;
        }
    }




    private static boolean temporal(@NotNull Task task, @Nullable Task belief) {
        if (!task.isEternal() || task.dt() != DTERNAL)
            return true;

        return belief != null && (!belief.isEternal() || belief.dt() != DTERNAL);
    }


    /**
     * calculates Budget used in a derived task,
     * returns null if invalid / insufficient
     */
    @Nullable
    public final Budget budget(@Nullable Truth truth, @NotNull Termed derived) {
        float minDur = nar.durMin.floatValue();
        return (truth != null) ?
                TaskBudgeting.derivationForward(truth, derived, this, minDur) :
                TaskBudgeting.derivationBackward(derived, this, minDur);
    }


    ;

    public final long occurrenceTarget(@NotNull OccurrenceSolver s) {
        long tOcc = task.occurrence();
        Task b = belief;
        if (b == null) {
            return tOcc;
        } else {
            long bOcc = b.occurrence();
            return s.compute(tOcc, bOcc);

//            //if (bOcc == ETERNAL) {
//            return (tOcc != ETERNAL) ?
//                        whenBothNonEternal.compute(tOcc, bOcc) :
//                        ((bOcc != ETERNAL) ?
//                            bOcc :
//                            ETERNAL
//            );
        }
    }


//    /** specific minimum confidence function for advanced filtering heuristics TODO */
//    public final float confidenceMin(Term pattern, char punc) {
//
////        //EXAMPLE TEMPORARY HACK
////        Op o = pattern.op();
////        if (o!=VAR_PATTERN) {
////            int str = pattern.structure();
////
////            if ((Op.hasAny(str, Op.EQUIV) || (o == Op.INHERIT)))
////                return minConfidence * 3;
////        }
//
//        return confMin;
//    }

    /**
     * gets the op of the (top-level) pattern being compared
     *
     * @param subterm 0 or 1, indicating task or belief
     */
    /*public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub0op : termSub1op) == op;
    }*/
    public final int subOp(int i /* 0 or 1 */) {
        return (i == 0 ? termSub0op : termSub1op);
    }

    /**
     * @param subterm 0 or 1, indicating task or belief
     */
    public final boolean subTermMatch(int subterm, int bits) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Op.hasAll((subterm == 0 ? termSub0Struct : termSub1Struct), bits);
    }

    /**
     * both
     */
    public final boolean subTermsMatch(int bits) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Op.hasAll(termSub0Struct, bits) &&
                Op.hasAll(termSub1Struct, bits);
    }

//    /** returns whether the put operation was successful */
//    public final boolean putXY(Term k, Versioned<Term> vv) {
//        Term v = vv.get();
//        if (v != null) {
//            return putXY(k, v);
//        }
//        return false;
//    }

//    /** copy the new mappings to the match; returns false if there was an error, true if successful or if it was empty */
//    public final boolean putAllXY(Subst m) {
//        if (m instanceof FindSubst) {
//            return ((FindSubst) m).forEachVersioned((BiPredicate<Term,Versioned>)this::putXY);
//        } else {
//            if (!m.isEmpty()) {
//                return m.forEach((BiPredicate<Term,Term>)this::putXY);
//            }
//        }
//        return true;
//    }

    public void replaceAllXY(@NotNull FindSubst m) {
        m.forEachVersioned(this::replaceXY);
    }


    @Nullable
    public long[] evidence(boolean single) {

        return single ? evidenceSingle() : evidenceDouble();
    }

    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = Stamp.cyclic(task.evidence());
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            evidenceDouble = Stamp.zip(task.evidence(), belief.evidence());
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

    public boolean overlap(boolean single) {
        return single ? cyclic : overlap;
    }
}


