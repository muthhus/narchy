package nars.time;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/** increments time on each frame */
public class FrameClock implements Clock {

    private final AtomicLong nextStamp = new AtomicLong(1);

    long t;

    @Override
    public void clear() {
        t = 0;
    }

    @Override
    public final long time() {
        return t;
    }


    @Override
    public final void tick() {
        t++;
    }

    @Override
    public long elapsed() {
        return 1;
    }

    @NotNull
    @Override
    public String toString() {
        return Long.toString(t);
    }

    /**
     * produces a new stamp serial #, used to uniquely identify inputs
     */
    @Override public final long newStampSerial() {
        return nextStamp.getAndIncrement();
    }

    /** used to ensure that the next system stamp serial is beyond the range of any input */
    protected final void ensureStampSerialGreater(long s) {
        if (s == Long.MAX_VALUE) //ignore cyclic indicator
            return;
        long nextStamp = this.nextStamp.longValue();
        if (nextStamp < s)
            this.nextStamp.set(s+1);
    }

    public final void ensureStampSerialGreater(@NotNull long[] s) {
        //assume that the evidence is sorted, and that the max value is in the last position
        ensureStampSerialGreater(s[s.length-1]);
    }

}
