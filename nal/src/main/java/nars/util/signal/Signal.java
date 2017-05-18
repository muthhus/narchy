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

import java.util.function.LongSupplier;

import static nars.time.Tense.ETERNAL;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal {
    final int MAX_PERCEPT_DURATIONS = 4;

    public FloatSupplier pri;


    /**
     * quantizes the output truth, ie. truth epsilon,
     * (does not process the value of the input signal)
     */
    public final FloatSupplier resolution;

//    boolean inputIfSame;
//    int maxTimeBetweenUpdates;
//    int minTimeBetweenUpdates;

    final byte punc;

    public long lastInputTime = ETERNAL;

    @Nullable
    public SignalTask current;

    public Signal(byte punc, FloatSupplier resolution) {
        pri(1);
        this.punc = punc;
        this.resolution = resolution;
    }


    public Task set(Compound term, Truth nextTruth, LongSupplier stamper, NAR nar) {


        long now = nar.time(); //allow the current percept to extend 1/2 duration into the future


        //int halfDur = Math.max(1, nar.dur() / 2);
        //long next = now + halfDur;

        long last = this.lastInputTime;
        if (last == ETERNAL)
            last = now;
        this.lastInputTime = now;

        if (this.current != null) {
            this.current.setEnd((now+last)/2);
            if (now - current.start() >= (nar.dur() * MAX_PERCEPT_DURATIONS)) {
                this.current = null;
            }
        }

        Task previous = current;


        //no signal
        if (nextTruth == null) {
            this.current = null;
            return null;
        } else {


            SignalTask t;
            //merge existing signal



            if (current != null && current.truth.equals(nextTruth, resolution.asFloat())) {
//                if (residualBudgetFactor > 0) {
//                    current.budget(residualBudgetFactor, nar); //rebudget
//                    current.setEnd(next);
//                    return current;
//                }
//                return null;
                //nothing, keep as-is
                t = current;
            } else {


                t = task(term, nextTruth,
                    (now+last)/2, now,
                        previous, stamper.getAsLong(), nar);
            }

            t.priMax(pri.asFloat() * deltaFactor(previous, t.truth()));


            return (this.current = t);
        }


    }

    @Nullable
    protected SignalTask task(Compound term, Truth t, long start, long end, Task previous, long stamp, NAR nar) {


        SignalTask s = new SignalTask(term, punc, t, start, end, stamp);

        return s;
    }

    /**
     * factor to reduce priority for similar truth value
     * TODO revise
     */
    protected float deltaFactor(@Nullable Truthed a, Truth b) {
        //return 1f;

        if (a == null)
            return 1f;
        return 0.5f + ( (a==b) ? 0 : 0.5f * Math.abs(a.freq() - b.freq()));
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
