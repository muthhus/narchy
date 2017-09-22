package nars.time;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.servo.util.Clock;
import nars.$;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {

    //TODO write this to replace LongObjectPair and cache the object's hash
    /*public static class Scheduled {

    }*/

    final MinMaxPriorityQueue<LongObjectPair<Runnable>> scheduled =
            MinMaxPriorityQueue.orderedBy((LongObjectPair<Runnable> a, LongObjectPair<Runnable> b) -> {
                if (a == b)
                    return 0;

                int t = Longs.compare(a.getOne(), b.getOne());
                if (t == 0) {
                    int h1 = Integer.compare(a.getTwo().hashCode(), b.getTwo().hashCode());
                    if (h1 == 0) {
                        //as a last resort, compare their system ID
                        return Integer.compare(System.identityHashCode(a.getTwo()), System.identityHashCode(b.getTwo())); //maintains uniqueness in case they occupy the same time
                    }
                    return h1;
                } else {
                    return t;
                }
            }).create();

    /**
     * called when memory reset
     */
    public abstract void clear();

    /**
     * returns the current time, as measured in units determined by this clock
     */
    @Override
    public abstract long now();


    /** time elapsed since last cycle */
    public abstract long sinceLast();

    /**
     * returns a new stamp evidence id
     */
    public abstract long nextStamp();


    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    public abstract int dur();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    public abstract Time dur(int d);


    public void at(long whenOrAfter, Runnable then) {
        LongObjectPair<Runnable> event = PrimitiveTuples.pair(whenOrAfter, then);
        synchronized (scheduled) {
            scheduled.offer(event);
        }
    }


    public void exeScheduled(Executor exe) {

        List<Runnable> pending = new LinkedList();

        synchronized (scheduled) {
            int ns = scheduled.size();
            if (ns == 0)
                return;

            LongObjectPair<Runnable> next;
            long now = now();
            while ((next = scheduled.peek()) != null) {
                if (next.getOne() <= now) {
                    scheduled.poll();
                    pending.add(next.getTwo());
                } else {
                    break; //wait till another time
                }
            }
        }


        //incase execution is synchronous, do it outside the synchronized block here to prevent deadlock.
        pending.forEach(exe::execute);
    }

    public void cycle(NAR n) {
        exeScheduled(n.exe);
    }


    /** flushes the pending work queued for the current time */
    public synchronized void synch() {
        exeScheduled(MoreExecutors.directExecutor());
    }

    public long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


}
