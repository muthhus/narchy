package jcog.bag.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

/** striping via 64bit (pair of 32bit codes) global exclusion locking via busy spin
 *  on a linear probed atomic array of fixed size */
public class Treadmill extends AtomicLongArray {

    final AtomicInteger mod = new AtomicInteger(0);

    protected final static int concurrency = Runtime.getRuntime().availableProcessors() + 1;

    public Treadmill() {
        this(concurrency);
    }

    /** extra space for additional usage */
    public Treadmill(int slots) {
        super(slots);
    }

    public final int start(int context, int key) {
        long hash = (context << 32) | key;
        if (hash == 0) hash = 1; //reserve 0

        final int slots = length();

        while (true) {

            int now = mod.get();

            //check all first
            boolean collision = false;
            for (int i = 0; i < slots; i++) {
                long v = get(i);
                if (v == hash) {
                    collision = true;
                    break; //collision
                }
            }

            if (!collision) {
                if (mod.compareAndSet(now, now+1)) { //acquire exclusive access to write to the first free entry
                    for (int i = 0; i < slots; i++) {
                        if (compareAndSet(i, 0, hash))
                            return i; //ready
                    }
                } //else: another modification occurred since beginning the last check, must try again
            }

            //no free slots, continue spinning
            Thread.onSpinWait();
        }
    }

    public final void end(int slot) {
        set(slot, 0);
    }

//    public void end(int hash) {
//        if (hash == 0) hash = 1; //reserve 0
//
//        for (int i = 0; i < slots; i++) {
//            if (compareAndSet(i, hash, 0))
//                return; //done
//        }
//        throw new RuntimeException("did not remove ticket");
//    }

}
