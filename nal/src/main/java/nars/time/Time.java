package nars.time;

import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.servo.util.Clock;
import nars.NAR;
import nars.task.ITask;
import nars.task.NativeTask.SchedTask;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {

//    //TODO write this to replace LongObjectPair and cache the object's hash
//    public static class Scheduled extends LinkedList<Runnable> implements Comparable<Scheduled> {
//        long when;
//        public Scheduled(Runnable r) {
//            super();
//            super.add(r);
//        }
//
//        @Override
//        public boolean add(Runnable runnable) {
//            synchronized (getFirst()) {
//                return super.add(runnable);
//            }
//        }
//
//    }


    final PriorityQueue<SchedTask> scheduled =
    //final MinMaxPriorityQueue<SchedTask> scheduled =
            //MinMaxPriorityQueue.orderedBy((SchedTask a, SchedTask b) -> {
            new PriorityQueue<>();

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
        at(new SchedTask(whenOrAfter, then));
    }

    public void at(SchedTask event) {
        synchronized (scheduled) {
            scheduled.offer(event);
        }
    }


    @Nullable
    public List<SchedTask> exeScheduled() {

        long now = now();

        SchedTask firstQuick = scheduled.peek(); //it's safe to call this outside synchronized block for speed
        if (firstQuick == null || firstQuick.when > now)
            return null; //too soon for the next one

        List<SchedTask> pending = new LinkedList();

        synchronized (scheduled) {

            SchedTask next;
            while ((next = scheduled.poll()) != null) {
                if (next.when <= now) {
                    pending.add(next);
                } else {
                    //oops we need to reinsert this for some reason? can this even happen?
                    at(next);
                    break; //wait till another time
                }
            }
        }


        return pending;
    }

    public void cycle(NAR n) {
        n.input(exeScheduled());
    }


    /** flushes the pending work queued for the current time */
    public synchronized void synch(NAR n) {
        n.input(exeScheduled());
    }

    public long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


}
