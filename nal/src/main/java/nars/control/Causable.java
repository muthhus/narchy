package nars.control;

import nars.NAR;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 */
abstract public class Causable extends NARService {

    private static final Logger logger = LoggerFactory.getLogger(Causable.class);

    /** upper limit for each individual work unit */
    private static final double OVERLOAD_NS = 500000; /* 0.5ms */

    /** lower limit for each individual work unit */
    private static final double UNDERLOAD_NS = 500; /* 0.5us */

    /** how much swing (difference) should be sought between the mean and the max value */
    private static final double COMPRESSION_RATIO = 3;

    /** absolute upper limit for any assigned workload. this determines the maximum granularity
     * of the scheduler, and should be quite finite. */
    private static final int UNIT_MAX = 16 * 1024;


//    /** value specific to this instance indicating the relative workload response */
//    float workGranularity = 1;

//    /**
//     * basic estimator of: workUnits / realtime (ns)
//     */
//    final RecyclingPolynomialFitter workEstimator =
//            new RecyclingPolynomialFitter(2, 16, Integer.MAX_VALUE)
//                .tolerate(1 /* work unit */, 1 /* ns */);

    final int WINDOW = 16;
    final DescriptiveStatistics iterationTimeNS = new DescriptiveStatistics(WINDOW);
    final DescriptiveStatistics iterationCount = new DescriptiveStatistics(WINDOW);
    public double cycleInvest; //temporary

    public Causable(NAR nar) {
        super(nar);
        //initial values
//        workEstimator.learn(0,0);
//        workEstimator.learn(1,1);
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);

        synchronized (nar.causables) {
            nar.causables.add(this);
        }

    }

    @Override
    protected void stop(NAR nar) {

        synchronized (nar.causables) {
            boolean removed = nar.causables.remove(this);
            assert(removed);
        }

        super.stop(nar);
    }

    protected final int run(NAR n, int iterations) {

        Throwable error = null;
        long start = System.nanoTime();
        int completed = 0;
        try {
            completed = next(n, iterations);
            assert (completed >=0 && completed <= iterations);
        } catch (Throwable t) {
            error = t;
        }
        long end = System.nanoTime();

        iterationTimeNS.addValue(  (double)(end-start) / completed );
        iterationCount.addValue(completed);

        if (error != null) {
            logger.error("{} {}", this, error);
        }

        return completed;
    }

//
//    protected void learn(long waitNS, float workAllowed, float workDone, long runtimeNS) {
//        //double cycleTime = runtimeNS + waitNS;
//        //double dutyCycle = runtimeNS / cycleTime; //estimate
//        workEstimator.learn(workDone, runtimeNS);
//    }


    /** returns iterations actually completed */
    protected abstract int next(NAR n, int iterations);

    /** returns the system estimated value of running this */
    public abstract float value();

    public double estimatedIterationTimeNS() {
        return (iterationTimeNS.getMean());
    }
    public double iterationsMean() {
        return (iterationCount.getMean());
    }

}
