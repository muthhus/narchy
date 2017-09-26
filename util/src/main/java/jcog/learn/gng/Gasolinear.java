package jcog.learn.gng;

import com.google.common.primitives.Doubles;
import jcog.learn.gng.impl.Centroid;
import org.jetbrains.annotations.NotNull;

/** convenience class for discretizing points in a 1D (linear) space
 *  with growing neural gasnet
 *
 * */
public class Gasolinear extends NeuralGasNet<Gasolinear.Sorted1DCentroid> {


    boolean needsSort = true;
    private double min, max;

    public static class Sorted1DCentroid extends Centroid {
        public int order = -1;
        public Sorted1DCentroid(int id) {
            super(id, 1);
        }
    }


    public Gasolinear(int nodes) {
        this(nodes, 0, 1f);
    }

    public Gasolinear(int nodes, double min, double max) {
        super(1, nodes);
        this.min = min;
        this.max = max;
        for (int i = 0; i < nodes; i++) {
            centroids[i].setEntry(0, (max-min)*(i/(nodes-1f))+min);
        }
    }


    public static Gasolinear of(int nodes, double... points) {
        double min = Doubles.min(points);
        double max = Doubles.max(points);
        Gasolinear g = new Gasolinear(nodes, min, max);
        g.setWinnerUpdateRate(2f/points.length, 0.05f/points.length);
        for (double x : points)
            g.put(x /* 1D */);


        return g;
    }

    public int which(double x) {
        if (needsSort)
            sort();
        return put(x).order;
    }

    @Override
    public Sorted1DCentroid put(double... x) {
        Sorted1DCentroid y = super.put(x);
        needsSort = true;
        return y;
    }

    protected void sort() {
        Centroid[] l = centroids.clone();
        java.util.Arrays.sort(l, (a,b)-> Doubles.compare(a.getEntry(0), b.getEntry(0)));
        int i = 0;
        for (Centroid m : l) {
            ((Sorted1DCentroid)m).order = i++;
        }
    }

    @NotNull @Override public Gasolinear.Sorted1DCentroid newCentroid(int i, int dims) {
        return (Sorted1DCentroid) new Sorted1DCentroid(i).randomizeUniform(min, max);
    }

}
