package jcog.learn.gng;

import com.google.common.primitives.Doubles;
import jcog.learn.gng.impl.Node;
import org.jetbrains.annotations.NotNull;

/** convenience class for discretizing points in a 1D (linear) space
 *  with growing neural gasnet
 *
 * */
public class Gasolinear extends NeuralGasNet<Gasolinear.Sorted1DNode> {

    private final double min;
    private final double max;

    boolean needsSort = true;

    public class Sorted1DNode extends Node {
        public int order = -1;
        public Sorted1DNode(int id) {
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
    }

    public static Gasolinear of(int nodes, double... points) {
        double min = Doubles.min(points);
        double max = Doubles.max(points);
        Gasolinear g = new Gasolinear(nodes, min, max);
        g.setWinnerUpdateRate(4f/points.length, 0.05f/points.length);
        for (double x : points)
            g.put(x);
        return g;
    }

    public int which(double x) {
        if (needsSort)
            sort();
        return put(x).order;
    }

    @Override
    public Sorted1DNode put(double... x) {
        Sorted1DNode y = super.put(x);
        needsSort = true;
        return y;
    }

    protected void sort() {
        Node[] l = node.clone();
        java.util.Arrays.sort(l, (a,b)-> Doubles.compare(a.getEntry(0), b.getEntry(0)));
        int i = 0;
        for (Node m : l) {
            ((Sorted1DNode)m).order = i++;
        }
    }

    @NotNull @Override public Sorted1DNode newNode(int i, int dims) {
        return (Sorted1DNode) new Sorted1DNode(i).randomizeUniform(min, max);
    }

}
