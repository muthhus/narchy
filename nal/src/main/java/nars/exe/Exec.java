package nars.exe;

import jcog.event.On;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Premise;
import nars.task.ITask;
import nars.task.NALTask;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * manages low level task scheduling and execution
 *
 */
abstract public class Exec implements Executor, PriMerge {

    protected NAR nar;

    private On onClear;


    /** schedules the task for execution but makes no guarantee it will ever actually execute */
    abstract public void add(/*@NotNull*/ ITask input);

    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();


    abstract public Stream<ITask> stream();

    public synchronized void start(NAR nar) {
        this.nar = nar;

        assert(onClear == null);
        onClear = nar.eventClear.on((n)->clear());
    }

    public synchronized void stop() {
        if (onClear!=null) {
            onClear.off();
            onClear = null;
        }
    }

    protected synchronized void clear() {

    }

  /** visits any pending tasks */
    @Deprecated public final void forEach(Consumer<ITask> each) {
        stream().forEach(each);
    }


    /** true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to bve safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }

    protected void ignore(@NotNull Task t) {
        t.delete();
        nar.emotion.taskIgnored.increment();
    }

    @Override
    public void execute(Runnable r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(r);
        } else {
            r.run();
        }
    }


    public void print(PrintStream out) {
        out.println(this);
    }

    @Override
    public float merge(Priority existing, Prioritized incoming) {
        if (existing instanceof Activate) {
            return Param.activateMerge.merge(existing, incoming);
        } else if (existing instanceof Premise) {
            ((Premise)existing).merge((Premise)incoming);
            return Param.premiseMerge.merge(existing, incoming);
        }else {
            if (existing instanceof NALTask) {
                ((NALTask)existing).causeMerge((NALTask) incoming);
            }
            return Param.taskMerge.merge(existing, incoming);
        }

    }

    //    public class Periodic extends Loop {
//
//        final AtomicBoolean busy = new AtomicBoolean(false);
//        //private final FloatParam fps = new FloatParam(0);
//        private final Runnable task;
//        private long last;
//
//        public Periodic(float fps, Runnable task) {
//            super(fps);
//            this.task = task;
//            this.last = nar.time();
//        }
//
//        @Override
//        public boolean next() {
//            if (nar.time() <= this.last)
//                return true; //hasn't proceeded to next cycle
//
//            if (!busy.compareAndSet(false, true)) {
//                return true; //black-out, the last frame didnt even finish yet
//            }
//
//            //runLater(()->{
//                try {
//                    last = nar.time();
//                    task.run();
//                } finally {
//                    busy.set(false);
//                }
//            //});
//
//            return true;
//        }
//
//
//    }

}

//package nars.nar.exe;
//
//import nars.util.Texts;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.management.ManagementFactory;
//import java.lang.management.ThreadMXBean;
//
///**
// * Created by me on 10/17/16.
// */
//public class CPUThrottle {
//
//    protected final DescriptiveStatistics recentDuty;
//    protected final DescriptiveStatistics recentPeriod;
//
//    private static final Logger logger = LoggerFactory.getLogger(CPUThrottle.class);
//
//    /**
//     * cycles per second
//     */
//    protected final MutableFloat targetRate;
//
//
//    long lastTime;
//    protected float throttle;
//
//
//    public CPUThrottle(MutableFloat targetRate /** cycles per second */) {
//
//        this.targetRate = targetRate;
//
//        float windowSeconds = 4;
//        int windowSize = (int)(targetRate.floatValue() * windowSeconds);
//        recentDuty = new DescriptiveStatistics(windowSize);
//        recentPeriod = new DescriptiveStatistics(windowSize);
//    }
//
//    public void next(Runnable r) {
//        long periodStart = now();
//
//        //try {
//        r.run();
////            } catch (Throwable t) {
////                logger.error(t);
////            }
//
//        long dutyEnd = now();
//        recentDuty.addValue(dutyEnd - periodStart);
//        recentPeriod.addValue(-(lastTime - (this.lastTime = periodStart)));
//
//        //long meanDuty = (long) recentDuty.getMean();
//        long tp = targetPeriod();
//        long sleepTime = tp - ((long) recentPeriod.getMean());
//        this.throttle = (float) (((double) (sleepTime)) / tp);
//
//        logger.info(" {}", summary());
//
//        if (sleepTime > 0) {
//            //Util.pause(sleepTime);
//            //logger.info("sleeping {}ms", sleepTime);
//            try {
//                Thread.sleep(sleepTime);
//            } catch (InterruptedException e) {
//            }
//        }
//
//
//    }
//
//    long targetPeriod() {
//        return (long) (1000f / targetRate.floatValue());
//    }
//
//    protected long now() {
//        return System.currentTimeMillis();
//    }
//
//    public String summary() {
//        return (targetRate.floatValue() + " ideal hz, " +
//                Texts.n2(1000f / recentPeriod.getMean()) + ".." + Texts.n2(1000f / recentDuty.getMean()) + " possible Hz: " +
//                //Texts.n2(cpuMS) + " cpu time ms,  ==> " +
//                Texts.n2(throttle) + " throttle"); //+ " cpu time (" + Texts.n2(cpuPercent) + "%)");
//
//
//    }
//
//    /**
//     * targets a specific CPU usage percentage
//     * http://stackoverflow.com/a/1235519
//     */
//    public static class CPUPercentageThrottle extends CPUThrottle {
//
//        static final ThreadMXBean TMB = ManagementFactory.getThreadMXBean();
//
//        static {
//            if (!TMB.isThreadCpuTimeSupported()) {
//                System.err.println("CPU Time Management not supported by " + TMB);
//                System.exit(1);
//            }
//            if (!TMB.isThreadCpuTimeEnabled()) {
//                TMB.setThreadCpuTimeEnabled(true);
//            }
//        }
//
//        private final long[] threadIDs;
//        private final long[] threadTime;
//
//        final MutableFloat targetCPU;
//        float deltaCPU;
//
//
//        public CPUPercentageThrottle(MutableFloat baseRate, MutableFloat targetCPU, long[] threadIDs) {
//            super(baseRate);
//
//            this.targetCPU = targetCPU;
//
//            if (threadIDs.length == 0)
//                throw new RuntimeException("no threads to measure");
//
//            this.threadIDs = threadIDs;
//            this.threadTime = new long[threadIDs.length];
//        }
//
//        @Override
//        public void next(Runnable r) {
//
//            super.next(r);
//
//            long c = cputime();
//            int numThreads = threadIDs.length;
//            long cpuNS = c / numThreads;
//            double cpuPct = cpuNS / 1.0E9;
//            double cpuMS = cpuNS / 1.0E6;
//
//
//            deltaCPU = (targetCPU.floatValue() - (float) cpuPct) / targetCPU.floatValue();
//
//
//            //cpuperc = (TMB.getCurrentThreadCpuTime() - cput);// / (new Date().getTime() *  1000000.0 - time) * 100.0;
//
////If cpu usage is greater then 50%
////            if (cpuperc > 50.0) {
////                //sleep for a little bit.
////                continue;
////            }
//
//        }
//
//        /**
//         * in nanoseconds
//         */
//        private long cputime() {
//
//            long sum = 0;
//            for (int i = 0; i < threadIDs.length; i++) {
//                long h = threadIDs[i];
//                long prev = threadTime[i];
//                long now = TMB.getThreadCpuTime(h);
//                sum += now - prev;
//                threadTime[i] = now;
//            }
//
//            return sum;
//        }
//    }
//
//
//}

//    /**
//     * run a procedure for each item in chunked stripes
//     */
//    public final <X> void runLater(@NotNull List<X> items, @NotNull Consumer<X> each, int maxChunkSize) {
//
//        int conc = exe.concurrency();
//        if (conc == 1) {
//            //special single-thread case: just execute all
//            items.forEach(each);
//        } else {
//            int s = items.size();
//            int chunkSize = Math.max(1, Math.min(maxChunkSize, (int) Math.floor(s / conc)));
//            for (int i = 0; i < s; ) {
//                int start = i;
//                int end = Math.min(i + chunkSize, s);
//                runLater(() -> {
//                    for (int j = start; j < end; j++) {
//                        X x = items.get(j);
//                        if (x != null)
//                            each.accept(x);
//                    }
//                });
//                i += chunkSize;
//            }
//        }
//    }
