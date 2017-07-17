package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.SignalTask;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import static nars.time.Tense.ETERNAL;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal extends AtomicReference<SignalTask> {


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


    public Signal(byte punc, FloatSupplier resolution) {
        super(null);
        pri(1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public Task set(@NotNull Compound term, @Nullable Truthed nextTruth, LongSupplier stamper, NAR nar) {


        //int halfDur = Math.max(1, nar.dur() / 2);
        //long next = now + halfDur;

        return updateAndGet((current) -> {

            if (nextTruth == null) {

                return null;             //no signal

            } else {

                long now = nar.time(); //allow the current percept to extend 1/2 duration into the future

                if (current == null ||
                        current.isDeleted() ||
                        !current.truth.equals(nextTruth, resolution.asFloat())
                        //(Param.SIGNAL_LATCH_TIME != Integer.MAX_VALUE && now - current.start() > nar.dur() * Param.SIGNAL_LATCH_TIME)
                    ) {

                    long last = this.lastInputTime;



                    this.lastInputTime = now;


                    //TODO move the task construction out of this critical update section?
                    return task(term, nextTruth.truth(),
                            last, now,
                            stamper.getAsLong());
                    //current.priMax(pri.asFloat() * deltaFactor(current, current.truth()));

                }

                return current; //nothing, keep as-is
            }

        });

    }

    @Nullable
    protected SignalTask task(Compound term, Truth t, long start, long end, long stamp) {


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

}
