package nars.util.signal;


import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 2/12/17.
 */
public class SignalTask extends ImmutableTask {

    /** because this is an input task, its hash and equality will not depend on this value so it is free to change to represent a growing duration */
    public long slidingEnd;

    public SignalTask(@NotNull Compound t, byte punct, @NotNull Truth truth, long start, long end, long stamp) {
        super(t, punct, truth, start, start, end,
                new long[] { stamp } /* TODO use an implementation which doenst need an array for this */ );
        slidingEnd = end;
    }

    public void setEnd(long now) {
        this.slidingEnd = now;
    }

    @Override
    public long end() {
        return slidingEnd;
    }
}
