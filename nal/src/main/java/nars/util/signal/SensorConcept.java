package nars.util.signal;

import nars.*;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.task.DerivedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.Sensor;
import nars.util.math.FloatSupplier;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.nal.Tense.ETERNAL;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier {

    @NotNull
    public final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    /** implicit motivation task */
    private Task desire;

    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, truth) {
            @Override
            public void input(Task t) {
                SensorConcept.this.input(t);
            }
        };

        this.input = input;

        final float gain = 0.5f; //1f;
        final float base = 0.1f;
        this.sensor.pri(()->Math.min(1f, base + gain * n.conceptPriority(term)));
    }

    protected void input(Task t) {
        nar.inputLater(t);
    }


    /** originating from this sensor, or a future prediction */
    @Override
    public boolean validBelief(@NotNull Task t, @NotNull NAR nar) {
        //return onlyDerivationsIfFuture(t, nar);
        return true;
    }
    @Override
    public boolean validGoal(@NotNull Task goal, @NotNull NAR nar) {
        //return onlyDerivationsIfFuture(t, nar);
        return true;
    }


    public static boolean onlyDerivationsIfFuture(@NotNull Task belief, @NotNull NAR nar) {
        if (!(belief instanceof DerivedTask))
            return true;

        long bocc = belief.occurrence();
        return (bocc!=ETERNAL && bocc > nar.time());
    }



    @Override
    final protected void beliefCapacity(ConceptPolicy p, long now, List<Task> removed) {
        beliefCapacity(0, beliefCapacity, desire!=null ? 1 : 0, goalCapacity, now, removed);
    }

    @Override
    final protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(desire!=null ? 1 : 0,goalCapacity);
    }


    /** async timing: only commits when value has changed significantly, and as often as necessary */
    @NotNull
    public SensorConcept async() {
        timing(0, 0);
        return this;
    }

    /** commits every N cycles only */
    @NotNull
    public SensorConcept every(int minCycles) {
        timing(minCycles, minCycles);
        return this;
    }

    
    /**
     * adjust min/max temporal resolution of feedback input
     * ex:
     *          min=0, max=2 : update every 2 cycles, or immediately if changed
     *          max=2, min=0 : update no sooner than 2 cycles
     *          max=2, min=4 : update no sooner than 2 cycles, and no later than 4
     */
    @NotNull
    public SensorConcept timing(int minCycles, int maxCycles) {
        sensor.minTimeBetweenUpdates(minCycles);
        sensor.maxTimeBetweenUpdates(maxCycles);
        return this;
    }

    public Task desire(@Nullable Truth t) {
        if (this.desire==null || !this.desire.truth().equals(t)) {
            if (this.desire != null) {
                this.desire.delete(nar);
            }

            if (t!=null) {
                this.desire = new MutableTask(term(), Symbols.GOAL, t).budget(1f, 1f).log("Sensor Goal");
                //policy(policy(), nar.time()); //trigger capacity update
                sensor.nar.inputLater(this.desire);
            }
        }
        return this.desire;
    }


    public void setInput(FloatSupplier input) {
        this.input = input;
    }

    public final FloatSupplier getInput() {
        return input;
    }

    @Override
    public final float floatValueOf(Term anObject) {
        return this.current = input.asFloat();
    }

    @NotNull
    public SensorConcept resolution(float v) {
        sensor.resolution(v);
        return this;
    }

    @NotNull public SensorConcept pri(FloatSupplier v) {
        sensor.pri(v);
        return this;
    }

    @Deprecated @NotNull public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }

    @NotNull
    public <S extends SensorConcept> S punc(char c) {
        sensor.punc(c);
        return (S)this;
    }

    @Override
    public float asFloat() {
        return current;
    }
}
