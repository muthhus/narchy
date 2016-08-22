package nars.util.signal;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.policy.ConceptPolicy;
import nars.concept.CompoundConcept;
import nars.concept.TruthDelta;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.TemporalBeliefTable;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

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
public abstract class WiredConcept extends CompoundConcept<Compound> implements Runnable {

    @NotNull
    protected final NAR nar;
    int beliefCapacity = Param.DEFAULT_WIRED_CONCEPT_BELIEFS;
    int goalCapacity = Param.DEFAULT_WIRED_CONCEPT_GOALS;

    @NotNull final private AtomicBoolean pendingRun = new AtomicBoolean(false);

    public WiredConcept(@NotNull Compound term, @NotNull NAR n) {
        super(term, n);
        n.on(this);
        this.nar = n;
    }

    @Nullable protected Task filter(@NotNull Task t, @NotNull BeliefTable table, @NotNull BiPredicate<Task,NAR> valid, @NotNull NAR nar, @NotNull List<Task> displaced) {

        if (!table.isEmpty() /*&& ((DefaultBeliefTable)beliefs()).temporal.isFull()*/) {
            //try to remove at least one past belief which did not originate from this sensor
            //this should clear space for future predictions
            TemporalBeliefTable tb = ((DefaultBeliefTable) table).temporal;
            tb.removeIf(x -> !valid.test(x, nar), displaced);
        }

        if (!valid.test(t, nar)) {

            //TODO delete its non-input parent tasks?
            onConflict(t);

            //TaskTable.removeTask(t, "Ignored Speculation", displaced); //will be displaced normally by returning null
            return null;
        }

        return t;
    }

    @Override
    public TruthDelta processBelief(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
        t = filterBeliefs(t, nar, displaced);
        if (t != null) {
            TruthDelta td = super.processBelief(t, nar, displaced);
            if (td != null) {
                executeLater(t, nar);
                return td;
            }
        }
        return null;
    }

    @Override
    public TruthDelta processGoal(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
        t = filterGoals(t, nar, displaced);
        if (t != null) {
            TruthDelta td = super.processGoal(t, nar, displaced);
            if (td != null) {
                executeLater(t, nar);
                return td;
            }
        }
        return null;
    }

    /** NOTE: if validBelief always returns true, then this can be bypassed by overriding with blank method */
    public @Nullable Task filterBeliefs(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
        t = filter(t, beliefs(), this::validBelief, nar, displaced);
        return t;
    }

    /** NOTE: if validGoal always returns true, then this can be bypassed by overriding with blank method */
    public @Nullable Task filterGoals(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
        t = filter(t, goals(), this::validGoal, nar, displaced);
        return t;
    }


    @Nullable
    private Task executeLater(@Nullable Task t, @NotNull NAR nar) {
        if (t != null && runLater(t, nar) && pendingRun.compareAndSet(false, true)) {
            nar.runLater(this);
        }

        return t;
    }

    @Override
    public final void run() {
        pendingRun.set(false); //this needs to happen first in case update re-triggers a change in this concept
        update();
    }


    protected void update() {
        //override in subclasses when used in combination with runLater(t,n)
    }

    /** when true, update(nar) will be called before the next frame */
    protected boolean runLater(@NotNull Task t, @NotNull NAR nar) {
        return false;
    }


    public abstract boolean validBelief(@NotNull Task belief, @NotNull NAR nar);
    public abstract boolean validGoal(@NotNull Task goal, @NotNull NAR nar);



    /** called when a conflicting belief has attempted to be processed */
    protected void onConflict(@NotNull Task belief) {
        //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());

    }

    @Override
    protected void beliefCapacity(@NotNull ConceptPolicy p, long now, List<Task> removed) {
        beliefCapacity(0, beliefCapacity, 0, goalCapacity, now, removed);
    }

    @Override
    protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }

    @Override
    protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(0,goalCapacity);
    }
}
