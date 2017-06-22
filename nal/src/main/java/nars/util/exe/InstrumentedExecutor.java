package nars.util.exe;

import com.google.common.base.Joiner;
import jcog.math.MultiStatistics;
import jcog.meter.event.PeriodMeter;
import jcog.pri.mix.control.CLink;
import nars.NAR;
import nars.control.SynchTaskExecutor;
import nars.task.ITask;
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

    final MultiStatistics<Class> types = new MultiStatistics<>();

    private final int collectionPeriod;


    private boolean trace = true;

    public InstrumentedExecutor(Executioner delegate) {
        this(delegate, 1);
    }

    public InstrumentedExecutor(Executioner delegate, int collectionPeriod) {
        this.exe = delegate;
        this.collectionPeriod = collectionPeriod;
        this.cycleTime = new PeriodMeter("cycleTime", collectionPeriod);
    }

    @Override
    public void start(NAR nar) {
        exe.start(nar);
    }

    @Override
    public void stop() {
        exe.stop();
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

            types.clear();

        }

    }

    public InstrumentedExecutor setTrace(boolean trace) {
        this.trace = trace;
        return this;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        types.print(sb);
        return sb.toString();

//        return Joiner.on('\n').join(
//                meters.values().stream().map(x -> PrimitiveTuples.pair(x.sum(), x.toStringMicro())).collect(
//                        Collectors2.toSortedListBy( x -> -x.getOne() )).stream().map(DoubleObjectPair::getTwo).iterator() );

    }

    @Override
    public int concurrency() {
        return exe.concurrency();
    }

    @Override
    public boolean run(@NotNull CLink<ITask> input) {
        types.accept(input.ref.getClass());
        exe.run(input);
        return false;
    }

    @Override
    public void forEach(Consumer<ITask> each) {
        exe.forEach(each);
    }

    @Override
    public void runLater(Runnable cmd) {
        exe.runLater(cmd);
    }
    //    @Override
//    public void run(@NotNull Consumer<NAR> r) {
//        exe.run( ()-> measure(() -> r.accept(nar), meter(r.getClass())));
//    }
//
//    @Override
//    public void run(Runnable cmd) {
//        exe.run( ()-> measure(cmd, meter(cmd.getClass())));
//    }
//

//        public PeriodMeter meter(Class cc) {
//        return types.
//        return meters.computeIfAbsent(cc, c -> new PeriodMeter(c.getName(), -1));
//    }

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
