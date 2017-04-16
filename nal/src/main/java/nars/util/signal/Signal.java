package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.task.SignalTask;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal {

    public FloatSupplier pri;

    boolean inputIfSame;
//    int maxTimeBetweenUpdates;
//    int minTimeBetweenUpdates;

    final byte punc;

    public long lastInputTime;
    @Nullable
    public SignalTask current;

    public Signal(byte punc) {
        pri(1);
        this.punc = punc;
    }


    public Task set(Compound term, Truth nextTruth, long stamp, NAR nar) {

        long now = nar.time();
        if (current != null) {
            current.setEnd(now);
        }

        this.lastInputTime = now;

        if (nextTruth == null) {
            this.current = null;
            return null;
        }


        if (current!=null) {
            if (current.truth.equals(nextTruth, nar.truthResolution.floatValue())) {
                current.budget(nar); //rebudget
                return current;
            }
        }


        SignalTask t = task(term, nextTruth,
                now, now,
                this.current, stamp, nar);
        if (t == null) {
            this.current = null;
            return null;
        } else {
            t.budget(nar);
            return (this.current = t);
        }
    }

    @Nullable
    protected SignalTask task(Compound term, Truth t, long start, long end, Task previous, long stamp, NAR nar) {


        SignalTask s = new SignalTask(term, punc, t, start, end, stamp);
        s.setPriority(pri.asFloat() * deltaFactor(previous, t));

        //float changeFactor = prevV==prevV ? Math.abs(v - prevV) : 1f /* if prevV == NaN */;
        //s.budgetByTruth( Math.max(Param.BUDGET_EPSILON*2, changeFactor * pri.asFloat())  /*(v, now, prevF, lastInput)*/);
        //.log(this);
        return s;
    }

    /**
     * factor to reduce priority for similar truth value
     * TODO revise
     */
    protected float deltaFactor(@Nullable Truthed a, Truth b) {
        return 1f;

//        if (a == null)
//            return 1f;
//
//        return Math.abs(a.freq() - b.freq());
    }

    @NotNull
    public Signal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }

    @NotNull
    public Signal pri(float p) {
        pri(() -> p);
        return this;
    }

    public Task get() {
        return current;
    }
}
