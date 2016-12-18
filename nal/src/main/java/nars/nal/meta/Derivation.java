package nars.nal.meta;

import jcog.version.Versioned;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.budget.policy.TaskBudgeting;
import nars.nal.Premise;
import nars.nal.Stamp;
import nars.nal.meta.constraint.MatchConstraint;
import nars.op.DepIndepVarIntroduction;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.Unify;
import nars.term.transform.substitute;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.*;
import static nars.time.Tense.DTERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify {


    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public final Premise premise;
    public final float truthResolution;
    public final float quaMin;
    public final float premiseEvidence;

    public boolean setPunct(@Nullable Truth t, char p, long[] evidence) {
        return this.punct.set(new Derivation.TruthPuncEvidence(t, p, evidence))!=null;
    }

    @Nullable public Truth dither(@NotNull Truth t) {
        float res = this.truthResolution;
        return res == Param.TRUTH_EPSILON ? t : DefaultTruth.ditherOrNull(t, res);
    }

    public final long time() {
        return nar.time();
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
     * only continues termuting while matchesRemain > 0
     */
    int matchesRemain;

    final int matchesMax;


    /**
     * cached values
     */

    public final float confMin;

    /** op ordinals, 0=task, 1=belief */
    public final int termSub0op;
    public final int termSub1op;

    /** op bits, 0=task, 1=belief */
    public final int termSub0opBit;
    public final int termSub1opBit;

    /** structs, 0=task, 1=belief */
    public final int termSub0Struct;
    public final int termSub1Struct;

    public final boolean overlap;

    @Nullable
    public final Truth taskTruth;
    @Nullable
    public final Truth beliefTruth;

    @NotNull
    public final Compound taskTerm;
    @NotNull
    public final Term beliefTerm;
    @NotNull
    public final NAR nar;

    @NotNull
    public final Task task;
    @Nullable
    public final Task belief;
    public final char taskPunct;

    /**
     * whether the premise involves temporality that must be calculated upon derivation
     */
    public final boolean temporal;
    @Nullable
    private long[] evidenceDouble, evidenceSingle;

    @Nullable
    public final Consumer<DerivedTask> target;
    public final boolean cyclic;


    public Derivation(@NotNull NAR nar, @NotNull Premise p, @NotNull Consumer<DerivedTask> c) {
        super(nar.concepts, VAR_PATTERN, nar.random, Param.UnificationStackMax, Param.UnificationTermutesMax);

        this.nar = nar;
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());
        this.quaMin = nar.quaMin.floatValue();


        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        this.punct = new Versioned(versioning, 2);

        set(new substitute(this));
        set(new substituteIfUnifiesAny(this));
        set(new substituteIfUnifiesForward(this));
        set(new substituteIfUnifiesDep(this));
        set(new DepIndepVarIntroduction.VarIntro(nar));

        this.premise = p;

        Task task;
        this.task = task = p.task();
        Compound tt = task.term();
        Term taskTerm = this.taskTerm = tt;

        Task belief;
        this.belief = belief = p.belief();
        this.beliefTerm = p.beliefTerm();
        if (beliefTerm.op()==NEG) {
            throw new RuntimeException("negated belief term");
        }

        this.taskTruth = task.truth();
        this.taskPunct = task.punc();
        this.beliefTruth = belief != null ? belief.truth() : null;
        this.matchesMax = matchesMax(task.qua() /* .summary() */);

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

        this.cyclic = task.cyclic(); //belief cyclic should not be considered because in single derivation its evidence will not be used any way
        //NOT: this.cyclic = task.cyclic() || (belief != null && belief.cyclic());

        this.overlap = belief != null && Stamp.overlapping(task, belief);

        this.termSub0Struct = taskTerm.structure();

        Op tOp = taskTerm.op();
        this.termSub0op = tOp.ordinal();
        this.termSub0opBit = tOp.bit;

        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = bOp.ordinal();
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);

        this.target = c;

        float premiseEvidence = task.isBeliefOrGoal() ? task.evi() : 0;
        if (belief!=null)
            premiseEvidence = Math.max(premiseEvidence, belief.evi());
        this.premiseEvidence = premiseEvidence;

    }

    protected final void put(@NotNull Term t) {
        putXY(t, t);
    }

    protected final void set(@NotNull Term t) {
        setXY(t, t);
    }

    public static int matchesMax(float p) {
        final float min = Param.UnificationMatchesMin, max = Param.UnificationMatchesMax;
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
            this.matchesRemain = Math.max(1, matchesMax / matchFactor);
            finish = true;
        } else {
            this.matchesRemain = -1; //will not apply unless eachMatch!=null (final step)
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
    public final boolean onMatch() {

 //       try {
//            if (!forEachMatch.run(this, now()))
//                return false;
//        } catch (RuntimeException e) {
//            if (Param.DEBUG_DERIVER)
//                Conclude.logger.warn("{}\n\tderiving {}", e, ((Conclude)forEachMatch).rule.source);
//            //continue
//        }

        //return forEachMatch.run(this, now()) && (--matchesRemain > 0);
        return  (--matchesRemain > 0) && forEachMatch.run(this, now());
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

        return
                TaskBudgeting.derivation(truth, derived, this);
    }


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

    public void replaceAllXY(@NotNull Unify m) {
        m.xy.forEachVersioned(this::replaceXY);
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
//            if ((task.cyclic() || belief.cyclic()) && Stamp.isCyclic(evidenceDouble))
//                throw new RuntimeException("cyclic should not be propagated");
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

    public boolean overlap(boolean single) {
        return single ? cyclic : overlap;
    }
}


