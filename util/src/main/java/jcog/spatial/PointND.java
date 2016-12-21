package jcog.spatial;

import jcog.Util;

import java.util.Arrays;

/**
 * Created by me on 12/21/16.
 */
public class PointND implements HyperPoint {

    public final float[] coord;

    public PointND(PointND copy) {
        this(copy.coord.clone());
    }

    public PointND(float... coord) {
        this.coord = coord;
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
        PointND p = (PointND)h;
        float sumSq = 0;
        for (int i = 0; i < coord.length; i++) {
            float x = coord[i];
            float y = p.coord[i];
            sumSq += Util.sqr(x-y);
        }
        return Math.sqrt(sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return Math.abs( coord[i] - ((PointND)p).coord[i] );
    }

    @Override
    public boolean equals(Object obj) {
        //TODO use float epsilon tolerance
        if (this == obj) return true;
        return Arrays.equals(coord, ((PointND)obj).coord);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coord);
    }

    @Override
    public String toString() {
        return "(" + Arrays.toString(coord) + ")";
    }
}
