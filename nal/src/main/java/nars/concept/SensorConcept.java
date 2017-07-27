package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.LongSupplier;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier, Function<NAR, Task> {

    public final ScalarSignal sensor;
    private FloatSupplier signal;
    protected float currentValue = Float.NaN;

    static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);


    public SensorConcept(@NotNull Term c, @NotNull NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth) {
        super(c,
                new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null, n);

        this.sensor = new ScalarSignal(n, c, this, truth, resolution) {
            @Override
            protected LongSupplier stamp(Truth currentBelief, @NotNull NAR nar) {
                return SensorConcept.this.nextStamp(nar);
            }
        };
        ((SensorBeliefTable)beliefs).sensor = sensor;

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(@NotNull NAR nar) {
        return nar.time::nextStamp;
    }


    public void setSignal(FloatSupplier signal) {
        this.signal = signal;
    }


    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = signal.asFloat();
    }

    @NotNull
    public final void pri(FloatSupplier v) {
        sensor.pri(v);
    }

    @NotNull
    public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }


    @Override
    public float asFloat() {
        return currentValue;
    }


    /**
     * should only be called if autoupdate() is false
     */
    @Nullable
    @Override
    public final Task apply(NAR nar) {
        return sensor.apply(nar);
    }

    public SensorConcept resolution(float r) {
        resolution.setValue(r);
        return this;
    }

}
