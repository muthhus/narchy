package nars.concept;

import nars.NAR;
import nars.Param;
import nars.budget.policy.ConceptPolicy;
import nars.term.Compound;
import nars.util.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * base class for concepts which are more or less programmatically "hard-wired" into
 * external systems and transducers that populate certain segments of its
 * belief tables and other components.
 *
 * this usually requires some specific management of
 * beliefs to prevent influence from derivations that the reasoner may form
 * in contradiction with provided values.
 *
 * warning: using action and sensor concepts with a term that can be structurally transformed
 * culd have unpredictable results because their belief management policies
 * may not be consistent with the SensorConcept.  one solution may be to
 * create dummy placeholders for all possible transforms of a sensorconcept term
 * to make them directly reflect the sensor concept as the authority.
 *
 * */
public abstract class WiredConcept extends CompoundConcept<Compound> implements PermanentConcept {

    @NotNull
    protected final NAR nar;
    int beliefCapacity = Param.DEFAULT_WIRED_CONCEPT_BELIEFS;
    int goalCapacity = Param.DEFAULT_WIRED_CONCEPT_GOALS;

    //@NotNull final private AtomicBoolean pendingRun = new AtomicBoolean(false);

    public interface Prioritizable {
        void pri(FloatSupplier v);
    }

    public WiredConcept(@NotNull Compound term, @NotNull NAR n) {
        super(term, n);
        this.nar = n;
        n.on(this);
    }

    @Override
    public void linkCapacity(int termlinks, int tasklinks) {
        super.linkCapacity(termlinks * termlinkMultiplier(), tasklinks * tasklinkMultiplier());
    }

    protected int termlinkMultiplier() {
        return 1;
    }

    protected int tasklinkMultiplier() {
        return 1;
    }

    //    @Override
//    protected TermContainer buildTemplates(Compound term, NAR nar) {
//        if (term.volume()==2 && term.op() == Op.PROD) {
//            //special case. these are atom-like products of 1 term
//            return Terms.NoSubterms;
//        }
//        return super.buildTemplates(term, nar);
//    }
//
//    @Nullable protected Task filter(@NotNull Task t, @NotNull BeliefTable table, @Nullable BiPredicate<Task,NAR> valid, @NotNull NAR nar, @NotNull List<Task> displaced) {
//
//        if (valid!=null) {
//            if (!table.isEmpty() /*&& ((DefaultBeliefTable)beliefs()).temporal.isFull()*/) {
//                //try to remove at least one past belief which did not originate from this sensor
//                //this should clear space for future predictions
//                TemporalBeliefTable tb = ((DefaultBeliefTable) table).temporal;
//                tb.removeIf(x -> !valid.test(x, nar), displaced);
//            }
//
//            if (!valid.test(t, nar)) {
//
//                //TODO delete its non-input parent tasks?
//                onConflict(t);
//
//                //TaskTable.removeTask(t, "Ignored Speculation", displaced); //will be displaced normally by returning null
//                return null;
//            }
//        }
//
//        return t;
//    }

//    @Override
//    public TruthDelta processBelief(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filterBeliefs(t, nar, displaced);
//        if (t != null) {
//            TruthDelta td = super.processBelief(t, nar, displaced);
//            if (td != null) {
//                //executeLater(t, nar);
//                return td;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public TruthDelta processGoal(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filterGoals(t, nar, displaced);
//        if (t != null) {
//            TruthDelta td = super.processGoal(t, nar, displaced);
//            if (td != null) {
//                //executeLater(t, nar);
//                return td;
//            }
//        }
//        return null;
//    }

//    /** NOTE: if validBelief always returns true, then this can be bypassed by overriding with blank method */
//    public @Nullable Task filterBeliefs(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filter(t, beliefs(), null, nar, displaced);
//        return t;
//    }
//
//    /** NOTE: if validGoal always returns true, then this can be bypassed by overriding with blank method */
//    public @Nullable Task filterGoals(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filter(t, goals(), null, nar, displaced);
//        return t;
//    }


//    @Nullable
//    private Task executeLater(@Nullable Task t, @NotNull NAR nar) {
//        if (t != null && pendingRun.compareAndSet(false, true)) {
//            nar.runLater(this);
//        }
//
//        return t;
//    }

//    /** called at most once per frame */
//    @Override public final void accept(NAR n) {
//        pendingRun.set(false); //this needs to happen first in case update re-triggers a change in this concept
//        update();
//    }
//
//
//    protected void update() {
//        //override in subclasses when used in combination with runLater(t,n)
//    }

//    /** when true, update(nar) will be called before the next frame */
//    protected boolean runLater(@NotNull Task t, @NotNull NAR nar) {
//        return false;
//    }


//    public abstract boolean validBelief(@NotNull Task belief, @NotNull NAR nar);
//    public abstract boolean validGoal(@NotNull Task goal, @NotNull NAR nar);

//    /** called when a conflicting belief has attempted to be processed */
//    protected void onConflict(@NotNull Task belief) {
//        //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());
//
//    }

    @Override
    protected void beliefCapacity(@NotNull ConceptPolicy p, NAR nar) {
        beliefCapacity(0, beliefCapacity, 1, goalCapacity, nar);
    }

//    @Override
//    protected @NotNull BeliefTable newBeliefTable() {
//        return newBeliefTable(nar, 0,beliefCapacity);
//    }
//
//    @Override
//    protected @NotNull BeliefTable newGoalTable() {
//        return newGoalTable(0,goalCapacity);
//    }
}
