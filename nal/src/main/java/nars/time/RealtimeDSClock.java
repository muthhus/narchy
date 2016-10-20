package nars.time;

/** decisecond (0.1) accuracy */
public class RealtimeDSClock extends RealtimeClock {

    private final long start;

    public RealtimeDSClock() {
        this(false);
    }


    public RealtimeDSClock(boolean relativeToStart) {
        this.start = relativeToStart ? getRealTime() : 0L;
    }

    @Override
    protected long getRealTime() {
        return System.currentTimeMillis()/100 - start;
    }

    @Override
    protected float unitsToSeconds(long l) {
        return (l / 10.0f);
    }

}
