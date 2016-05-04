package nars.util;

/**
 * Created by me on 5/4/16.
 */
public class RangeNormalizedFloat implements FloatSupplier {

    private final FloatSupplier in;
    private float min, max;


    public RangeNormalizedFloat(FloatSupplier in) {
        this.in = in;
    }

    public void reset() {
        min = Float.POSITIVE_INFINITY;
        max = Float.NEGATIVE_INFINITY;
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
