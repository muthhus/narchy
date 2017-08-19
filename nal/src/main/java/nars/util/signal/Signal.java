package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.table.DefaultBeliefTable;
import nars.task.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
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

    private static final int lookAheadDurs = 1;


    public Signal(byte punc, FloatSupplier resolution) {
        super(null);
        pri(1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public Task set(@NotNull Term term, @Nullable Truthed nextTruth, LongSupplier stamper, NAR nar) {
        return set(term, nextTruth, stamper, nar, 0);
    }

    public Task set(@NotNull Term term, @Nullable Truthed nextTruth, LongSupplier stamper, NAR nar, int dt) {


        //int halfDur = Math.max(1, nar.dur() / 2);
        //long next = now + halfDur;
        return updateAndGet((current) -> {

            long now = nar.time();
            float p = pri.asFloat();

            if (current != null) {
                current.setEnd(now);


                current.priMax(p);


                if (current.stretchKey != null) {
                    Concept c = nar.concept(current);
                    if (c!=null) {
                        DefaultBeliefTable bt = (DefaultBeliefTable) ((BaseConcept) c).table(current.punc);
                        current.stretchKey = bt.temporal.stretch(current);
                    }
                }
            }


            SignalTask next;

            if (nextTruth == null) {

                next = null;             //no signal

            } else {

                if (current == null ||
                        current.isDeleted() ||
                        (!current.truth.equals(nextTruth, nar.truthResolution.floatValue()) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - current.start() >= nar.dur() * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {


                    //TODO move the task construction out of this critical update section?
                    next = task(term, nextTruth.truth(),
                            now, now + lookAheadDurs * nar.dur(),
                            stamper.getAsLong(), dt);

                    next.stretchKey = this; //temporary placeholder, for at least signaling the stretch to itself
                    next.setEnd(now);

                    //System.out.println(current + " " + next );
                } else {

                    next = current;

                }

            }


            if (next != null) {
                next.priMax(p);
            }


            return next; //nothing, keep as-is

        });

    }

    @Nullable
    protected SignalTask task(Term term, Truth t, long start, long end, long stamp, int dt) {


        SignalTask s = new SignalTask(term, punc, t, start + dt, end + dt, stamp);

        return s;
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
