package jcog.learn.gng.impl;


import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.util.MathArrays;

import static jcog.Texts.n4;

/**
 * Created by Scadgek on 11/3/2014.
 */
public class Centroid extends ArrayRealVector  {

    public final int id;
    private double localError;
    private double localDistanceSq; //caches square of last tested distance

    public Centroid(int id, int dimensions) {
        super(dimensions);
        this.id = id;
        this.localError = 0;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other) || ((Centroid)other).id == id;
    }

    @Override
    public int hashCode() {
        return (id+1)*37;
    }

    /** create a node from two existing nodes */
    public void set(Centroid maxErrorNode, Centroid maxErrorNeighbour) {
        double[] a = maxErrorNode.getDataRef();
        setLocalError(maxErrorNode.localError());
        double[] b = maxErrorNeighbour.getDataRef();
        double[] d= getDataRef();
        for (int i = 0; i < d.length; i++) {
            d[i] = (a[i] + b[i]) / 2;
        }
    }

    public Centroid randomizeUniform(int dim, double min, double max) {
        setEntry(dim, Math.random() * (max-min) + min);
        return this;
    }

    public Centroid randomizeUniform(double min, double max) {
        int dim = getDimension();
        for (int i = 0; i < dim; i++) {
            setEntry(i, Math.random() * (max-min) + min);
        }
        return this;
    }

//    public double[] getWeights() {
//        return weights;
//    }
//
//    public void setWeights(double[] weights) {
//        this.weights = weights;
//    }

    public double localError() {
        return localError;
    }

    public Centroid setLocalError(double localError) {
        this.localError = localError;
        return this;
    }

    public void mulLocalError(double alpha) {
        this.localError *= alpha;
    }

    public double distanceSq(final double[] x) {
        return distanceCartesian(getDataRef(), x);
    }

    public static double distanceManhattan(double[] x, double[] y) {
        return MathArrays.distance1(x, y);
    }

    public static double distanceCartesian(double[] x, double[] y) {
        double s = 0;
        int l = y.length;
        for (int i = 0; i < l; i++) {
            final double d = y[i] - x[i];
            s += d*d;
        }
        return s;
    }


    public double distance(final double[] x) {
        return Math.sqrt(distanceSq(x));
    }

    /** 0 < rate < 1.0 */
    public void update(final double rate, final double[] x) {
        final double[] d = getDataRef();
        final double ir = (1.0 - rate);
        for (int i = 0; i < d.length; i++) {
            d[i] =  (ir * d[i] ) + (rate * x[i]);
        }
    }

    public void add(final double[] x) {
        final double[] d = getDataRef();
        for (int i = 0; i < d.length; i++) {
            d[i] += x[i];
        }
    }

    @Override
    public String toString() {
        return id + ": <" + n4(getDataRef()) + "> lErr=" + n4(localError) + " dist=" + n4(localDistance());
    }

    public double learn(double[] x) {
        return (this.localDistanceSq = distanceSq(x));
    }

    public double localDistanceSq() {
        return localDistanceSq;
    }
    public double localDistance() { return Math.sqrt(localDistanceSq); }



    public double getDistanceSq(Centroid b) {
        return distanceSq(b.getDataRef());
    }

    public void updateLocalError(double winnerUpdateRate, double[] x) {
        setLocalError(localError() + localDistance());
        update(winnerUpdateRate, x);
    }


    //    public double distanceTo(double[] x) {
//        double retVal = 0;
//        for (int i = 0; i < x.length; i++) {
//            retVal += Math.pow(x[i] - weights[i], 2);
//        }
//        return Math.sqrt(retVal);
//    }
}