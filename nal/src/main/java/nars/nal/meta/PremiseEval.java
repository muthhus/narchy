package nars.nal.meta;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.budget.Budget;
import nars.budget.policy.TaskBudgeting;
import nars.index.TermIndex;
import nars.nal.ConceptProcess;
import nars.nal.Deriver;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.op.ImmediateTermTransform;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.subst.FindSubst;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static nars.Op.VAR_PATTERN;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class PremiseEval extends FindSubst {

    private final Deriver deriver;


    /** the current premise being evaluated in this context TODO make private again */
    public transient ConceptProcess premise;

    @NotNull
    public final Versioned<Truth> truth;
    @NotNull
    public final Versioned<Character> punct;


    /** current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee */
    @NotNull
    @Deprecated public ProcTerm forEachMatch;

    public final Map<Atomic, ImmediateTermTransform> transforms = new HashMap();

    /** run parameters */
    int premiseMatchesMax, termutes;



    /** cached value */
    public float confMin = Global.TRUTH_EPSILON;
    private int termSub0op, termSub1op;
    private int termSub0Struct, termSub1Struct;
    public boolean cyclic, overlap;
    @Nullable
    public Truth taskTruth, beliefTruth;
    @Nullable
    public Compound taskTerm;
    public Term beliefTerm;
    public NAR nar;
    public Task task;
    public char taskPunct;


    /** initializes with the default static term index/builder */
    public PremiseEval(Random r, Deriver deriver) {
        this($.terms, r, deriver);
    }

    public PremiseEval(TermIndex index, Random r, Deriver deriver) {
        super(index, VAR_PATTERN, r );

        for (Class<? extends ImmediateTermTransform> c : PremiseRule.Operators) {
            addTransform(c);
        }

        this.deriver = deriver;
        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        truth = new Versioned(versioning);
        punct = new Versioned(versioning);
    }

    private void addTransform(@NotNull Class<? extends ImmediateTermTransform> c) {
        try {
            transforms.put((Atomic) index.the($.operator(c.getSimpleName())).term(), c.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(c + ": " + e);
        }
    }

    @Override public final ImmediateTermTransform getTransform(Atomic t) {
        return transforms.get(t);
    }

    /** only one thread should be in here at a time */
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable ProcTerm eachMatch, @Nullable MatchConstraint constraints) {

        int t = now();

        boolean finish = (eachMatch != null);

        this.forEachMatch = eachMatch; //to notify of matches

        if (constraints!=null)
            this.constraints.set( constraints );

        matchAll(x, y, finish);

        this.forEachMatch = null;

        if (finish) {
            versioning.revert(t);
        } //else: allows the set constraints to continue


    }

    @Override
    public boolean matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.termutes = premiseMatchesMax;
        return super.matchAll(x, y, finish);
    }

    @Override
    public boolean onMatch() {
        if (termutes-- > 0) {
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
                (truth.get()!=null ? (", truth:" + truth) : "")+
                //(!secondary.isEmpty() ? (", secondary:" + secondary) : "")+
                //(occurrenceShift.get()!=null ? (", occShift:" + occurrenceShift) : "")+
                //(branchPower.get()!=null ? (", derived:" + branchPower) : "")+
                '}';

    }


    /**
     * set the next premise
     */
    public final void run(@NotNull ConceptProcess p) {
        this.nar = p.nar;

        Task task = p.task();
        if (task == null)
            return;

        this.task = task;
        this.taskPunct = task.punc();

        this.premiseMatchesMax = p.matchesMax();

        this.premise = p;


        this.punct.set(task.punc());

        Task belief = p.belief();

        Compound tt = task.term();
        Termed bb = p.beliefTerm(); //includes when belief==null, the termlink

        this.taskTruth = task.truth();
        this.beliefTruth = belief != null ? belief.truth() : null;

        if (taskTruth == null || !taskTruth.isNegative()) {
            this.taskTerm = tt;
        } else {
            this.taskTruth = this.taskTruth.negated();
            this.taskTerm = $.neg(tt);
        }

        //if (beliefTruth == null || !beliefTruth.isNegative()) {
            this.beliefTerm = bb.term();
//        } else {
//            this.beliefTruth = this.beliefTruth.negated();
//            this.beliefTerm = $.neg(bb);
//        }





        this.cyclic = p.cyclic();
        this.overlap = p.overlap();

        this.termSub0Struct = taskTerm.structure();
        this.termSub0op = taskTerm.op().ordinal();
        this.termSub1Struct = beliefTerm.structure();
        this.termSub1op = beliefTerm.op().ordinal();

        //term.set( termPattern );

//        //set initial power which will be divided by branch
//        setPower(
//            //LERP the power in min/max range by premise mean priority
//            (int) ((p.getMeanPriority() * (Global.UNIFICATION_POWER - Global.UNIFICATION_POWERmin))
//                    + Global.UNIFICATION_POWERmin)
//        );

        //setPower(branchPower.get()); //HACK is this where it should be assigned?

        //p.nar.eventConceptProcess.emit(p);


        deriver.run(this);

        clear();

    }

//    public final void occurrenceAdd(long durationsDelta) {
//        //TODO move to post
//        int oc = occurrenceShift.getIfAbsent(Tense.TIMELESS);
//        if (oc == Tense.TIMELESS)
//            oc = 0;
//        oc += durationsDelta * premise.getTask().duration();
//        occurrenceShift.set((int)oc);
//    }

    /** calculates Budget used in a derived task,
     *  returns null if invalid / insufficient */
    @Nullable
    public final Budget budget(@Nullable Truth truth, @NotNull Termed derived) {
        ConceptProcess p = this.premise;
        Budget budget = truth != null ?
                    TaskBudgeting.compoundForward(truth, derived, p) :
                    TaskBudgeting.compoundQuestion(derived, p);
        return (budget.dur() >= p.nar().derivationDurabilityThreshold.floatValue()) ?
                budget : null;
    }


    /** specific minimum confidence function for advanced filtering heuristics TODO */
    public final float confidenceMin(Term pattern, char punc) {

//        //EXAMPLE TEMPORARY HACK
//        Op o = pattern.op();
//        if (o!=VAR_PATTERN) {
//            int str = pattern.structure();
//
//            if ((Op.hasAny(str, Op.EQUIV) || (o == Op.INHERIT)))
//                return minConfidence * 3;
//        }

        return confMin;
    }

    /** gets the op of the (top-level) pattern being compared
     * @param subterm 0 or 1, indicating task or belief
     * */
    public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub0op : termSub1op) == op;
    }
    public final int subOp(int i /* 0 or 1 */) {
        return (i == 0 ? termSub0op : termSub1op);
    }

    /** @param subterm 0 or 1, indicating task or belief */
    public final boolean subTermMatch(int subterm, int bits) {
        int existingStructure = (subterm == 0 ? termSub0Struct : termSub1Struct);
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Termlike.hasAll(existingStructure, bits);
    }

    /** both */
    public final boolean subTermsMatch(int bits) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Termlike.hasAll(termSub0Struct, bits) &&
               Termlike.hasAll(termSub1Struct, bits);
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


}


