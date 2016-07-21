package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.budget.policy.ConceptPolicy;
import nars.budget.policy.DefaultConceptPolicy;
import nars.concept.CompoundConcept;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.TemporalBeliefTable;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.Sensor;
import nars.util.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.nal.Tense.ETERNAL;

/**
 * primarily a collector for believing time-changing input signals
 *
 * warning: using action and sensor concepts with a term that can be structurally transformed
 * culd have unpredictable results because their belief management policies
 * may not be consistent with the SensorConcept.  one solution may be to
 * create dummy placeholders for all possible transforms of a sensorconcept term
 * to make them directly reflect the sensor concept as the authority.
 *
 * */
public class SensorConcept extends CompoundConcept<Compound> implements FloatFunction<Term> {

    @NotNull
    protected final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    int beliefCapacity = 16;
    int goalCapacity = 16;

    /** implicit motivation task */
    private Task desire = null;

    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, truth);
        n.on(this);

        this.input = input;

    }

//    public float pri(float v, long now, float prevV, long lastV) {
////        float m;
////        if (prevV != prevV) //NaN
////            m = 1f; //first input
////        else
////            m = (Math.abs(v-prevV)); //decrease by sameness
////        return sensor.pri * m;
//        return sensor.pri;
//    }

//    @NotNull
//    public SensorConcept sensorDT(int newDT) {
//        sensor.dt(newDT);
//        return this;
//    }

    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar, List<Task> displaced) {


        //Filter past or present or eternal feedback (ie. contradicts the sensor's recorded beliefs)
        //but allow future predictions

        if (!validBelief(belief, nar)) {
            //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());

            //TODO delete its non-input parent tasks?
            onConflict(belief);

            return null;
        }

        if (hasBeliefs() && ((DefaultBeliefTable)beliefs()).temporal.isFull()) {
            //try to remove at least one past belief which did not originate from this sensor
            //this should clear space for future predictions
            TemporalBeliefTable tb = ((DefaultBeliefTable) beliefs()).temporal;
            tb.removeIf(t -> !validBelief(t, nar));
        }

        return super.processBelief(belief, nar, displaced);
    }

    /** originating from this sensor, or a future prediction */
    public boolean validBelief(@NotNull Task belief, @NotNull NAR nar) {
        if (belief.log(0) == sensor)
            return true;
        long bocc = belief.occurrence();
        return (bocc!=ETERNAL && bocc > nar.time());
    }


//    @Override
//    protected BeliefTable newGoalTable(int eCap, int tCap) {
//        return new SensorBeliefTable(eCap, tCap);
//    }

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
    final protected void beliefCapacity(ConceptPolicy p) {
        beliefs().capacity(0, beliefCapacity);
        goals().capacity(desire!=null ? 1 : 0, goalCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }
    @Override
    final protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(desire!=null ? 1 : 0,goalCapacity);
    }

    public Task desire(Truth t) {
        if (this.desire==null || !this.desire.truth().equals(t)) {
            if (this.desire != null) {
                this.desire.delete();
            }

            if (t!=null) {
                this.desire = new MutableTask(term(), Symbols.GOAL, t).log("Sensor Goal");
                beliefCapacity(null); //trigger capacity update
                sensor.nar.input(this.desire);
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
