package jcog.math;

import jcog.Util;

import java.util.function.DoubleSupplier;


public class FloatNormalized implements FloatSupplier {

    private final FloatSupplier in;
    protected float min;
    protected float max;
    private final float minStart;
    private final float maxStart;

    /** precision threshold */
    static final float epsilon = 0.01f;
    private float decay = 1f;


    public FloatNormalized(DoubleSupplier in) {
        this(()->(float)in.getAsDouble());
    }
    public FloatNormalized(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart) {
        this.in = in;
        this.minStart = minStart;
        this.maxStart = maxStart;
        reset();
    }

    @Override
    public String toString() {
        return "RangeNormalizedFloat{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public void reset() {
        min = minStart;
        max = maxStart;
    }

    @Override
    public float asFloat() {
        float raw = in.asFloat();
        return raw != raw ? Float.NaN : normalize(raw);
    }

    public float normalize(float raw) {
//        if (!Float.isFinite(raw))
//            //throw new ArithmeticException();
//            return 0.5f;

        updateRange(raw);

        if (Util.equals(min,max,epsilon))
            return 0.5f;
        else
            return (raw - min) / (max - min);
    }

    /**
     * decay rate = 1 means unaffected.  values less than 1 constantly
     * try to shrink the range to zero.
     * @param decayRate
     * @return
     */
    public FloatNormalized relax(float decayRate) {
        this.decay = decayRate;
        return this;
    }

    public FloatNormalized updateRange(float raw) {
        if (min > raw)
            min = raw;
        else
            min *= decay;

        if (max < raw)
            max = raw;
        else
            max *= decay;

        return this;
    }


    /** whether a min/max range has been set or measured */
    public boolean ranged() {
        return Float.isFinite(min) && Float.isFinite(max);
    }
}
