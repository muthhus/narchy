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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal {


    FloatSupplier pri;


    /**
     * quantizes the input value
     * quantization of the output value, ie. truth epsilon is separately applied according to the NAR's parameter
     */
    public final FloatSupplier resolution;

    public final AtomicBoolean busy = new AtomicBoolean(false);

//    boolean inputIfSame;
//    int maxTimeBetweenUpdates;
//    int minTimeBetweenUpdates;

    final byte punc;

    private static final int lookAheadDurs = 0;
    private SignalTask last;


    public Signal(byte punc, FloatSupplier resolution) {
        pri(() -> 1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public SignalTask get() {
        return last;
    }

    public Task set(@NotNull Term term, @Nullable Truth nextTruth, LongSupplier stamper, long now, int dur, NAR nar) {

        if (!busy.compareAndSet(false, true))
            return last;

        try {
            SignalTask current = this.last;
            @Nullable PreciseTruth tt = nextTruth != null ? nextTruth.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f) : null;


            SignalTask toInput;
            if (tt == null) {
                //no signal
                toInput = null;
            } else {


                if (current == null ||
                        current.isDeleted() ||
                        (!current.truth.equals(tt, nar.truthResolution.floatValue()) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - current.start() >= dur * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {

                    //TODO move the task construction out of this critical update section?
                    toInput = task(term, tt,
                            now, now + lookAheadDurs * dur,
                            stamper.getAsLong());

                } else {

                    toInput = current; //nothing, keep as-is

                }


            }

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
        } finally {
            busy.set(false);
        }
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
