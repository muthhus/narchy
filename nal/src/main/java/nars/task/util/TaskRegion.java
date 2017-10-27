package nars.task.util;

import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;

import static nars.task.util.TaskRegion.nearestBetween;
import static nars.time.Tense.ETERNAL;

public interface TaskRegion extends HyperRegion, Tasked {

    /**
     * relative to time sameness (1)
     */
    float FREQ_SAMENESS_IMPORTANCE = 0.25f;
    /**
     * relative to time sameness (1)
     */
    float CONF_SAMENESS_IMPORTANCE = 0.05f;

    static long nearestBetween(long s, long e, long when) {
        assert (when != ETERNAL);

        if (s == ETERNAL) {
            return when;
        } else if (when < s || e == s) {
            return s; //point or at or beyond the start
        } else if (when > e) {
            return e; //at or beyond the end
        } else {
            return when; //internal
        }
    }

    static long furthestBetween(long s, long e, long when) {
        assert (when != ETERNAL);

        if (s == ETERNAL) {
            return when;
        } else if (when < s || e == s) {
            return e; //point or at or beyond the start
        } else if (when > e) {
            return s; //at or beyond the end
        } else {
            //internal, choose most distant endpoint
            if (Math.abs(when - s) > Math.abs(when - e))
                return s;
            else
                return e;
        }
    }

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    long start();

    long end();

    default long mid() {
        return (start() + end()) / 2;
    }

    default long nearestTimeTo(long when) {
        return nearestBetween(start(), end(), when);
    }

    default long furthestTimeTo(long when) {
        return furthestBetween(start(), end(), when);
    }

    @Override
    default double cost() {
        return timeCost() * freqCost() * confCost();
    }

    @Override
    default double perimeter() {
        return timeCost() + freqCost() + confCost();
    }


    default float timeCost() {
        return (float) (1 + range(0));
    }

    default float freqCost() {
        float d = (float) range(1);
        return 1 + d /* * timeCost()*/ * FREQ_SAMENESS_IMPORTANCE;
    }

    default float confCost() {
        float d = (float) range(2);
        return 1 + d /* * timeCost()*/ * CONF_SAMENESS_IMPORTANCE;
    }


    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        if (this == r)
            return this;
        else {
            if (r instanceof Task) {
                //accelerated mbr
                Task er = (Task) r;
                float ef = er.freq();
                float ec = er.conf();
                if (this instanceof Task) {
                    Task tr = (Task) this;
                    float tf = tr.freq();
                    float tc = tr.conf();
                    float f0, f1, c0, c1;
                    if (tf <= ef) {
                        f0 = tf;
                        f1 = ef;
                    } else {
                        f0 = ef;
                        f1 = tf;
                    }
                    if (tc <= ec) {
                        c0 = tc;
                        c1 = ec;
                    } else {
                        c0 = ec;
                        c1 = tc;
                    }
                    return new TasksRegion(
                            Math.min(start(), er.start()), Math.max(end(), er.end()),
                            f0, f1, c0, c1
                    );
                } else {
                    return new TasksRegion(
                            Math.min(start(), er.start()), Math.max(end(), er.end()),
                            (float) Math.min(coord(false, 1), ef),
                            (float) Math.max(coord(true, 1), ef),
                            (float) Math.min(coord(false, 2), ec),
                            (float) Math.max(coord(true, 2), ec)
                    );
               }
            } else {
                TaskRegion er = (TaskRegion) r;
                return new TasksRegion(
                        Math.min(start(), er.start()), Math.max(end(), er.end()),
                        (float) Math.min(coord(false, 1), er.coord(false, 1)),
                        (float) Math.max(coord(true, 1), er.coord(true, 1)),
                        (float) Math.min(coord(false, 2), er.coord(false, 2)),
                        (float) Math.max(coord(true, 2), er.coord(true, 2))
                );
            }
        }
    }

    @Override
    default boolean intersects(HyperRegion x) {
        if (x == this) return true;
        //        for (int i = 0; i < d; i++)
        //            if (coord(false, i) > x.coord(true, i) ||
        //                    coord(true, i) < x.coord(false, i))
        //                return false;
        //        return true;
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return !((start() > t.end) || (end() < t.start));
        } else {
            TaskRegion t = (TaskRegion) x;
            if ((start() > t.end()) || (end() < t.start()))
                return false;
            if ((coord(false, 1) > t.coord(true, 1)) || (coord(true, 1) < t.coord(false, 1)))
                return false;
            return (!(coord(false, 2) > t.coord(true, 2))) && (!(coord(true, 2) < t.coord(false, 2)));
        }
    }

    @Override
    default boolean contains(HyperRegion x) {
        if (this == x) return false;

        //    default boolean contains(HyperRegion<X> x) {
        //        int d = dim();
        //        for (int i = 0; i < d; i++)
        //            if (coord(false, i) > x.coord(false, i) ||
        //                    coord(true, i) < x.coord(true, i))
        //                return false;
        //        return true;
        //    }
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return !((start() > t.start) || (end() < t.end));
        } else {
            TaskRegion t = (TaskRegion) x;
            if ((start() > t.start()) || (end() < t.end()))
                return false;
            if ((coord(false, 1) > t.coord(false, 1)) || (coord(true, 1) < t.coord(true, 1)))
                return false;
            return (!(coord(false, 2) > t.coord(false, 2))) && (!(coord(true, 2) < t.coord(true, 2)));
        }
    }

}
