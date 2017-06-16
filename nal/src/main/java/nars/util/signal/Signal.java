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

    final int MAX_PERCEPT_DURATIONS =
            Integer.MAX_VALUE;
            //16;

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


    public Task set(@NotNull Compound term, @Nullable Truthed nextTruth, NAR nar) {
        return set(term, nextTruth, nar.time::nextStamp, nar);
    }

    public Task set(@NotNull Compound term, @Nullable Truthed nextTruth, LongSupplier stamper, NAR nar) {


        long now = nar.time(); //allow the current percept to extend 1/2 duration into the future


        //int halfDur = Math.max(1, nar.dur() / 2);
        //long next = now + halfDur;


        long last = this.lastInputTime;
        if (last == ETERNAL)
            last = now;

        Task previous = current;

        if (this.current != null) {
            this.current.setEnd(now);
            if (current.isDeleted() || now - current.start() >= (nar.dur() * MAX_PERCEPT_DURATIONS)) {
                if (nextTruth == null)
                    nextTruth = current.truth;
                this.current = null;
            }
        }

            this.lastInputTime = now;


        //no signal
        if (nextTruth == null) {
            return (this.current = null);
        } else {





            if (current != null && current.truth.equals(nextTruth, resolution.asFloat())) {
//                if (residualBudgetFactor > 0) {
//                    current.budget(residualBudgetFactor, nar); //rebudget
//                    current.setEnd(next);
//                    return current;
//                }
//                return null;
                //nothing, keep as-is
            } else {

                SignalTask t;

                t = task(term, nextTruth.truth(),
                    last, now,
                        previous, stamper.getAsLong(), nar);

                if (t!=null)
                    current = t;
            }

            current.priMax(pri.asFloat() * deltaFactor(previous, current.truth()));
            return current;
        }


    }

    @Nullable
    protected SignalTask task(Compound term, Truth t, long start, long end, Task previous, long stamp, NAR nar) {


        SignalTask s = new SignalTask(term, punc, t, start, end, stamp);

        return s;
    }

//    /**
//     * factor to reduce priority for similar truth value
//     * TODO revise
//     */
    protected float deltaFactor(@Nullable Truthed a, Truth b) {
        //return 1f;

        //if (a == null)
            return 1f;
//        else {
//            float diff = Math.abs(a.freq() - b.freq());
            //return 0.5f + ( (a==b) ? 0 : 0.5f * diff);
            //1-((1-x)^2)
            //1-((1-x)^4)
//            return 1f - (Util.sqr(Util.sqr(1-diff)));
//        }
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
