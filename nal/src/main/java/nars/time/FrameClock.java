package nars.time;

import org.jetbrains.annotations.NotNull;

/** increments time on each frame */
public class FrameClock implements Clock {


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

}
