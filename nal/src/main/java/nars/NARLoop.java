package nars;

import jcog.Loop;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 * TODO extract the hft core reservation to a subclass and put that in the app module, along with the hft dependency
 */
public class NARLoop extends Loop {


    @NotNull
    public final NAR nar;



    private long cycles;


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
