package nars.time;

import com.netflix.servo.util.Clock;
import nars.NAR;
import nars.task.NativeTask.SchedTask;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

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
    public void at(long whenOrAfter, Consumer<NAR> then) {
        at(new SchedTask(whenOrAfter, then));
    }

    public void at(SchedTask event) {
        synchronized (scheduled) {
            scheduled.offer(event);
        }
    }


    @Nullable
    public List<SchedTask> exeScheduled() {

        //        now = now();
//        SchedTask firstQuick = scheduled.peek(); //it's safe to call this outside synchronized block for speed
//        if (firstQuick == null || firstQuick.when > now)
//            return null; //too soon for the next one

        List<SchedTask> pending = new LinkedList();

        synchronized (scheduled) {

            long now = now(); //get time now inside the synch in case it had to wait to enter

            SchedTask next;
            while (((next = scheduled.peek()) != null) && (next.when <= now)) {
                SchedTask next2 = scheduled.poll(); assert(next==next2);
                pending.add(next);
            }
        }


        return pending;
    }

    public void cycle(NAR n) {
        n.input(exeScheduled());
    }


    /** flushes the pending work queued for the current time */
    public void synch(NAR n) {
        n.input(exeScheduled());
    }

    public long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


}
