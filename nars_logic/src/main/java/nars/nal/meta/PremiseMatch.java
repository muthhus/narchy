package nars.nal.meta;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.Op;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.concept.ConceptProcess;
import nars.nal.Deriver;
import nars.nal.meta.op.MatchTerm;
import nars.nal.nal8.Operator;
import nars.nal.op.ImmediateTermTransform;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.Compound;
import nars.term.constraint.MatchConstraint;
import nars.term.transform.FindSubst;
import nars.truth.Truth;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;


/**
 * rule matching context, re-recyclable as thread local
 */
public class PremiseMatch extends FindSubst {

    private final Deriver deriver;

    /** Global Context */
    public Consumer<Task> receiver;

    /** current Premise */
    public ConceptProcess premise;

    //public final VarCachedVersionMap secondary;
    @NotNull
    public final Versioned<Integer> occDelta;
    @NotNull
    public final Versioned<Integer> tDelta;
    @NotNull
    public final Versioned<Truth> truth;
    @NotNull
    public final Versioned<Character> punct;
    @NotNull
    public final Versioned<MatchTerm> pattern;

    @NotNull
    private TaskBeliefPair termPattern = new TaskBeliefPair();
    public boolean cyclic;
    int termutesPerMatch, termutes;

    final Map<Operator, ImmediateTermTransform> transforms =
            Global.newHashMap();
    private float minConfidence = Global.TRUTH_EPSILON;

    public PremiseMatch(Random r, Deriver deriver) {
        super(Op.VAR_PATTERN, r );

        for (Class<? extends ImmediateTermTransform> c : PremiseRule.Operators) {
            addTransform(c);
        }

        this.deriver = deriver;
        //secondary = new VarCachedVersionMap(this);
        occDelta = new Versioned(this);
        tDelta = new Versioned(this);
        truth = new Versioned(this);
        punct = new Versioned(this);
        pattern = new Versioned(this);
    }

    /**
     * Forward logic with CompoundTerm conclusion
     *
     * @param truth The truth value of the conclusion
     * @param content The content of the conclusion
     * @param nal Reference to the memory
     * @return The budget of the conclusion
     */
    public static Budget compoundForward(@NotNull Truth truth, @NotNull Termed content, @NotNull ConceptProcess nal) {
        return BudgetFunctions.compoundForward(new UnitBudget(), truth, content, nal);
    }

    private void addTransform(@NotNull Class<? extends ImmediateTermTransform> c) {
        Operator o = $.operator(c.getSimpleName());
        try {
            transforms.put(o, c.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(c + ": " + e);
        }
    }

    @Override public ImmediateTermTransform getTransform(Operator t) {
        return transforms.get(t);
    }



    public final void match(@NotNull MatchTerm pattern /* callback */, ImmutableMap<Term, MatchConstraint> c) {
        this.pattern.set(pattern); //to notify of matches
        this.constraints = c;
        matchAll(pattern.x, term.get() /* current term */);
        this.constraints = null;
    }

    @Override
    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {
        this.termutes = termutesPerMatch;
        super.matchAll(x, y, finish);
    }

    @Override
    public boolean onMatch() {
        return (termutes-- > 0) ?
            pattern.get().onMatch(this) :
            false;
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
    public final void start(@NotNull ConceptProcess p, Consumer<Task> receiver) {

        premise = p;
        this.receiver = receiver;

        Compound taskTerm = p.getTask().term();

        Termed beliefTerm = p.getBeliefTerm();  //experimental, prefer to use the belief term's Term in case it has more relevant TermMetadata (intermvals)

        this.termutesPerMatch = p.getMaxMatches();

        termPattern.set( taskTerm.term(), beliefTerm.term() );
        term.set( termPattern );

        cyclic = p.isCyclic();

//        //set initial power which will be divided by branch
//        setPower(
//            //LERP the power in min/max range by premise mean priority
//            (int) ((p.getMeanPriority() * (Global.UNIFICATION_POWER - Global.UNIFICATION_POWERmin))
//                    + Global.UNIFICATION_POWERmin)
//        );

        //setPower(branchPower.get()); //HACK is this where it should be assigned?

        p.nar.memory.eventConceptProcess.emit(p);

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
                compoundForward(truth, c, p) :
                BudgetFunctions.compoundBackward(c, p);

//        if (Budget.isDeleted(budget.getPriority())) {
//            throw new RuntimeException("why is " + budget + " deleted");
//        }

        float derThresh = p.memory().derivationDurabilityThreshold.floatValue();
        if (budget.dur() < derThresh)
            return null;


//        if (!!budget.summaryLessThan(p.memory().derivationThreshold.floatValue())) {
////            if (false) {
////                RuleMatch.removeInsufficientBudget(premise, new PreTask(t,
////                        m.punct.get(), truth, budget,
////                        m.occurrenceShift.getIfAbsent(Tense.TIMELESS), premise));
////            }
//            return null;
//        }

        return budget;
    }

    @Nullable
    @Override public final Term apply(Term t) {
        //TODO make a half resolve that only does xy?

//        Term ret = getXY(t);
//        if (ret != null) {
//            ret = getYX(ret);
//        }
//
//        if (ret != null) return ret;
//        return t;
            Term ret = premise.memory().index.apply(this, t);

//            if ((ret != null) /*&& (!yx.isEmpty())*/) {
//                ret = ret.apply(yx, fullMatch);
//            }
            return ret;


    }

    public void setMinConfidence(float minConfidence) {
        this.minConfidence = minConfidence;
    }

    public float getMinConfidence() {
        return minConfidence;
    }
}


