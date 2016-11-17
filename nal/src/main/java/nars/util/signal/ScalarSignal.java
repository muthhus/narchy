package nars.util.signal;

import nars.*;
import nars.task.AbstractTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.math.FloatSupplier;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Generates temporal tasks in reaction to the change in a scalar numeric value
 */
public class ScalarSignal implements Consumer<NAR>, DoubleSupplier {


    private final Consumer<Task> target;
    /**
     * resolution of the output freq value
     */
    float resolution = 0.01f;

    /** in frames time */
    final static long latchResolution = 1;

    @NotNull
    private final Term term;
    private final FloatFunction<Term> value;
    @NotNull
    private final FloatToObjectFunction<Truth> truthFloatFunction;

    public FloatSupplier pri;
    public float dur;

    private float prevF = Float.NaN;

    boolean inputIfSame;
    int maxTimeBetweenUpdates;
    int minTimeBetweenUpdates;

    char punc = '.';

    public long lastInputTime;

    public final static FloatToFloatFunction direct = n -> n;
    @Nullable
    public SignalTask current;


    public ScalarSignal(@NotNull NAR n, @NotNull Termed t, FloatFunction<Term> value, FloatToObjectFunction<Truth> truthFloatFunction, Consumer<Task> target) {
        this(n, t, value, truthFloatFunction, n.priorityDefault(Symbols.BELIEF), n.durabilityDefault(Symbols.BELIEF), target);
    }

    public ScalarSignal(@NotNull NAR n, @NotNull Termed t, FloatFunction<Term> value, @Nullable FloatToObjectFunction<Truth> truthFloatFunction, float pri, float dur, Consumer<Task> target) {

        this.term = t.term();
        this.value = value;
        this.truthFloatFunction = truthFloatFunction == null ? (v)->null : truthFloatFunction;


        pri(pri);
        this.dur = dur;
        this.lastInputTime = n.time() - 1;

        this.prevF = Float.NaN;
        this.target = target;
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
    public ScalarSignal punc(char c) {
        this.punc = c;
        return this;
    }

    public char punc() { return punc; }

//    /** clears timing information so it thinks it will need to input on next attempt */
//    public void ready() {
//        this.lastInputTime = nar.time() - minTimeBetweenUpdates;
//    }

    @Override
    public void accept(@NotNull NAR nar) {

        long now = nar.time();

        //update previous task: extend its end time to current time
        if (current!=null) {
            if (!current.isDeleted()) {
                current.setEnd(now);
            } else {
                prevF = Float.NaN; //force re-input
            }
        }

        int timeSinceLastInput = (int) (now - lastInputTime);


        float next = value.floatValueOf(term);
        if (next!=next) {
            this.prevF = next;
            return; //all
        }// ow the value function to prevent input by returning NaN

        float f = Util.round(next, resolution);

        int maxT = this.maxTimeBetweenUpdates;
        boolean limitsMaxTime = maxT > 0;
        int minT = this.minTimeBetweenUpdates;
        boolean limitsMinTime = minT > 0;

        boolean tooSoon = (limitsMinTime && (timeSinceLastInput < minT));
        boolean lateEnough = (limitsMaxTime && (timeSinceLastInput >= maxT));
        boolean different = (prevF!=prevF /* NaN */) || !Util.equals(f, prevF, Param.TRUTH_EPSILON);

        if ((inputIfSame || different || lateEnough) && (!tooSoon)) {

            SignalTask t = task(f, prevF, now, this.current);
            if (t!=null) {
                //Task prevStart = this.current;

//                if (prevStart!=null && t.occurrence() - prevStart.occurrence() > latchResolution) {
//                    //input a cloned version of the previous task as an intermediate task, squarewave approximation
//                    prevEnd = newInputTask(prevStart.truth(), now, now-1);
//                } else {
//                    prevEnd = null; //dont generate an intermediate task
//                }

                target.accept(this.current = t);
                this.lastInputTime = now;
                this.prevF = f;
            }
        }

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

    static class SignalTask extends MutableTask {

        long end;

        public SignalTask(@NotNull Termed<Compound> t, char punct, @Nullable Truth truth, long start)  {
            super(t, punct, truth);
            time(start, start);

            end = start;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        @Override
        public long end() {
            return end;
        }
    }

    @Nullable
    protected SignalTask task(float v, float prevV, long now, Task previous) {
        float changeFactor = prevV==prevV ? Math.abs(v - prevV) : 1f /* if prevV == NaN */;

        Truth t = truthFloatFunction.valueOf(v);
        if (t == null)
            return null;

        SignalTask s = new SignalTask(term(), punc, t, now);
        s.budgetByTruth( Math.max(Param.BUDGET_EPSILON*2, changeFactor * pri.asFloat())  /*(v, now, prevF, lastInput)*/, dur);
        //.log(this);
        return s;
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
    public Termed<Compound> term() {
        return term;
    }


    @Override
    public double getAsDouble() {
        return prevF;
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
