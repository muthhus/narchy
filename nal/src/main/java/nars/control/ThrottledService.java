package nars.control;

import jcog.Texts;
import jcog.math.RecycledSummaryStatistics;
import jcog.math.RecyclingPolynomialFitter;
import jcog.pri.Pri;
import nars.NAR;

/**
 * instruments the runtime resource consumption of its iterated procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 */
abstract public class ThrottledService extends DurService {

    /** upper limit for each individual work unit */
    private static final double OVERLOAD_NS = 500000; /* 0.5ms */

    /** lower limit for each individual work unit */
    private static final double UNDERLOAD_NS = 500; /* 0.5us */

    /** how much swing (difference) should be sought between the mean and the max value */
    private static final double COMPRESSION_RATIO = 3;

    /** absolute upper limit for any assigned workload. this determines the maximum granularity
     * of the scheduler, and should be quite finite. */
    private static final int UNIT_MAX = 16 * 1024;


    /** value specific to this instance indicating the relative workload response */
    float workGranularity = 1;

    /**
     * basic estimator of: workUnits / realtime (ns)
     */
    final RecyclingPolynomialFitter workEstimator =
            new RecyclingPolynomialFitter(2, 16, Integer.MAX_VALUE)
                .tolerate(1 /* work unit */, 1 /* ns */);

    public ThrottledService(NAR nar) {
        super(nar);

        //initial values
        workEstimator.learn(0,0);
        workEstimator.learn(1,1);
    }

    @Override
    protected final void run(NAR n, long dt) {
        float v = value();

        //        if (workAllowed < Pri.EPSILON)
//            return;
        StringBuilder summary = new StringBuilder(256);
        summary.append("\t" + this + " x " + Texts.n1(v * workGranularity) + "/" + workGranularity + "\t");
        RecycledSummaryStatistics s = new RecycledSummaryStatistics();
        for (float i = 0.1f; i <= 1; i+=0.1f) {
            double t = workEstimator.guess(i * workGranularity);
            s.accept(t);
            summary.append(" " + Texts.n1(i) + "=" + Texts.n1((float) t) + "ns");
        }
        System.out.println(summary);


        //principles:
        //  1. if the distribution of estimates is too flat, or contains negatives, it indicates the granularity of work assignment is too low for the procedure. so increase it
        //  2. if a value exceeds the allowable limit point for a task, that indicates the work amount needs scaled down.
        {
            //example naive policy
            double max = s.getMax();
            if (s.getMin() > OVERLOAD_NS) {
                workGranularity = Math.max(1, workGranularity * 0.9f); //backoff
            } else if (s.getMax() < UNDERLOAD_NS) {
                workGranularity = Math.min(UNIT_MAX, workGranularity * 1.25f); //surge hard
            } else if (max / s.getMean() < COMPRESSION_RATIO) {
                //assert(mean!=0);
                workGranularity = Math.min(UNIT_MAX, workGranularity * 1.05f); //surge soft
            } else {
                workGranularity = Math.max(1, workGranularity * 0.99f); //constant gentle backoff
            }
        }

        float workAllowed = v * workGranularity;

        Throwable error = null;
        long start = System.nanoTime();
        float workDone;
        try {
            workDone = run(n, dt, workAllowed);
            assert (workDone == workDone && workDone >= 0);
        } catch (Throwable t) {
            error = t;
            workDone = 0;
        }
        long end = System.nanoTime();

        if (workDone > workAllowed) {
            float ratio = workDone / workAllowed;
            workGranularity = workGranularity * ratio;
        }
        learn(dt, workAllowed, workDone, (end - start));
        if (error != null)
            throw new RuntimeException(error);
    }


    protected void learn(long waitNS, float workAllowed, float workDone, long runtimeNS) {
        //double cycleTime = runtimeNS + waitNS;
        //double dutyCycle = runtimeNS / cycleTime; //estimate
        workEstimator.learn(workDone, runtimeNS);
    }


    /**
     * returns an estimate of the work completed, some value between 0..workAllowed
     * @param work a factor representig work allowed, ie. strength of computational effort
     */
    protected abstract float run(NAR n, long dt, float work);

    /** returns the system estimated value of running this */
    public abstract float value();

}
