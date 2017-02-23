package nars.util.exe;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.meter.event.PeriodMeter;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *  measures statistics of time elapsed between cycles for various procedures in the system
 *  analogous to brainwave frequencies
 */
public class InstrumentedExecutor extends Executioner {

    private final Executioner exe;

    final PeriodMeter cycleTime, taskInput;

    final Map<Class, PeriodMeter> meters = new ConcurrentHashMap<>();

    private final int windowCycles;

    private boolean trace = true;

    public InstrumentedExecutor(Executioner delegate, int windowCycles) {
        this.exe = delegate;
        this.windowCycles = windowCycles;
        this.cycleTime = meter(NAR.class);
        this.taskInput = meter(Task.class);
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
        exe.start(nar);
    }

    @Override
    public synchronized void stop() {
        exe.stop();
        super.stop();
    }

    public PeriodMeter meter(Class cc) {
        return meters.computeIfAbsent(cc, c -> new PeriodMeter(c.getName(), windowCycles));
    }

    @Override
    public void cycle(@NotNull NAR nar) {
        synchronized (cycleTime) {
            cycleTime.hit();
        }

        exe.cycle(nar);

        if (trace) {
            System.out.println(summary());
        }
    }

    public InstrumentedExecutor setTrace(boolean trace) {
        this.trace = trace;
        return this;
    }

    public String summary() {
        return Joiner.on(' ').join(meters.values());
//        return new StringBuilder().append(
//            Texts.n4(
//                this.cycleTime.mean()/1E6
//            )).append("ms ")
//        .toString();
    }

    @Override
    public int concurrency() {
        return exe.concurrency();
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        run(()->exe.run(r), meter(r.getClass()));
    }

    @Override
    public void run(Runnable cmd) {
        run(()->exe.run(cmd), meter(cmd.getClass()));
    }

    static void run(Runnable cmd, PeriodMeter p) {
        long start = System.nanoTime();
        cmd.run();
        long end = System.nanoTime();

        synchronized (p) {
            p.hitNano(end - start);
        }
    }

    @Override
    public void run(@NotNull Task[] t) {
        run(()->exe.run(t), taskInput);
    }

    @Override
    public float load() {
        return exe.load();
    }

}
