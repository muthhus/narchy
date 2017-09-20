package nars.control;

import jcog.math.RecycledSummaryStatistics;
import jcog.math.RecyclingPolynomialFitter;
import jcog.pri.Pri;
import nars.NAR;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

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

    /** value specific to this instance indicating the relative workload response */
    float workGranularity = 1;

    public ThrottledService(NAR nar) {
        super(nar);

        //initial values
        workEstimator.learn(0,0);
        workEstimator.learn(1,1);
    }

    @Override
    protected final void run(NAR n, long dt) {
//        if (workAllowed < Pri.EPSILON)
//            return;
        System.out.print("\t" + this + " x " + workGranularity + "\t");
        RecycledSummaryStatistics s = new RecycledSummaryStatistics();
        for (float i = 0.1f; i <= 1; i+=0.1f) {
            double t = workEstimator.guess(i);
            s.accept(t);
            System.out.print(" " + i + "=" + Math.round(t)+"ms");
        }
        System.out.println();


        //principles:
        //  1. if the distribution of estimates is too flat, or contains negatives, it indicates the granularity of work assignment is too low for the procedure. so increase it
        //  2. if a value exceeds the allowable limit point for a task, that indicates the work amount needs scaled down.
        {
            //example naive policy
            //TODO use function solver to find estimates for target values

            if (s.getMin() <= 1f) {
                workGranularity = Math.min(1024, workGranularity * 1.5f);
            } else {
                workGranularity = Math.max(1, workGranularity * 0.99f);
            }
        }

        float workAllowed = value() * workGranularity;

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
        learn(dt/1000000.0, workAllowed, workDone, (end - start)/1000000.0);

        if (error != null)
            throw new RuntimeException(error);
    }

    /**
     * basic estimator:  dims: work done, compute time (ms)
     */
    final RecyclingPolynomialFitter workEstimator = new RecyclingPolynomialFitter(3, 16, Integer.MAX_VALUE);

    protected void learn(double waitNS, float workAllowed, float workDone, double runtimeNS) {
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
