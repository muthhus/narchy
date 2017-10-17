package nars;

import jcog.data.FloatParam;
import jcog.exe.Loop;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public class NARLoop extends Loop {

    public final NAR nar;

    public final FloatParam throttle = new FloatParam(1f, 0f, 1f);

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
    }

    /**
     * @param n
     * @param initialPeriod
     */
    public NARLoop(@NotNull NAR n, int initialPeriod) {
        this(n);
        setPeriodMS(initialPeriod);
    }

    @Override
    public final boolean next() {
        nar.cycle();
        return true;
    }

}
