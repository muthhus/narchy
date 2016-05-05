package nars.util;

/**
 * Created by me on 5/4/16.
 */
public class RangeNormalizedFloat implements FloatSupplier {

    private final FloatSupplier in;
    private float min, max;
    private float minStart, maxStart;


    public RangeNormalizedFloat(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public RangeNormalizedFloat(FloatSupplier in, float minStart, float maxStart) {
        this.in = in;
        this.minStart = minStart;
        this.minStart = maxStart;
        reset();
    }

    public void reset() {
        min = minStart;
        max = maxStart;
    }

    @Override
    public float asFloat() {
        float raw = in.asFloat();
        if (min > raw)
            min = raw;
        if (max < raw)
            max = raw;

        return (raw - min) / (max - min);
    }
}
