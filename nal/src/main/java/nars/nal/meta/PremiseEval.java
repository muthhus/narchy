package nars.nal.meta;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.Op;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.ConceptProcess;
import nars.nal.Deriver;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.meta.op.MatchTerm;
import nars.nal.op.ImmediateTermTransform;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.transform.subst.FindSubst;
import nars.term.transform.subst.Subst;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;

import static nars.budget.BudgetFunctions.*;
import static nars.budget.BudgetFunctions.compoundForward;


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

    @NotNull
    @Deprecated public final Versioned<MatchTerm> pattern;

    @NotNull
    public final TaskBeliefPair term = new TaskBeliefPair();

    //    /**
    //     * current "y"-term being matched against
    //     */
    //public Term term;


    int termutesPerMatch, termutes;

    public final Map<Atomic, ImmediateTermTransform> transforms =
            Global.newHashMap();
    private float minConfidence = Global.TRUTH_EPSILON;


    /** cached value */
    private int termSub1Op, termSub2Op;
    private int termSub1Struct, termSub2Struct;

    public PremiseEval(Random r, Deriver deriver) {
        super(Op.VAR_PATTERN, r );

        for (Class<? extends ImmediateTermTransform> c : PremiseRule.Operators) {
            addTransform(c);
        }

        this.deriver = deriver;
        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        truth = new Versioned(this);
        punct = new Versioned(this);
        pattern = new Versioned(this);
    }

    private void addTransform(@NotNull Class<? extends ImmediateTermTransform> c) {
        try {
            transforms.put($.operator(c.getSimpleName()), c.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(c + ": " + e);
        }
    }

    @Override public final ImmediateTermTransform getTransform(Atomic t) {
        return transforms.get(t);
    }

    /** only one thread should be in here at a time */
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable MatchTerm callback, MatchConstraint constraints) {

        int t = now();

        boolean finished = callback != null;

        //if (finished)
            this.pattern.set(callback); //to notify of matches

        if (constraints!=null)
            this.constraints.set( constraints );

        matchAll(x, y, finished);

        if (finished) {
            revert(t);
        } //else: allows the set constraints to continue

    }

    @Override
    public boolean matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.termutes = termutesPerMatch;
        return super.matchAll(x, y, finish);
    }

    @Override
    public boolean onMatch() {
        return (termutes-- > 0) && pattern.get().onMatch(this);
    }




    @NotNull
    @Override
    public String toString() {
        return "RuleMatch:{" +
                "premise:" + premise +
                ", subst:" + super.toString() +
                (pattern.get()!=null ? (", derived:" + pattern) : "")+
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

        this.premise = p;

        this.index = premise.nar().index;

        Task task = p.task();
        Compound taskTerm = task.term();
        punct.set(task.punc());

        Term beliefTerm = p.beliefTerm().term();  //experimental, prefer to use the belief term's Term in case it has more relevant TermMetadata (intermvals)

        this.termutesPerMatch = p.getMaxMatches();

        term.set( taskTerm, beliefTerm );
        this.termSub1Struct = taskTerm.structure();
        this.termSub1Op = taskTerm.op().ordinal();
        this.termSub2Struct = beliefTerm.structure();
        this.termSub2Op = beliefTerm.op().ordinal();

        //term.set( termPattern );

//        //set initial power which will be divided by branch
//        setPower(
//            //LERP the power in min/max range by premise mean priority
//            (int) ((p.getMeanPriority() * (Global.UNIFICATION_POWER - Global.UNIFICATION_POWERmin))
//                    + Global.UNIFICATION_POWERmin)
//        );

        //setPower(branchPower.get()); //HACK is this where it should be assigned?

        p.nar.eventConceptProcess.emit(p);


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
    public final Budget budget(@Nullable Truth truth, @NotNull Term derived) {
        ConceptProcess p = this.premise;
        return valid(truth != null ?
                    compoundForward(truth, derived, p) :
                    compoundBackward(derived, p)
                , p.nar());
    }



    public final void setMinConfidence(float minConfidence) {
        this.minConfidence = minConfidence;
    }

    public final float getMinConfidence() {
        return minConfidence;
    }

    /** gets the op of the (top-level) pattern being compared
     * @param subterm 0 or 1, indicating task or belief
     * */
    public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub1Op : termSub2Op) == op;
    }

    /** @param subterm 0 or 1, indicating task or belief */
    public final boolean subTermMatch(int subterm, int bits) {
        return !Termlike.impossibleStructureMatch(
                    (subterm == 0 ? termSub1Struct : termSub2Struct), bits);
    }

    /** both */
    public final boolean subTermsMatch(int bits) {
        return !Termlike.impossibleStructureMatch(termSub1Struct, bits) &&
               !Termlike.impossibleStructureMatch(termSub2Struct, bits);
    }

    /** returns whether the put operation was successful */
    public final boolean putXY(Term k, Versioned<Term> vv) {
        Term v = vv.get();
        if (v != null) {
            return putXY(k, v);
        }
        return false;
    }

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

    public void replaceAllXY(Subst m) {
        m.forEach(this::replaceXY);
    }


}


