package nars.nal.meta;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.budget.Budget;
import nars.budget.policy.TaskBudgeting;
import nars.index.TermIndex;
import nars.nal.Conclusion;
import nars.nal.Premise;
import nars.nal.Deriver;
import nars.nal.Stamp;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.op.Solve;
import nars.nal.op.substitute;
import nars.nal.op.substituteIfUnifies;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.FindSubst;
import nars.term.subst.OneMatchFindSubst;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.Op.VAR_PATTERN;
import static nars.nal.Tense.DTERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class PremiseEval extends FindSubst {

    @NotNull private final Deriver deriver;
    private final int start;


    /** the current premise being evaluated in this context TODO make private again */
    public transient Premise premise;


    @NotNull
    public final Versioned<Character> punct;


    /** current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee */
    @Nullable
    @Deprecated public ProcTerm forEachMatch;



    /** run parameters */
    int termutes;
    private int termutesMax;


    /** cached value */
    public float confMin = Param.TRUTH_EPSILON;
    public int termSub0op, termSub1op;
    public int termSub0Struct, termSub1Struct;
    //@Deprecated public boolean cyclic; //TODO if this is necessary, encode this in the stamp as a -1 element prefix which will indicate that it inherited evidence directly from a parent in a single-premise derivation
    public boolean overlap;

    @Nullable public Truth taskTruth, beliefTruth;

    public Compound taskTerm;
    public Term beliefTerm;
    public NAR nar;

    public Task task;
    @Nullable public Task belief;
    public char taskPunct;

    /** whether the premise involves temporality that must be calculated upon derivation */
    public boolean temporal;
    private long[] evidence;

    public Conclusion conclusion;


    /** initializes with the default static term index/builder */
    public PremiseEval(Random r, @NotNull Deriver deriver) {
        this($.terms, r, deriver);
    }

    public PremiseEval(TermIndex index, Random r, @NotNull Deriver deriver) {
        super(index, VAR_PATTERN, r );


        this.deriver = deriver;
        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        punct = new Versioned(versioning);

        put(new substitute(this));

        OneMatchFindSubst subMatcher = new OneMatchFindSubst(index, r);
        put(new substituteIfUnifies.substituteIfUnifiesDep(this, subMatcher));
        put(new substituteIfUnifies.substituteOnlyIfUnifiesDep(this, subMatcher));
        put(new substituteIfUnifies.substituteIfUnifiesIndep(this, subMatcher));
        put(new substituteIfUnifies.substituteOnlyIfUnifiesIndep(this, subMatcher));

        this.start = now();

    }

    protected void put(@NotNull Term t) {
        putXY(t,t);
    }

    public int matchesMax(float p) {
        final float min = Param.matchTermutationsMin, max = Param.matchTermutationsMax;
        return (int) Math.ceil(p * (max - min) + min);
    }


    /** only one thread should be in here at a time */
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable ProcTerm eachMatch, @Nullable MatchConstraint constraints, int matchFactor) {

        int t = now();

        this.forEachMatch = eachMatch; //to notify of matches
        boolean finish;
        if (eachMatch!=null) {
            //set the # of matches according to the # of conclusions in this branch
            //each matched termutation will be used to derive F=matchFactor conclusions,
            //so divide the premiseMatches value by it to equalize the derivation quantity
            this.termutes = Math.max(1, termutesMax / matchFactor);
            finish = true;
        } else {
            this.termutes = -1; //will not apply unless eachMatch!=null (final step)
            finish = false;
        }

        if (constraints!=null)
            this.constraints.set( constraints );

        matchAll(x, y, finish);

        this.forEachMatch = null;

        if (finish) {
            versioning.revert(t);
        } //else: allows the set constraints to continue


    }

    @Override
    public boolean onMatch() {
        if (termutes-- >= 0) {
            forEachMatch.accept(this);
            return true;
        }
        return false;
    }




    @NotNull
    @Override
    public String toString() {
        return "RuleMatch:{" +
                "premise:" + premise +
                ", subst:" + super.toString() +
                (forEachMatch !=null ? (", derived:" + forEachMatch) : "")+
                //(!secondary.isEmpty() ? (", secondary:" + secondary) : "")+
                //(occurrenceShift.get()!=null ? (", occShift:" + occurrenceShift) : "")+
                //(branchPower.get()!=null ? (", derived:" + branchPower) : "")+
                '}';

    }


    public void init(@NotNull NAR nar) {
        this.nar = nar;
        this.confMin = nar.confMin.floatValue();
    }

    /**
     * execute the next premise, be sure to call init() before a batch of run()'s
     */
    public final Conclusion run(@NotNull Premise p, @NotNull Conclusion c) {

        Task task;
        this.task = task = p.task();

        this.punct.set(this.taskPunct = task.punc());

        this.premise = p;

        Task belief;
        this.belief = belief = p.belief();

        Compound tt = task.term();

        this.taskTruth = task.truth();
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

        Term beliefTerm = this.beliefTerm = p.beliefTerm().term();
        Term taskTerm = this.taskTerm = tt;

        //this.cyclic = task.cyclic();
        this.overlap = belief != null && Stamp.overlapping(task, belief);

        this.termSub0Struct = taskTerm.structure();
        this.termSub0op = taskTerm.op().ordinal();
        this.termSub1Struct = beliefTerm.structure();
        this.termSub1op = beliefTerm.op().ordinal();

        this.temporal = temporal(task, belief);


        this.conclusion = c;

        deriver.run(this);
        revert(start);

        this.conclusion = null; //forget a reference to the local field copy but return this instance
        this.evidence = null;
        this.premise = null;

        return c;
    }

    private static boolean temporal(@NotNull Task task, @Nullable Task belief) {
        if (!task.isEternal() || task.dt()!= DTERNAL)
            return true;

        return belief != null && (!belief.isEternal() || belief.dt() != DTERNAL);
    }


    /** calculates Budget used in a derived task,
     *  returns null if invalid / insufficient */
    @Nullable
    public final Budget budget(@Nullable Truth truth, @NotNull Termed derived) {
        float minDur = nar.durMin.floatValue();
        return (truth != null) ?
                    TaskBudgeting.compoundForward(truth, derived, this, minDur) :
                    TaskBudgeting.compoundQuestion(derived, this, minDur);
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

    /** gets the op of the (top-level) pattern being compared
     * @param subterm 0 or 1, indicating task or belief
     * */
    /*public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub0op : termSub1op) == op;
    }*/
    public final int subOp(int i /* 0 or 1 */) {
        return (i == 0 ? termSub0op : termSub1op);
    }

    /** @param subterm 0 or 1, indicating task or belief */
    public final boolean subTermMatch(int subterm, int bits) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Op.hasAll((subterm == 0 ? termSub0Struct : termSub1Struct), bits);
    }

    /** both */
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


    public long[] evidence() {

        if (evidence == null) {
            long[] pte = task.evidence();

            Task b = belief;
            evidence =
                    b != null ?
                            Stamp.zip(pte, b.evidence()) : //double
                            pte //single
            ;
        }

        return evidence;
    }
}


