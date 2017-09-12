package nars.task.util;

import jcog.tree.rtree.HyperRegion;
import nars.task.Tasked;

public interface TaskRegion extends HyperRegion, Tasked {

    /**
     * relative to time sameness (1)
     */
    float FREQ_SAMENESS_IMPORTANCE = 0.2f;
    /**
     * relative to time sameness (1)
     */
    float CONF_SAMENESS_IMPORTANCE = 0.05f;

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    abstract long start();

    abstract long end();

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
        return 1 + d * timeCost() * FREQ_SAMENESS_IMPORTANCE;
    }

    default float confCost() {
        float d = (float) range(2);
        return 1 + d * timeCost() * CONF_SAMENESS_IMPORTANCE;
    }


    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        if (this == r || contains(r))
            return this;
        else {
            TaskRegion er = (TaskRegion) r;
            if (r.contains(this))
                return er;
            else return new TasksRegion(
                    Math.min(start(), er.start()), Math.max(end(), er.end()),
                    (float)Math.min(coord(false, 1), er.coord(false, 1)),
                        (float)Math.max(coord(true, 1), er.coord(true, 1)),
                    (float)Math.min(coord(false, 2), er.coord(false, 2)),
                        (float)Math.max(coord(true, 2), er.coord(true, 2))
            );
        }
    }

    @Override
    default public boolean intersects(HyperRegion x) {
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
            if ((coord(false,1) > t.coord(true,1)) || (coord(true,1) < t.coord(false,1)))
                return false;
            if ((coord(false,2)> t.coord(true,2)) || (coord(true,2) < t.coord(false, 2)))
                return false;
            return true;
        }
    }

    @Override
    default public  boolean contains(HyperRegion x) {
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
            if ((coord(false,1) > t.coord(false,1)) || (coord(true,1) < t.coord(true, 1)))
                return false;
            if ((coord(false,2) > t.coord(false, 2)) || (coord(true, 2) < t.coord(true,2)))
                return false;
            return true;
        }
    }

}
