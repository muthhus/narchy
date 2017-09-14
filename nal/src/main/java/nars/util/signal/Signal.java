package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.SignalTask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal extends AtomicReference<SignalTask> {


    FloatSupplier pri;


    /**
     * quantizes the output truth, ie. truth epsilon,
     * (does not process the value of the input signal)
     */
    public final FloatSupplier resolution;

//    boolean inputIfSame;
//    int maxTimeBetweenUpdates;
//    int minTimeBetweenUpdates;

    final byte punc;

    private static final int lookAheadDurs = 0;
    private SignalTask last = null;


    public Signal(byte punc, FloatSupplier resolution) {
        super(null);
        pri(() -> 1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public Task set(@NotNull Term term, @Nullable Truth nextTruth, LongSupplier stamper, long now, int dur, NAR nar) {

        @Nullable PreciseTruth tt = nextTruth != null ? nextTruth.ditherFreqConf(resolution.asFloat(), nar.confMin.floatValue(), 1f) : null;


        SignalTask toInput;
        if (tt == null) {
            //no signal
            set(null);
            toInput = null;
        } else {
            toInput = updateAndGet((current) -> {


                if (current == null ||
                        current.isDeleted() ||
                        (!current.truth.equals(tt, nar.truthResolution.floatValue()) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - current.start() >= dur * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {

                    //TODO move the task construction out of this critical update section?
                    return task(term, tt,
                            now, now + lookAheadDurs * dur,
                            stamper.getAsLong());

                } else {

                    return current; //nothing, keep as-is

                }


            });
        }

        SignalTask last = this.last;
        if (last != null && !last.isDeleted()) {
            last.setEnd(now);
            BooleanProcedure stretch = this.last.stretchKey;
            if (last != toInput) {
                stretch.value(false);
                last.stretchKey = null; //end previous
            } else {
                stretch.value(true);
            }
        }

        this.last = toInput;

        if (toInput == null || toInput.stretchKey != Pending)
            return null;
        else
            return toInput;
    }

    @Nullable
    protected SignalTask task(Term term, Truth t, long start, long end, long stamp) {


        SignalTask s = new SignalTask(term, punc, t, start, end, stamp);
        s.priMax(pri.asFloat());
        s.stretchKey = Pending;
        return s;
    }

    @NotNull
    public Signal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }


    public static final BooleanProcedure Pending = (b) -> {
    };

}
