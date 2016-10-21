package nars.time;

/** millisecond accuracy */
public class RealtimeMSClock extends RealtimeClock {


    private final long start;

    public RealtimeMSClock() {
        this(false);
    }


    public RealtimeMSClock(boolean relativeToStart) {
        this.start = relativeToStart ? System.currentTimeMillis() : 0L;
    }

    @Override
    protected long getRealTime() {
        return (System.currentTimeMillis() - start);
    }


    @Override
    protected final float unitsToSeconds(long l) {
        return (l / 1000.0f);
    }


}
