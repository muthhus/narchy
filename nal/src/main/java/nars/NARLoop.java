package nars;

import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 * TODO extract the hft core reservation to a subclass and put that in the app module, along with the hft dependency
 *
 * mostly replaced by Executioner's
 */
@Deprecated public class NARLoop implements Runnable {

    private static final Logger logger = getLogger(NARLoop.class);

    @NotNull
    public final NAR nar;

    @NotNull
    private final Thread thread;

    /**
     * sleep mode delay time
     */
    static final long sleepTimeMS = 250;


    volatile int periodMS = 1000;
    private volatile boolean stopping,  stopped;
    //private boolean running;


    final int windowLength = 16;
    public final DescriptiveStatistics frameTime = new DescriptiveStatistics(windowLength); //in millisecond


    /** average desired cpu percentage */
    public final MutableFloat priority = new MutableFloat(1f);


    @NotNull
    @Override
    public String toString() {
        return nar + ":loop@" + getFrequency() + "Hz";
    }

    //in Hz / fps
    public double getFrequency() {
        return 1000.0 / periodMS;
    }

//    public int getPeriodMS() {
//        return periodMS;
//    }
//

    public NARLoop(@NotNull NAR n) {
        this(n, 0);
    }

    @Deprecated public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param n
     * @param initialPeriod
     */
    public NARLoop(@NotNull NAR n, int initialPeriod) {


        nar = n;


        setPeriodMS(initialPeriod);

        thread = new Thread(this, n.self + ":loop");
        thread.start();
        logger.info("started {}", thread);
    }


    public final boolean setPeriodMS(int period) {
        int prevPeriod = periodMS;

        if (prevPeriod == period) return false;

        periodMS = period;

        if (period == -1) {
            logger.info("pause");
        } else {
            if (prevPeriod == -1)
                logger.info("resume:period={}", period);
            else {
                //dont log change in period, too noisy
            }
        }

        //thread priority control
        if (thread != null) {
            //int pri = thread.getPriority();

            thread.interrupt();

        }


        return true;
    }

    public void stop() /*throws InterruptedException */ {
        logger.info("stopping {}", this);

        stopping = true;

//        synchronized (thread) {
//            if (stopping || stopped)
//                throw new RuntimeException("already waiting for stop");
//
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }


    /** dont call this directly */
    @Override public final void run() {

//        AffinityLock al;
//        if (cpuCoreReserve) {
//            al = AffinityLock.acquireLock();
//            if (al.isAllocated() && al.isBound()) {
//                logger.info(thread + " running exclusively on CPU " +  al.cpuId());
//            }
//        }
//        else {
//            al = null;
//        }

        /* try */
        NAR nar = this.nar;

        if (periodMS != -1)
            logger.info("started, period={}", periodMS);

        prevTime = System.currentTimeMillis();

        while (!stopping) {
            try {
                frame(nar);
            } catch (Throwable e) {
                logger.error("{}",e.toString());
                e.printStackTrace();
                /*nar.eventError.emit(e);
                if (Param.DEBUG) {
                    stop();
                    break;
                }*/
            }
        }

        logger.info("stopped");
        stopped = true;
        stopping = false;
    }

    long prevTime;

    public void frame(@NotNull NAR nar) {
        int periodMS = this.periodMS;

        if (periodMS < 0) {
            //idle
            Util.pause(sleepTimeMS);
        } else {

            //if (!nar.running.get()) {

            nar.run(1);

                //this.prevTime = Util.pauseLockUntil(prevTime + periodMS);
            long prevPrevTime = this.prevTime;

            //if we have a set period time, delay as appropriate otherwise continue immediately with the next cycle

            this.prevTime = periodMS <= 0 ? System.currentTimeMillis() : Util.pauseWaitUntil(prevPrevTime + periodMS);

            frameTime.addValue(prevTime - prevPrevTime);

                //throttle(periodMS, System.currentTimeMillis() - lastTime);


//            } else {
//                //logger.warn("nar began running before this frame attempted to start");
//                Thread.yield();
//            }

        }

    }

//    protected static long throttle(long minFramePeriodMS, long frameTimeMS) {
//        double remainingTime = (minFramePeriodMS - frameTimeMS) / 1.0E3;
//
//        if (remainingTime > 0) {
//
//            //        try {
////            Thread.sleep(sleepTime);
////        } catch (InterruptedException e) {
////            //e.printStackTrace();
////        }
//
//            Util.pause(minFramePeriodMS);
//
//        } else if (remainingTime < 0) {
//
//            Thread.yield();
//
//            if (Global.DEBUG) {
//                //TODO blink a non-intrusive indicator in GUI
//                logger.warn("lag {}ms", remainingTime);
//            }
//
//            //minFramePeriodMS++;
//            //; incresing frame period to " + minFramePeriodMS + "ms");
//        }
//        return minFramePeriodMS;
//    }


    public final void pause() {
        setPeriodMS(-1);
    }

//    //TODO not well tested
//    public void setRunning(boolean r) {
//        this.running = r;
//    }
//
//    public boolean isRunning() {
//        return running;
//    }
}
