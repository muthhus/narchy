package nars;

import nars.util.Loop;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 * TODO extract the hft core reservation to a subclass and put that in the app module, along with the hft dependency
 * <p>
 * mostly replaced by Executioner's
 */
public class NARLoop extends Loop {


    @NotNull
    public final NAR nar;


    private static final int framesPerLoop = 1;
    private long cycles = 0;


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
        nar.run(framesPerLoop);
        cycles++;
        return true;
    }


    public long cycleCount() {
        return cycles;
    }


    public void prePeriodMS(int ms) {
        periodMS.set(ms);
    }
}
