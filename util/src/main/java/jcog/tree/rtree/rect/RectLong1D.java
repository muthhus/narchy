package jcog.tree.rtree.rect;

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.point.Long1D;

/**
 * Created by me on 12/2/16.
 */
public class RectLong1D implements HyperRect<Long1D> {

    public final long from, to;

    /** point */
    public RectLong1D(long f) {
        this(f, f);
    }

    /** range */
    public RectLong1D(long f, long t) {
        this.from = f;
        this.to = t;
    }

    @Override
    public HyperRect<Long1D> mbr(HyperRect<Long1D> r) {

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
    public Long1D min() {
        return new Long1D(from);
    }

    @Override
    public Long1D max() {
        return new Long1D(to);
    }

    @Override
    public Long1D center() {
        return new Long1D((from + to)/2 );
    }

    @Override
    public double center(int d) {
        assert(d==0);
        return (from + to) / 2.0;
    }

    @Override
    public double getRange(int d) {
        assert(d==0);
        return Math.abs(from - to);
    }

    @Override
    public boolean contains(HyperRect<Long1D> r) {
        RectLong1D inner = (RectLong1D) r;
        return inner.from >= from && inner.to <= to;
    }

    @Override
    public boolean intersects(HyperRect<Long1D> r) {
        RectLong1D rr = (RectLong1D) r;
        return (Math.max(from, rr.from) <= Math.min(to, rr.to));
    }

    @Override
    public double cost() {
        return getRange(0);
    }


}
