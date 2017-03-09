package jcog.rtree.rect;

import jcog.rtree.HyperPoint;
import jcog.rtree.HyperRect;
import jcog.rtree.point.Double1D;
import jcog.rtree.point.Long1D;

/**
 * Created by me on 12/2/16.
 */
public class RectLong1D implements HyperRect<Long1D> {

    public final long from, to;

    public RectLong1D(long f, long t) {
        this.from = f;
        this.to = t;
    }

    @Override
    public HyperRect mbr(HyperRect r) {

        RectLong1D s = (RectLong1D) r;
        long f = Math.min(from, s.from);
        long t = Math.max(to, s.to);
        return new RectLong1D(f, t);
    }

    @Override
    public int dim() {
        return 1;
    }

    @Override
    public HyperPoint min() {
        return new Long1D(from);
    }

    @Override
    public HyperPoint max() {
        return new Long1D(to);
    }

    @Override
    public HyperPoint center() {
        return new Long1D((from + to)/2 );
    }

    @Override
    public double center(int d) {
        assert (d == 0);
        return (from + to) / 2.0;
    }

    @Override
    public double getRange(int d) {
        return Math.abs(from - to);
    }

    @Override
    public boolean contains(HyperRect r) {
        RectLong1D inner = (RectLong1D) r;
        return inner.from >= from && inner.to <= to;
    }

    @Override
    public boolean intersects(HyperRect r) {
        RectLong1D rr = (RectLong1D) r;
        return (Math.max(from, rr.from) <= Math.min(to, rr.to));
    }

    @Override
    public double cost() {
        return getRange(0);
    }





}
