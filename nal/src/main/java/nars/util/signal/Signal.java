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
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
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

    public Task set(Term term, @Nullable Truth nextTruth, LongSupplier stamper, long now, int dur, NAR nar) {

        if (!busy.compareAndSet(false, true))
            return last;

        try {
            SignalTask current = this.last;
            @Nullable PreciseTruth tt = nextTruth != null ? nextTruth.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f) : null;


            SignalTask next;
            if (tt == null) {
                //no signal
                next = null;
            } else {


                if (current == null ||
                        current.isDeleted() ||
                        (!current.truth.equals(tt, nar.truthResolution.floatValue()) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - current.start() >= dur * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {

                    //TODO move the task construction out of this critical update section?
                    next = task(term, tt,
                            now, now + lookAheadDurs * dur,
                            stamper.getAsLong());

                } else {

                    next = current; //nothing, keep as-is

                }


            }

            if (current != null && !current.isDeleted()) {

                LongObjectProcedure<SignalTask> stretch = current.stretch;
                if (stretch!=null) {
                    stretch.value(now, current);
                }

            }

            if (current != next) {
                if (current!=null)
                    current.stretch = SignalTask.Frozen;
                this.last = next;
            }

            if (next == null || next.stretch != null)
                return null; //dont re-input the task, just stretch it where it is in the temporal belief table
            else
                return next; //new input; stretch will be assigned on first insert to the belief table (if this happens)

        } finally {
            busy.set(false);
        }
    }

    @Nullable
    protected SignalTask task(Term term, Truth t, long start, long end, long stamp) {
        SignalTask s = new SignalTask(term, punc, t, start, end, stamp);
        s.priMax(pri.asFloat());
        return s;
    }

    @NotNull
    public Signal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }




}
