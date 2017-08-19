package nars.task;


import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/12/17.
 */
public class SignalTask extends NALTask {

    /** because this is an input task, its hash and equality will not depend on this value so it is free to change to represent a growing duration */
    public long slidingEnd = ETERNAL;

    public Object stretchKey;

    public SignalTask(@NotNull Term t, byte punct, @NotNull Truth truth, long start, long end, long stamp) {
        super(t, punct, truth, start, start, end,
                new long[] { stamp } /* TODO use an implementation which doenst need an array for this */ );
    }

    @Override
    public boolean eternalizable() {
        return false;
    }


    @Override public long end() {
        if (this.slidingEnd == ETERNAL)
            return start();
        else
            return slidingEnd;
    }

    public void setEnd(long now) {
        this.slidingEnd = now;
    }


}
