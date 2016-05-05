package nars.util;

import nars.util.data.Util;

/**
 * Created by me on 5/4/16.
 */
public class RangeNormalizedFloat implements FloatSupplier {

    private final FloatSupplier in;
    private float min, max;
    private float minStart, maxStart;

    /** precision threshold */
    static final float epsilon = 0.001f;

    public RangeNormalizedFloat(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public RangeNormalizedFloat(FloatSupplier in, float minStart, float maxStart) {
        this.in = in;
        this.minStart = minStart;
        this.maxStart = maxStart;
        reset();
    }

    public void reset() {
        min = minStart;
        max = maxStart;
    }

    @Override
    public float asFloat() {
        float raw = in.asFloat();
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
}
