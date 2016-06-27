package nars.util.math;

import nars.util.Util;


public class RangeNormalizedFloat implements FloatSupplier {

    private final FloatSupplier in;
    private float min, max;
    private final float minStart;
    private final float maxStart;

    /** precision threshold */
    static final float epsilon = 0.01f;

    public RangeNormalizedFloat(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public RangeNormalizedFloat(FloatSupplier in, float minStart, float maxStart) {
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
        return normalize(raw);
    }

    public float normalize(float raw) {
        if (!Float.isFinite(raw))
            //throw new ArithmeticException();
            return 0.5f;

        if (min > raw)
            min = raw;
        if (max < raw)
            max = raw;

        if (Util.equals(min,max,epsilon))
            return 0.5f;

        return (raw - min) / (max - min);
    }


    /** whether a min/max range has been set or measured */
    public boolean ranged() {
        return Float.isFinite(min) && Float.isFinite(max);
    }
}
