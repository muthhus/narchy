package nars.nar.exe;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
abstract public class Executioner implements Executor {
    @Nullable
    protected NAR nar;

    public void start(NAR nar) {
        if (this.nar == null) {
            this.nar = nar;
        } else {
            throw new RuntimeException("already started");
        }
    }
    public synchronized void stop() {
        if (this.nar != null) {
            this.nar = null;
        } /*else {
            throw new RuntimeException("not already started");
        }*/
    }

    abstract public void next(@NotNull NAR nar);


    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();

    /** true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to bve safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }

    /** whether the execution model is synchronous or asynch */
    public boolean sync() {
        return false;
    }

    /** default impl: */
    public void run(@NotNull Consumer<NAR> r) {
        this.run(()->r.accept(nar));
    }

    abstract public void run(Runnable cmd);

    abstract public void run(@NotNull Task[] t);


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


    /** a scaling factor that executions can use to throttle the workload they will produce in the next cycle */
    public float load() { return 0; }

    @Override
    public final void execute(Runnable command) {
        run(command);
    }

}
