package nars.time;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/** increments time on each frame */
public class FrameTime implements Time {

    private final AtomicLong nextStamp = new AtomicLong(1);

    long t;
    int dt;

    float dur;

    public FrameTime() {
        this(1);
    }

    public FrameTime(float dur) {
        this(1, dur);
    }

    public FrameTime(int dt) {
        this(dt, 1f);
    }

    public FrameTime(int dt, float dur) {
        this.dt = dt;
        this.dur = dur;
    }

    @Override
    public float dur() {
        return dur;
    }

    public FrameTime dur(float d) {
        this.dur = d;
        return this;
    }

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
        t+=dt;
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
    @Override public final long nextStamp() {
        return nextStamp.getAndIncrement();
    }

    /** used to ensure that the next system stamp serial is beyond the range of any input */
    protected final void validate(long s) {
        if (s == Long.MAX_VALUE) //ignore cyclic indicator
            return;
        long nextStamp = this.nextStamp.longValue();
        if (s == Long.MAX_VALUE) //ignore cyclic indicator
            s = 0; //wraparound skipping MAX_VALUE which is reserved for cyclic, but ignore the negative spectrum

        if (nextStamp < s)
            this.nextStamp.set(s+1);
    }

    public final void validate(@NotNull long[] s) {
        //assume that the evidence is sorted, and that the max value is in the last position in addition to 1 or more preceding values
        if (s.length > 1)
            validate(s[s.length-1]);
    }


}
