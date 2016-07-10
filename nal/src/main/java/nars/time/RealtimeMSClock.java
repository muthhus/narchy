package nars.time;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** millisecond accuracy */
public class RealtimeMSClock extends RealtimeClock {



    @Override
    protected final long getRealTime() {
        return System.currentTimeMillis();
    }

    @Override
    protected final float unitsToSeconds(long l) {
        return (l / 1000.0f);
    }


}
