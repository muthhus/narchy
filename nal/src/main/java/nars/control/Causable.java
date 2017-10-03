package nars.control;

import jcog.exe.Schedulearn;
import nars.NAR;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

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


    public final Schedulearn.Can can = new Schedulearn.Can();
    private final AtomicBoolean busy;

    public Causable(NAR nar) {
        super(nar);
        busy = singleton() ? new AtomicBoolean(false) : null;
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

    /** if true, allows multiple threads to execute on this instance */
    public boolean singleton() {
        return false;
    }

    public final int run(NAR n, int iterations) {

        if (singleton() && !busy.compareAndSet(false, true)) {
            return 0; //another thread running in here
        }

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

        can.update(completed, value(), (double)(end-start)/1.0E9);

        if (busy!=null)
            busy.set(false);

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

    /** returns a system estimated value of invoking this. between 0..1.0 */
    public abstract float value();

}
