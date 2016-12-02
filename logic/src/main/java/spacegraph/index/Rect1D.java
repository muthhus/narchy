package spacegraph.index;

import nars.util.math.Interval;

/**
 * Created by me on 12/2/16.
 */
public class Rect1D<X extends Comparable<X>> implements HyperRect<X> {

    final double from, to;


    public Rect1D(double from, double to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public HyperRect getMbr(HyperRect r) {
        Rect1D s = (Rect1D)r;
        double f = Math.min(from, s.from);
        double t = Math.max(to, s.to);
        return new Rect1D(f, t);
    }

    @Override
    public int getNDim() {
        return 1;
    }

    @Override
    public HyperPoint getMin() {
        return new Point1D(from);
    }

    @Override
    public HyperPoint getMax() {
        return new Point1D(to);
    }

    @Override
    public HyperPoint getCentroid() {
        return new Point1D((from+to)/2.0);
    }

    @Override
    public double getRange(int d) {
        return Math.abs(from-to);
    }

    @Override
    public boolean contains(HyperRect r) {
        Rect1D rr = (Rect1D)r;
        return rr.from >= from && rr.to <= to;
    }

    @Override
    public boolean intersects(HyperRect r) {
        Rect1D rr = (Rect1D)r;
        return (Math.max(from, rr.from) <= Math.min(to, rr.to));
    }

    @Override
    public double cost() {
        return getRange(0);
    }

    @Override
    public double perimeter() {
        return getRange(0);
    }
}
