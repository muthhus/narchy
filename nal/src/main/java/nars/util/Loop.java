package nars.util;

import jcog.Texts;
import jcog.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static nars.time.Tense.DTERNAL;
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
    protected boolean stopping;
    protected boolean stopped;
    public final DescriptiveStatistics frameTime = new DescriptiveStatistics(windowLength); //in millisecond
    private int periodMS = DTERNAL;

    @Override
    public String toString() {
        return "ideal=" + periodMS + "ms, " +
                Texts.n4(frameTime.getMean()) + "+-" + Texts.n4(frameTime.getStandardDeviation()) + "ms avg";
    }

    public Loop(@NotNull String threadName, int periodMS) {
        this(threadName);
        start(periodMS);
    }

    public Loop(@NotNull String threadName, float fps) {
        this(threadName);
        start(fps);
    }

    public Loop() {
        thread = new Thread(this);
    }

    public Loop(float fps) {
        this();
        start(fps);
    }

    public Loop(int periodMS) {
        this();
        start(periodMS);
    }

    public Loop(@NotNull String threadName) {
        thread = new Thread(this, threadName);
    }

    protected final void start(float fps) {
        start((int) (1000f / fps));
    }

    protected void start(int period) {
        if (setPeriodMS(period)) {
            logger.info("start {}", thread);
            thread.start();
        }
    }


    /**
     * dont call this directly
     */
    @Override
    public final void run() {

        afterTime = System.currentTimeMillis();

        while (!stopping) {

            //this.prevTime = Util.pauseLockUntil(prevTime + periodMS);
            long prevPrevTime = this.afterTime;

            long beforeTime = System.currentTimeMillis();

            //try {

            if (!next())
                break;

//            } catch (Throwable e) {
//                logger.error("{}", e);
//                /*nar.eventError.emit(e);
//                if (Param.DEBUG) {
//                    stop();
//                    break;
//                }*/
//            }


            //if we have a set period time, delay as appropriate otherwise continue immediately with the next cycle
            this.afterTime = System.currentTimeMillis();
            //periodMS <= 0 ? System.currentTimeMillis() : Util.pauseWaitUntil(prevPrevTime + periodMS);

            long frameTime = afterTime - prevPrevTime;

            long delayable = (beforeTime + periodMS) - afterTime;
            if (delayable > 0) {
                //logger.info("delay {}", delayable);
                //Util.pause(delayable);
                Util.sleep(delayable);
            }

            this.frameTime.addValue(frameTime);

        }

        logger.info("stopped");
        stopped = true;
        stopping = false;
    }


    abstract public boolean next();

    public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public final synchronized boolean setPeriodMS(int newPeriod) {

        int oldPeriod = periodMS;
        if (oldPeriod != newPeriod) {

            logger.info("period={}ms", newPeriod);

            boolean paused = (isPaused());

            this.periodMS = newPeriod;
            if (paused) {
                logger.info("waking {}", thread);
                thread.interrupt();
            }

            return true;

        }
        return false;

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

    public synchronized void stop() /*throws InterruptedException */ {

        logger.info("stopping {}", this);

        stopping = true;
        thread.stop();
        stopped = true;


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

    public boolean isPaused() {
        return periodMS == -1;
    }

    public boolean isStopped() {
        return stopped;
    }
}
