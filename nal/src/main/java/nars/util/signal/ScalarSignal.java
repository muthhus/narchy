package nars.util.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.SignalTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static nars.Op.BELIEF;


/**
 * Generates temporal tasks in reaction to the change in a scalar numeric value
 *
 * when NAR wants to update the signal, it will call Function.apply. it can return
 * an update Task, or null if no change
 */
public class ScalarSignal extends Signal implements Function<NAR, Task>, DoubleSupplier {


    private final Compound term;
    /**
     * resolution of the output freq value
     */
    float resolution = Param.DEFAULT_SENSOR_RESOLUTION;


    private final FloatFunction<Term> value;
    @NotNull
    private final FloatToObjectFunction<Truth> truthFloatFunction;


    public float currentValue = Float.NaN;


    public final static FloatToFloatFunction direct = n -> n;



    public ScalarSignal(@NotNull NAR n, @NotNull Compound t, FloatFunction<Term> value, @Nullable FloatToObjectFunction<Truth> truthFloatFunction) {
        super(BELIEF);

        this.term = t;
        this.value = value;
        this.truthFloatFunction = truthFloatFunction == null ? (v)->null : truthFloatFunction;



        this.lastInputTime = n.time() - 1;

        this.currentValue = Float.NaN;
    }


    public byte punc() { return punc; }

//    /** clears timing information so it thinks it will need to input on next attempt */
//    public void ready() {
//        this.lastInputTime = nar.time() - minTimeBetweenUpdates;
//    }

    @Override
    public Task apply(@NotNull NAR nar) {

        //long now = nar.time();

        //update previous task: extend its end time to current time
//        if (current!=null && current.isDeleted())  {
//            currentValue = Float.NaN; //force re-input
//        }

        //int timeSinceLastInput = (int) (now - lastInputTime);


        float next = value.floatValueOf(term);
        return set(term,
                (next == next) ? truthFloatFunction.valueOf(this.currentValue = next) : null,
                nar);

//        int maxT = this.maxTimeBetweenUpdates;
//        boolean limitsMaxTime = maxT > 0;
//        int minT = this.minTimeBetweenUpdates;
//        boolean limitsMinTime = minT > 0;

//        boolean tooSoon = (limitsMinTime && (timeSinceLastInput < minT));
//        boolean lateEnough = (limitsMaxTime && (timeSinceLastInput >= maxT));
//        boolean different = (currentValue != currentValue /* NaN */) || !Util.equals(next, currentValue, resolution);

        //if ((inputIfSame || different || lateEnough) && (!tooSoon)) {



        //}

//        //nothing new was input, continue previous task if exists
//        if (current!=null && !current.isDeleted()) {
//            current.setEnd(now);
//        }

//        return null;
    }



    @Nullable
    public Task next() {
        return current;
    }

    @NotNull
    public ScalarSignal resolution(float r) {
        this.resolution = r;
        return this;
    }




//    protected float conf(float v) {
//        return confFactor;
//    }
//    protected float freq(float v) {
//        return v;
//    }



    /** provides an immediate truth assessment with the last known signal value */
    @Nullable public final Truth truth() {
        Task t = this.current;
        return t!=null ? t.truth() : null;
    }

    //    public float pri(float v, long now, float prevV, long lastV) {
//        return pri;
//    }

    @NotNull
    public Compound term() {
        return term;
    }


    @Override
    public double getAsDouble() {
        return currentValue;
    }


}
