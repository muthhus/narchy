package nars.control;

import jcog.util.AtomicFloat;

/** concurrent traffic accumulator;
 *  concurrent updates (ie. add) but expects a synchronous commit
 *  to sample after each cycle.
 *
 *  the AtomicFloat which this subclasses holds the accumulating value
 *  that safely supports multiple concurrent accumulators */
public class Traffic extends AtomicFloat {

    /** previous value */
    public float prev;

    /** current value */
    public float current;

    public double total;

    public final void commit() {
        this.prev = this.current;
        double next = getAndSet(0f);
        this.total += (this.current = (float) next);
    }
}
