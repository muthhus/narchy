package spacegraph.index;

/**
 * Created by me on 12/2/16.
 */
@Deprecated public class Point1D implements HyperPoint {
    public final double x;

    public Point1D(double X) {
        this.x = X;
    }

    @Override
    public int getNDim() {
        return 1;
    }

    @Override
    public Double getCoord(int d) {
        return x;
    }

    @Override
    public double distance(HyperPoint p) {
        return Math.abs(x - ((Point1D)p).x);
    }

    @Override
    public double distance(HyperPoint p, int d) {
        return distance(p);
    }
}
