package nars.util;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop implements Runnable {
    protected static final Logger logger = getLogger(Loop.class);


    /**
     * sleep mode delay time
     */
    @NotNull
    protected final Thread thread;
    protected final int windowLength = 16;

    protected long afterTime;
    protected volatile boolean stopping;
    protected volatile boolean stopped;
    public final DescriptiveStatistics frameTime = new DescriptiveStatistics(windowLength); //in millisecond
    private int periodMS;


    public Loop(String threadName, int periodMS) {
        this(threadName);

        start(periodMS);
    }

    public Loop(String threadName, float fps) {
        this(threadName, (int)(1000f/fps));
    }

    public Loop(String threadName) {
        thread = new Thread(this, threadName);
    }

    protected final void start(float fps) {
        start((int)(1000f/fps));
    }

    protected void start(int period) {

        setPeriodMS(period);
        thread.start();
        logger.info("started {}", thread);
    }

    public Loop at(float fps) {
        setPeriodMS((int)(1000f/fps));
        return this;
    }

    /** dont call this directly */
    @Override public final void run() {

        afterTime = System.currentTimeMillis();

        while (!stopping) {
            try {



                long beforeTime = System.currentTimeMillis();

                next();

                //this.prevTime = Util.pauseLockUntil(prevTime + periodMS);
                long prevPrevTime = this.afterTime;

                //if we have a set period time, delay as appropriate otherwise continue immediately with the next cycle
                this.afterTime = System.currentTimeMillis();
                            //periodMS <= 0 ? System.currentTimeMillis() : Util.pauseWaitUntil(prevPrevTime + periodMS);

                long frameTime = afterTime - prevPrevTime;

                long delayable = (beforeTime+periodMS) - afterTime;
                if (delayable > 0) {
                    //logger.info("delay {}", delayable);
                    Thread.sleep(delayable);
                }

                this.frameTime.addValue(frameTime);


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


    abstract public void next();

    @Deprecated public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public final void setPeriodMS(int period) {
        this.periodMS = period;
//        int prevPeriod = periodMS;
//
//        if (prevPeriod == period) return false;
//
//        periodMS = period;
//
//        if (period == -1) {
//            logger.info("pause");
//        } else {
//            if (prevPeriod == -1)
//                logger.info("resume:period={}", period);
//            else {
//                //dont log change in period, too noisy
//            }
//        }
//
//        //thread priority control
//        if (thread != null) {
//            //int pri = thread.getPriority();
//
//            thread.interrupt();
//
//        }
//
//
//        return true;
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

    public final void pause() {
        setPeriodMS(-1);
    }
}
