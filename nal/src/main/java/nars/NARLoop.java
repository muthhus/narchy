package nars;

import jcog.Util;
import jcog.exe.Loop;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 * TODO extract the hft core reservation to a subclass and put that in the app module, along with the hft dependency
 */
public class NARLoop extends Loop {

    @NotNull
    public final NAR nar;

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
    protected int nextPeriodMS() {
        int delayMS = super.nextPeriodMS();

//        float load = nar.exe.load();
//
//        float SAFETY_LOAD = 0.5f;
//        if (load > SAFETY_LOAD) {
//            float MAX_LOAD_BACKOFF_MS = 5f;
//            int stallMS = 1+Math.round(1 + ((load - SAFETY_LOAD)) / (1 - SAFETY_LOAD) * MAX_LOAD_BACKOFF_MS);
//            //delayMS += stallMS;
//            Util.pause(stallMS);
//        }

        return delayMS;
    }

    @Override
    public final boolean next() {
        nar.cycle();
        return true;
    }




}
