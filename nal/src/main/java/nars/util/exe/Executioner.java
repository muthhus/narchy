package nars.util.exe;

import nars.NAR;
import nars.Task;
import nars.task.ITask;
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

    public boolean isRunning() {
        return nar!=null;
    }

    public void start(NAR nar) {
        //if (this.nar == null) {
            this.nar = nar;
//        } else {
//            throw new RuntimeException("already started");
//        }
    }

    /** needs to set nar to null */
    public void stop() {
        this.nar = null;
    }

    abstract public void cycle(@NotNull NAR nar);


    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();

    /** true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to bve safe.
     */
    public final boolean concurrent() {
        return concurrency() > 1;
    }




    /** default impl: */
    abstract public void run(@NotNull Consumer<NAR> r);

//        if (nar!=null) {
//            this.run(() -> r.accept(nar));
//        } else {
//            throw new RuntimeException("stopped");
//        }


    abstract public void runLater(Runnable cmd);


    /** a positive or negative value indicating the percentage difference from the
     * currently configured CPU usage target to the actual measured CPU usage.
     *
     * tasks can use this value to determine runtime parameters.
     *
     * if this value is positive then more work is allowed, if negative, less work is allowed.
     *
     * @return
     */
    //TODO public float throttle() { return 0; }


    /** a scaling factor that executions can use to throttle the workload they will produce in the next cycle */
    public float load() { return 0; }

    @Override
    public final void execute(Runnable whenIGetAroundToIt) {
        runLater(whenIGetAroundToIt);
    }

    abstract public void run(@NotNull ITask input);
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
