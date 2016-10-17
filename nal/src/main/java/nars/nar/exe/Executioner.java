package nars.nar.exe;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
abstract public class Executioner implements Executor {
    protected NAR nar;



    public synchronized void start(NAR nar) {
        if (this.nar == null) {
            this.nar = nar;
        } else {
            throw new RuntimeException("already started");
        }
    }
    public synchronized void stop() {
        if (this.nar != null) {
            this.nar = null;
        } else {
            throw new RuntimeException("not already started");
        }
    }
    abstract public void inputLater(@NotNull Task[] t);

    abstract public void next(@NotNull NAR nar);

    abstract public boolean executeMaybe(Runnable r);



    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();

    /** true if this executioner executes procedures concurrently */
    public final boolean concurrent() {
        return concurrency() > 1;
    }

    /** default impl: */
    public void execute(@NotNull Consumer<NAR> r) {
        execute(()->r.accept(nar));
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
