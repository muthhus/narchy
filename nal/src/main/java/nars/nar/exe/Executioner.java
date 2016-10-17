package nars.nar.exe;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * Created by me on 8/16/16.
 */
abstract public class Executioner implements Executor {
    abstract public void start(@NotNull NAR nar);

    abstract public void synchronize();

    abstract public void inputLater(@NotNull Task[] t);

    abstract public void next(@NotNull NAR nar);

    abstract public boolean executeMaybe(Runnable r);

    abstract public void stop();

    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();

    /** true if this executioner executes procedures concurrently */
    public final boolean concurrent() {
        return concurrency() > 1;
    }

    /** a postive or negative value indicating the percentage difference from the
     * currently configured CPU usage target to the actual measured CPU usage.
     *
     * tasks can use this value to determine runtime parameters.
     *
     * if this value is positive then more work is allowed, if negative, less work is allowed.
     *
     * @return
     */
    public float throttle() { return 0; }
}
