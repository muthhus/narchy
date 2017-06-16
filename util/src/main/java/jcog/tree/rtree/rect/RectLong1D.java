package jcog.tree.rtree.rect;

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.point.Long1D;

/**
 * Created by me on 12/2/16.
 */
public class RectLong1D implements HyperRect<Long1D> {

    public final long min, max;

    /** point */
    public RectLong1D(long f) {
        this(f, f);
    }

    /** range */
    public RectLong1D(long f, long t) {
        this.min = f;
        this.max = t;
    }

    @Override
    public HyperRect<Long1D> mbr(HyperRect<Long1D>[] rect) {
        int n = rect.length;
        assert(n > 0);
        if (n == 1)
            return rect[0];
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (HyperRect h : rect) {
            if (h == null)
                break;
            RectLong1D r = (RectLong1D) h;
            long rmin = r.min;
            if (rmin < min) min = rmin;
            long rmax = r.max;
            if (rmax > max) max = rmax;
        }
        return new RectLong1D(min, max);
    }

    @Override
    public HyperRect<Long1D> mbr(HyperRect<Long1D> r) {

        RectLong1D s = (RectLong1D) r;
        long f = Math.min(min, s.min);
        long t = Math.max(max, s.max);
        return new RectLong1D(f, t);
    }

    @Override
    public int dim() {
        return 1;
    }

    @Override
    public Long1D min() {
        return new Long1D(min);
    }

    @Override
    public Long1D max() {
        return new Long1D(max);
    }

    @Override
    public Long1D center() {
        return new Long1D((min + max)/2 );
    }

    @Override
    public double center(int d) {
        assert(d==0);
        return (min + max) / 2.0;
    }

    @Override
    public double getRange(int d) {
        assert(d==0);
        return Math.abs(min - max);
    }

    @Override
    public boolean contains(HyperRect<Long1D> r) {
        RectLong1D inner = (RectLong1D) r;
        return inner.min >= min && inner.max <= max;
    }

    @Override
    public boolean intersects(HyperRect<Long1D> r) {
        RectLong1D rr = (RectLong1D) r;
        return (Math.max(min, rr.min) <= Math.min(max, rr.max));
    }

    @Override
    public double cost() {
        return getRange(0);
    }


}
