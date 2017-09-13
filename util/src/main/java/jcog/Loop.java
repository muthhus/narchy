package jcog;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop implements Runnable {

    protected static final Logger logger = getLogger(Loop.class);

    public final AtomicReference<Thread> thread = new AtomicReference();

    protected final int windowLength = 4;

    private float lag, lagSum;

    protected long prevTime;

    public final DescriptiveStatistics frameTime = new DescriptiveStatistics(windowLength); //in millisecond

    public static Loop of(Runnable iteration) {
        return new Loop() {
            @Override public boolean next() {
                iteration.run();
                return true;
            }
        };
    }

    /**
     * < 0: paused
     * 0: loop at full speed
     * > 0: delay in milliseconds
     */
    protected final AtomicInteger periodMS = new AtomicInteger(-1);


    @Override
    public String toString() {
        return super.toString() + " ideal=" + periodMS + "ms, " +
                Texts.n4(frameTime.getMean()) + "+-" + Texts.n4(frameTime.getStandardDeviation()) + "ms avg";
    }

    /**
     * create but do not start
     */
    public Loop() {

    }

    /**
     * create and auto-start
     */
    public Loop(float fps) {
        this();
        runFPS(fps);
    }

    /**
     * create and auto-start
     */
    public Loop(int periodMS) {
        this();
        setPeriodMS(periodMS);
    }

    public boolean isRunning() {
        return thread.get() != null;
    }

    public final Loop runFPS(float fps) {
        setPeriodMS((int) (1000f / fps));
        return this;
    }

    public final Loop runMS(int periodMS) {
        setPeriodMS(periodMS);
        return this;
    }

    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {
                Thread myNewThread = new Thread(this);
                if (thread.compareAndSet(null, myNewThread)) {
                    myNewThread.start();
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS < 0) {
                Thread prevThread;
                if ((prevThread = thread.getAndSet(null)) != null) {
                    try {
                        prevThread.stop();
                    } catch (Throwable ignored) { }
                    logger.info("stop {}", this);
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS >= 0) {
                //change speed
                logger.info("{} period={}ms", this, nextPeriodMS);
            }
            return true;
        }
        return false;
    }

    public void stop()  {
        setPeriodMS(-1);
    }

    /** for subclass overriding; called from the looping thread */
    protected void onStart() {

    }
    /** for subclass overriding; called from the looping thread */
    protected void onStop() {

    }

    /**
     * dont call this directly
     */
    @Override
    public final void run() {

        onStart();

        logger.info("start {} @ {}ms period", this, periodMS.get());

        prevTime = System.currentTimeMillis();

        int periodMS;
        long beforeTime = System.currentTimeMillis();
        while ((periodMS = this.periodMS.get()) >= 0) {


            try {

                if (!next())
                    break;

            } catch (Throwable e) {
                logger.error(" {}", e);
                /*nar.eventError.emit(e);
                if (Param.DEBUG) {
                    stop();
                    break;
                }*/
            }


            if (periodMS > 0) {
                //if we have a set period time, delay as appropriate otherwise continue immediately with the next cycle
                this.prevTime = beforeTime;
                //periodMS <= 0 ? System.currentTimeMillis() : Util.pauseWaitUntil(prevPrevTime + periodMS);

                long afterTime = System.currentTimeMillis();
                long frameTime = afterTime - beforeTime;

                lagSum += (this.lag = Math.max(0, (frameTime - periodMS) / ((float) periodMS)));

                //System.out.println(getClass() + " " + frameTime + " " + periodMS + " " + lag);

                this.frameTime.addValue(frameTime);

                Util.sleep((int)( periodMS - ((long) this.frameTime.getMean()) ));

                beforeTime = System.currentTimeMillis();

            } else {
                Thread.yield();
                //Thread.onSpinWait();
            }


        }

        stop();

        onStop();

        lag = lagSum = 0;
    }


    abstract public boolean next();

    public void join() {
        try {
            Thread t = thread.get();
            if (t!=null) {
                t.join();
            } else {
                throw new RuntimeException("not started");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public Thread thread() {
        return thread.get();
    }

    /** lag in proportion to the current FPS, >= 0 */
    public float lag() {
        return lag;
    }

    public float lagSumThenClear() {
        float l = lagSum;
        this.lagSum = 0;
        return l;
    }
}
