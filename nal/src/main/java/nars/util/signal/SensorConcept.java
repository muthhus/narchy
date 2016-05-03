package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.CompoundConcept;
import nars.term.Compound;
import nars.term.Term;
import nars.util.FloatSupplier;
import nars.util.data.Sensor;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Sensor.direct;

/** primarily a collector for believing time-changing input signals */
public class SensorConcept extends CompoundConcept implements FloatFunction<Term> {

    @NotNull
    private final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public SensorConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MutableFloat v) throws Narsese.NarseseException {
        this(compoundTermString, n, v::floatValue);
    }

    public SensorConcept(@NotNull String compoundTermString, @NotNull NAR n, FloatSupplier input) throws Narsese.NarseseException {
        this($.$(compoundTermString), n, input, direct);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input)  {
        this(term, n, input, direct);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToFloatFunction toFreq)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, toFreq) {
//            @Override
//            protected float freq(float v) {
//                return SensorConcept.this.freq(v);
//            }
//            @Override
//            protected float conf(float v) {
//                return SensorConcept.this.conf(v);
//            }

        };
        n.on(this);

        this.input = input;

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
        return eternalOrTemporal ? 0 : maxBeliefs; //no eternal
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
