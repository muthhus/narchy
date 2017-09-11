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
    static final float epsilon = Float.MIN_NORMAL;

    /** relaxation rate: brings min and max closer to each other in proportion to the value. if == 0, disables */
    private float relax = 0;


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

        float r = max - min;
        assert(r >= 0);
        if (r <= epsilon)
            return 0.5f;
        else
            return (raw - min) / r;
    }

    /**
     * decay rate = 1 means unaffected.  values less than 1 constantly
     * try to shrink the range to zero.
     * @param decayRate
     * @return
     */
    public FloatNormalized relax(float decayRate) {
        this.relax = decayRate;
        return this;
    }

    public FloatNormalized updateRange(float raw) {

        boolean incMin = false, incMax = false;

        if (min > raw) {
            min = raw;
        } else {
            incMin = true;
        }

        if (max < raw) {
            max = raw;
        } else {
            incMax = true;
        }

        if (relax > 0) {
            if (incMax)
                max = Util.clamp(max - (max - min * relax), min, max);
            if (incMin)
                min = Util.clamp(min + (max - min * relax), min, max);
        }

        return this;
    }


    /** whether a min/max range has been set or measured */
    public boolean ranged() {
        return Float.isFinite(min) && Float.isFinite(max);
    }
}
