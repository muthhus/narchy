package nars.task.util;

import jcog.tree.rtree.HyperRegion;

/**
 * only valid for comparison during rtree iteration
 */
public class TimeRange implements HyperRegion {

    long start, end;

    public TimeRange() {

    }

    public TimeRange(long s, long e) {
        set(s, e);
    }

    public TimeRange set(long s, long e) {
        this.start = s;
        this.end = e;
        return this;
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int dim() {
        return 3;
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
//            switch (dimension) {
//                case 0: return maxOrMin ? end : start;
//            }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(HyperRegion x) {
//            TaskRegion t = (TaskRegion)x;
//            return !((start > t.start) || (end < t.end));
        throw new UnsupportedOperationException();
    }
}
