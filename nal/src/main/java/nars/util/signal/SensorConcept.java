package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.budget.policy.ConceptBudgeting;
import nars.budget.policy.DefaultConceptBudgeting;
import nars.concept.CompoundConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.Sensor;
import nars.util.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** primarily a collector for believing time-changing input signals */
public class SensorConcept extends CompoundConcept implements FloatFunction<Term> {

    @NotNull
    protected final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);


    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, truth) {
            @Override
            public float pri() {
                return SensorConcept.this.pri();
            }
        };
        n.index.set(this);

        this.input = input;

    }

    public float pri() {
        return sensor.pri;
    }

    @NotNull
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
    protected void beliefCapacity(ConceptBudgeting p) {
        DefaultConceptBudgeting.beliefCapacityNonEternal(this, p);
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
    public <S extends SensorConcept> S punc(char c) {
        sensor.punc(c);
        return (S)this;
    }

}
