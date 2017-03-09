package jcog.tree.rtree.point;


import jcog.tree.rtree.HyperPoint;
import jcog.tree.rtree.RTree;

import java.io.Serializable;
import java.util.Arrays;

import static jcog.tree.rtree.RTree.FPSILON;


/**
 * Created by me on 12/21/16.
 */
public class FloatND implements HyperPoint, Serializable {

    public final float[] coord;

    public FloatND(FloatND copy) {
        this(copy.coord.clone());
    }

    public FloatND(float... coord) {
        this.coord = coord;
    }

    public static FloatND fill(int dims, float value) {
        float[] a = new float[dims];
        Arrays.fill(a, value);
        return new FloatND(a);
    }

    @Override
    public int dim() {
        return coord.length;
    }

    @Override
    public Float coord(int d) {
        return coord[d];
    }

    @Override
    public double distance(HyperPoint h) {
        FloatND p = (FloatND) h;
        float sumSq = 0;
        for (int i = 0; i < coord.length; i++) {
            float x = coord[i];
            float y = p.coord[i];
            float xMinY = x - y;
            sumSq += xMinY * xMinY;
        }
        return Math.sqrt(sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return Math.abs(coord[i] - ((FloatND) p).coord[i]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        FloatND p = (FloatND) obj;
        return RTree.equals(coord, p.coord, FPSILON);
    }

    @Override
    public int hashCode() {
        //TODO compute each component rounded to nearest epsilon?
        return Arrays.hashCode(coord);
    }

    @Override
    public String toString() {
        return '(' + Arrays.toString(coord) + ')';
    }


}
