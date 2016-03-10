package nars.nal.meta;

import com.gs.collections.api.map.ImmutableMap;
import nars.*;
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
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.transform.subst.FindSubst;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class PremiseEval extends FindSubst {

    private final Deriver deriver;


    /** the current premise being evaluated in this context TODO make private again */
    public transient ConceptProcess premise = null;

    @NotNull
    public final Versioned<Truth> truth;
    @NotNull
    public final Versioned<Character> punct;

    @NotNull
    @Deprecated public final Versioned<MatchTerm> pattern;

    @NotNull
    private final TaskBeliefPair termPattern = new TaskBeliefPair();

    //    /**
    //     * current "y"-term being matched against
    //     */
    public Term term = null;


    int termutesPerMatch, termutes;

    public final Map<Atomic, ImmediateTermTransform> transforms =
            Global.newHashMap();
    private float minConfidence = Global.TRUTH_EPSILON;


    /** cached value */
    private TermIndex index;

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
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable MatchTerm callback, ImmutableMap<Term, MatchConstraint> constraints) {

        int t = now();

        boolean finished = callback != null;

        if (finished)
            this.pattern.set(callback); //to notify of matches

        this.constraints.set( constraints );

        matchAll(x, y, finished);

        if (finished) {
            revert(t);
        } //else: allows the set constraints to continue

    }

    @Override
    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.termutes = termutesPerMatch;
        super.matchAll(x, y, finish);
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
    public final void start(@NotNull ConceptProcess p) {

        this.premise = p;

        this.index = premise.nar().index;

        Task task = p.task();
        Compound taskTerm = task.term();
        punct.set(task.punc());

        Term beliefTerm = p.beliefTerm().term();  //experimental, prefer to use the belief term's Term in case it has more relevant TermMetadata (intermvals)

        this.termutesPerMatch = p.getMaxMatches();

        termPattern.set( taskTerm, beliefTerm );
        this.term = termPattern;
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
    public final Budget getBudget(@Nullable Truth truth, @NotNull Termed c) {

        ConceptProcess p = this.premise;

        Budget budget = truth != null ?
                BudgetFunctions.compoundForward(truth, c, p) :
                BudgetFunctions.compoundBackward(c, p);

//        if (Budget.isDeleted(budget.getPriority())) {
//            throw new RuntimeException("why is " + budget + " Deleted");
//        }

        return budget!=null && BudgetFunctions.valid(budget, p.nar()) ? budget : null;


//        if (!!budget.summaryLessThan(p.memory().derivationThreshold.floatValue())) {
////            if (false) {
////                RuleMatch.removeInsufficientBudget(premise, new PreTask(t,
////                        m.punct.get(), truth, budget,
////                        m.occurrenceShift.getIfAbsent(Tense.TIMELESS), premise));
////            }
//            return null;
//        }
    }

    @Nullable
    @Override public final Term resolve(@NotNull Term t) {
        //TODO make a half resolve that only does xy?
        return index.apply(this, t);
    }

    public void setMinConfidence(float minConfidence) {
        this.minConfidence = minConfidence;
    }

    public float getMinConfidence() {
        return minConfidence;
    }

}


