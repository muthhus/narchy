package nars.task;


import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/12/17.
 */
public class SignalTask extends NALTask {


    /** because this is an input task, its hash and equality will not depend on this value so it is free to change to represent a growing duration */
    public long slidingEnd;

    /** true passed to update the task with a new futur endpoint,
     * false parameter signals the stretching end which the table may need to do any finalization */
    public LongObjectProcedure<SignalTask> stretch = null;

    public SignalTask(@NotNull Term t, byte punct, @NotNull Truth truth, long start, long end, long stamp) {
        super(t, punct, truth, start, start, end,
                new long[] { stamp } /* TODO use an implementation which doenst need an array for this */ );
        slidingEnd = end;
    }

    @Override
    public float eternalizable() {
        return 0;
        //return 0.1f;
    }


    @Override public long end() {
        return slidingEnd;
    }

    /** should be called only from the stretch procedure */
    public boolean setEnd(long e) {
        if (e <= start() || e <= slidingEnd)
            return false;

        this.slidingEnd = e;
        return true;
    }


    public static final LongObjectProcedure<SignalTask> Frozen = (l,w) -> { /* nothing */ };

}
