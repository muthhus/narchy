package nars.util.exe;

import com.google.common.base.Joiner;
import jcog.meter.event.PeriodMeter;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *  measures statistics of time elapsed between cycles for various procedures in the system
 *  analogous to brainwave frequencies
 *
 *  TODO use this correctly with MultithreadExecutor, this is currently missing most of what it does
 */
public class InstrumentedExecutor extends Executioner {

    private final Executioner exe;

    final PeriodMeter cycleTime;

    final Map<Class, PeriodMeter> meters = new ConcurrentHashMap<>();
    private final int collectionPeriod;


    private boolean trace = true;

    public InstrumentedExecutor(Executioner delegate) {
        this(delegate, 1);
    }

    public InstrumentedExecutor(Executioner delegate, int collectionPeriod) {
        this.exe = delegate;
        this.collectionPeriod = collectionPeriod;
        this.cycleTime = meter(NAR.class);
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
        return meters.computeIfAbsent(cc, c -> new PeriodMeter(c.getName(), -1));
    }

    @Override
    public void cycle(@NotNull NAR nar) {

        exe.cycle(nar);

        synchronized (cycleTime) {
            cycleTime.hit();
        }

        if (cycleTime.getN() % collectionPeriod == collectionPeriod-1) {
            if (trace) {
                System.out.println(summary());
            }

            meters.values().forEach(x -> {
                synchronized (x) {
                    x.clear();
                }
            });
        }

    }

    public InstrumentedExecutor setTrace(boolean trace) {
        this.trace = trace;
        return this;
    }

    public String summary() {
        return Joiner.on('\n').join(
                meters.values().stream().map(x -> PrimitiveTuples.pair(x.sum(), x.toStringMicro())).collect(
                        Collectors2.toSortedListBy( x -> -x.getOne() )).stream().map(DoubleObjectPair::getTwo).iterator() );

    }

    @Override
    public int concurrency() {
        return exe.concurrency();
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        exe.run( ()-> measure(()->r.accept(nar), meter(r.getClass())));
    }

    @Override
    public void run(Runnable cmd) {
        exe.run( ()-> measure(cmd, meter(cmd.getClass())));
    }


    static void measure(Runnable cmd, PeriodMeter p) {
        long start = System.nanoTime();
        cmd.run();
        long end = System.nanoTime();

        synchronized (p) {
            p.hitNano(end - start);
        }
    }

    @Override
    public float load() {
        return exe.load();
    }

}
