package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.FloatSupplier;
import nars.util.data.Sensor;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/** primarily a collector for believing time-changing input signals */
public class SensorConcept extends CompoundConcept implements FloatFunction<Term> {

    @NotNull
    private final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);


    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this((Compound)$.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, truth);
        n.on(this);

        this.input = input;

    }

    public SensorConcept sensorDT(int newDT) {
        sensor.dt(newDT);
        return this;
    }

    @Override
    public @Nullable
    Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        //if (belief.evidence().length > 1) {

        //Filter feedback that contradicts the sensor's provided beliefs
        if (belief!=sensor.next()) {
            //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());

            //TODO delete its non-input parent tasks?
            onConflict(belief);

            return null;
        }

        return super.processBelief(belief, nar);
    }

    /** called when a conflicting belief has attempted to be processed */
    protected void onConflict(@NotNull Task belief) {

    }

    //    float freq(float v) {
//        return v;
//    }
//    float conf(float v) {
//        return 0.90f;
//    }
    
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

    @Override
    protected int capacity(int maxBeliefs, boolean beliefOrGoal, boolean eternalOrTemporal) {
        boolean forBeliefs = sensor.punc() == Symbols.BELIEF;
        if (forBeliefs == beliefOrGoal)
            return eternalOrTemporal ? 0 : maxBeliefs; //no eternal for tasks of the type managed by this sensor
        else
            return super.capacity(maxBeliefs, beliefOrGoal, eternalOrTemporal);
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

    @NotNull
    public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }

    public final float get() {
        return current;
    }

    @NotNull
    public SensorConcept punc(char c) {
        sensor.punc(c);
        return this;
    }

}
