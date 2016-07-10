package nars.util.math;

import java.util.Arrays;

/**
 * simple delay line; shifts data on each access
 */
public class DelayedFloat implements FloatSupplier {

    final FloatSupplier input;

    private final float[] data;

    public DelayedFloat(FloatSupplier input, int history) {
        this.input = input;
        this.data = new float[history+1];
        Arrays.fill(data, input.asFloat()); //fill with initial value, HACK
    }

    protected boolean autoshift() {
        return true;
    }

    @Override
    public float asFloat() {
        float v = data[data.length-1];
        if (autoshift()) {
            next();
        }
        return v;
    }

    public void next() {
        System.arraycopy(data, 0, data, 1, data.length-1);
        data[0] = input.asFloat();
    }
}
