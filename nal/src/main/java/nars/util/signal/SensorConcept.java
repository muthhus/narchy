package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.CompoundConcept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.FloatSupplier;
import nars.util.data.Sensor;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Sensor.direct;

/** primarily a collector for believing time-changing input signals */
public class SensorConcept extends CompoundConcept implements FloatFunction<Term> {

    private final Sensor sensor;
    private FloatSupplier input;


    public SensorConcept(@NotNull String compoundTermString, NAR n, FloatSupplier input) throws Narsese.NarseseException {
        this((Compound) $.$(compoundTermString), n, input, direct);
    }

    public SensorConcept(@NotNull Compound term, NAR n, FloatSupplier input)  {
        this(term, n, input, direct);
    }

    public SensorConcept(@NotNull Compound term, NAR n, FloatSupplier input, FloatToFloatFunction toFreq)  {
        super(term, n);

        this.sensor = new Sensor(n, term, this, toFreq);

        this.input = input;
    }

    /**
     * adjust min/max temporal resolution of feedback input
     */
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
        return input.asFloat();
    }
}
