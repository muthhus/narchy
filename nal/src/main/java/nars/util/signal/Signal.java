package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.table.DefaultBeliefTable;
import nars.task.SignalTask;
import nars.term.Term;
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

    long lastUpdated = ETERNAL;


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
            long last = this.lastUpdated;
            if (last == ETERNAL) {
                last = now;
            }

            if (nextTruth == null) {

                this.lastUpdated = ETERNAL;
                return null;             //no signal

            } else {

                SignalTask next ;

                if (current == null ||
                        current.isDeleted() ||
                        (!current.truth.equals(nextTruth, resolution.asFloat()) ||
                                (Param.SIGNAL_LATCH_TIME != Integer.MAX_VALUE && now - current.start() > nar.dur() * Param.SIGNAL_LATCH_TIME)
                        )) {


                    if (current != null) {
                        current.setEnd(now);
                    }

                    //TODO move the task construction out of this critical update section?
                    next = task(term, nextTruth.truth(),
                            now, now,
                            stamper.getAsLong(), dt);

                    //System.out.println(current + " " + next );
                } else {

                    current.setEnd(now); //stretch existing
                    next = current;

                }

                this.lastUpdated = now;

                next.priMax(pri.asFloat()); // * deltaFactor(prev, current.truth()));

                if (next.stretchKey != null) {
                    next.stretchKey = ((DefaultBeliefTable) ((BaseConcept) nar.concept(next)).table(next.punc)).temporal.stretch(next);
                }

                return next; //nothing, keep as-is
            }


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
