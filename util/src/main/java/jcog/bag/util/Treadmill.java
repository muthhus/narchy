package jcog.bag.util;

import java.util.concurrent.atomic.AtomicIntegerArray;

/** striped (32bit integer hashCode granularity) exclusion locking via busy spin
 *  on a linear probed atomic integer array of fixed size */
public class Treadmill extends AtomicIntegerArray {

    protected final static int concurrency = Runtime.getRuntime().availableProcessors();

    private final int slots;

    public Treadmill() {
        this(concurrency);
    }

    public Treadmill(int slots) {
        this(slots, 0);
    }

    /** extra space for additional usage */
    public Treadmill(int slots, int extra) {
        super(slots + extra);
        this.slots = slots;
    }

    /** get a value from the extra space */
    public int xGet(int index) {
        return get(slots + index);
    }

    /** set a value in the extra space */
    public void xSet(int index, int value) {
        set(slots + index, value);
    }

    /** compare and set a value in the extra space */
    public boolean xCompareAndSet(int index, int expected, int newValue) {
        return compareAndSet(slots + index, expected, newValue);
    }

    public int xGetAndSet(int index, int value) {
        return getAndSet( slots + index, value );
    }

    public int xIncrementAndGet(int index) {
        return incrementAndGet(slots + index);
    }

    public int xDecrementAndGet(int index) {
        return decrementAndGet(slots + index);
    }

    public void start(int hash) {
        if (hash == 0) hash = 1; //reserve 0
        restart:
        while (true) {

            int best = -1;
            for (int i = 0; i < slots; i++) {
                int v = get(i);
                if (v == hash) {
                    continue restart; //collision
                } else if (v == 0) {
                    best = i;
                }
            }

            if (best != -1) {
                if (compareAndSet(best, 0, hash))
                    return; //ready
            }

            //no free slots, continue spinning
            Thread.onSpinWait();
        }
    }

    public void end(int hash) {
        if (hash == 0) hash = 1; //reserve 0

        for (int i = 0; i < slots; i++) {
            if (compareAndSet(i, hash, 0))
                return; //done
        }
        throw new RuntimeException("did not remove ticket");
    }

}
