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


/**
 * Generates temporal tasks in reaction to the change in a scalar numeric value
 *
 * when NAR wants to update the signal, it will call Function.apply. it can return
 * an update Task, or null if no change
 */
public class ScalarSignal implements Function<NAR, Task>, DoubleSupplier {


    /**
     * resolution of the output freq value
     */
    float resolution = Param.DEFAULT_SENSOR_RESOLUTION;



    @NotNull
    private final Compound term;
    private final FloatFunction<Term> value;
    @NotNull
    private final FloatToObjectFunction<Truth> truthFloatFunction;

    public FloatSupplier pri;

    public float currentValue = Float.NaN;

    boolean inputIfSame;
    int maxTimeBetweenUpdates;
    int minTimeBetweenUpdates;

    byte punc = '.';

    public long lastInputTime;

    public final static FloatToFloatFunction direct = n -> n;

    @Nullable public SignalTask current;


    public ScalarSignal(@NotNull NAR n, @NotNull Compound t, FloatFunction<Term> value, @Nullable FloatToObjectFunction<Truth> truthFloatFunction) {

        this.term = t.term();
        this.value = value;
        this.truthFloatFunction = truthFloatFunction == null ? (v)->null : truthFloatFunction;


        pri(1);

        this.lastInputTime = n.time() - 1;

        this.currentValue = Float.NaN;
    }

    @NotNull
    public ScalarSignal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }
    @NotNull
    public ScalarSignal pri(float p) {
        pri(()->p);
        return this;
    }

    @NotNull
    public ScalarSignal punc(byte c) {
        this.punc = c;
        return this;
    }

    public byte punc() { return punc; }

//    /** clears timing information so it thinks it will need to input on next attempt */
//    public void ready() {
//        this.lastInputTime = nar.time() - minTimeBetweenUpdates;
//    }

    @Override
    public Task apply(@NotNull NAR nar) {

        long now = nar.time();

        //update previous task: extend its end time to current time
        if (current!=null && current.isDeleted())  {
            currentValue = Float.NaN; //force re-input
        }

        int timeSinceLastInput = (int) (now - lastInputTime);


        float next = value.floatValueOf(term);
        if (next!=next) {
            invalidate();
            this.current = null;

            return null; //all
        }// ow the value function to prevent input by returning NaN


        int maxT = this.maxTimeBetweenUpdates;
        boolean limitsMaxTime = maxT > 0;
        int minT = this.minTimeBetweenUpdates;
        boolean limitsMinTime = minT > 0;

        boolean tooSoon = (limitsMinTime && (timeSinceLastInput < minT));
        boolean lateEnough = (limitsMaxTime && (timeSinceLastInput >= maxT));
        boolean different = (currentValue != currentValue /* NaN */) || !Util.equals(next, currentValue, resolution);

        if ((inputIfSame || different || lateEnough) && (!tooSoon)) {

            SignalTask t = task(next, currentValue,
                    now - Math.round(nar.dur()), now,
                    this.current, nar);
            if (t!=null) {
                //Task prevStart = this.current;

//                if (prevStart!=null && t.occurrence() - prevStart.occurrence() > latchResolution) {
//                    //input a cloned version of the previous task as an intermediate task, squarewave approximation
//                    prevEnd = newInputTask(prevStart.truth(), now, now-1);
//                } else {
//                    prevEnd = null; //dont generate an intermediate task
//                }

                this.current = t;
                this.lastInputTime = now;
                this.currentValue = next;
                return t;
            }
        }

        //nothing new was input, continue previous task if exists
        if (current!=null && !current.isDeleted()) {
            current.setEnd(now);
        }

        return null;
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

    public void invalidate() {
        this.currentValue = Float.NaN;
    }


//    protected float conf(float v) {
//        return confFactor;
//    }
//    protected float freq(float v) {
//        return v;
//    }

    @Nullable
    protected SignalTask task(float next, float prevV, long start, long end, Task previous, NAR nar) {

        Truth t = truthFloatFunction.valueOf(next);
        if (t == null)
            return null;


        SignalTask s = new SignalTask(term(), punc, t, start, end, nar.time.nextStamp());
        s.budget(pri.asFloat() * deltaFactor(previous, t), nar);

        //float changeFactor = prevV==prevV ? Math.abs(v - prevV) : 1f /* if prevV == NaN */;
        //s.budgetByTruth( Math.max(Param.BUDGET_EPSILON*2, changeFactor * pri.asFloat())  /*(v, now, prevF, lastInput)*/);
        //.log(this);
        return s;
    }

    /** factor to reduce priority for similar truth value
     *  TODO revise
     * */
    protected float deltaFactor(@Nullable Truthed a, Truth b) {
        return 1f;

//        if (a == null)
//            return 1f;
//
//        return Math.abs(a.freq() - b.freq());
    }

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


//    /** sets minimum time between updates, even if nothing changed. zero to disable this */
//    @NotNull
//    public Sensor maxTimeBetweenUpdates(int dt) {
//        this.maxTimeBetweenUpdates = dt;
//        return this;
//    }

    @NotNull
    public ScalarSignal minTimeBetweenUpdates(int dt) {
        this.minTimeBetweenUpdates = dt;
        return this;
    }

    @NotNull
    public ScalarSignal maxTimeBetweenUpdates(int dt) {
        this.maxTimeBetweenUpdates = dt;
        return this;
    }
    //        public void on() {
//
//        }
//        public void off() {
//
//        }
}
