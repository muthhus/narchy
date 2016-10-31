package nars.time;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealtimeClock implements Clock {


    private static final float DEFAULT_DURATION_SECONDS = 0.5f;
    private final int unitsPerSecod;
    long t, t0 = -1;
    private long start;

    long seed = Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000;
    final AtomicLong nextStamp = new AtomicLong(1);

    float duration;


    protected RealtimeClock(int unitsPerSecond, boolean relativeToStart) {
        super();
        this.unitsPerSecod = unitsPerSecond;
        this.start = relativeToStart ? getRealTime() : 0L;

        setDuration(DEFAULT_DURATION_SECONDS);

    }

    public final RealtimeClock setDuration(float seconds) {
        duration = secondsToUnits(seconds);
        return this;
    }

    public final float secondsToUnits(float s) {
        return s / unitsToSeconds(1);
    }

    @Override
    public long nextStamp() {
        return seed | nextStamp.getAndIncrement();
    }

    @Override
    public void clear() {
        tick();

        if (start!=0)
            start = getRealTime();

        t = t0 = (getRealTime()-start);
    }


    @Override
    public final void tick() {
        long now = (getRealTime()-start);

        t0 = t;

        t = now;
    }


    @Override
    public final long time() {
        return t;
    }

    @Override
    public long elapsed() {
        return t0 - t;
    }

    protected abstract long getRealTime();

    float secondsSinceStart() {
        return unitsToSeconds(t - start);
    }

    protected final float unitsToSeconds(long l) {
        return l / ((float)unitsPerSecod);
    }

    @Override
    public final float duration() {
        return duration;
    }

    @NotNull
    @Override
    public String toString() {
        return secondsSinceStart() + "s";
    }

    /** decisecond (0.1) accuracy */
    public static class DS extends RealtimeClock {


        public DS() {
            this(false);
        }

        public DS(boolean relativeToStart) {
            super(10, relativeToStart);
        }

        @Override
        protected long getRealTime() {
            return System.currentTimeMillis() / 100;
        }

    }

    /** centisecond (0.01) accuracy */
    public static class CS extends RealtimeClock {


        public CS() {
            this(false);
        }

        public CS(boolean relativeToStart) {
            super(100, relativeToStart);
        }

        @Override
        protected long getRealTime() {
            return System.currentTimeMillis() / 10;
        }

    }

    /** millisecond accuracy */
    public static class MS extends RealtimeClock {


        public MS() {
            this(false);
        }


        public MS(boolean relativeToStart) {
            super(1000, relativeToStart);
        }

        @Override
        protected long getRealTime() {
            return System.currentTimeMillis();
        }

    }

    /** nanosecond accuracy */
    public static class NS extends RealtimeClock {


        protected NS(boolean relativeToStart) {
            super(1000*1000*1000, relativeToStart);
        }

        @Override
        protected long getRealTime() {
            return System.nanoTime();
        }

    }
}


//package nars.clock;
//
///**
// * hard realtime does not cache the value and will always update when time()
// * is called
// */
//public class HardRealtimeClock extends RealtimeClock {
//
//    private final boolean msOrNano;
//
//    public HardRealtimeClock(boolean msOrNano) {
//        super(false);
//        this.msOrNano = msOrNano;
//    }
//
//    /** default: ms resolution */
//    public HardRealtimeClock() {
//        this(true);
//    }
//
//
//    @Override
//    protected long getRealTime() {
//        if (msOrNano) {
//            return System.currentTimeMillis();
//        }
//        else {
//            return System.nanoTime();
//        }
//    }
//
//    @Override
//    protected float unitsToSeconds(long l) {
//        if (msOrNano) {
//            return (l / 1000f);
//        }
//        else {
//            return (l / 1e9f);
//        }
//    }
//
//    @Override
//    public long time() {
//        return getRealTime();
//    }
//
//}
