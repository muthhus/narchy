package jcog.math;

import java.util.Arrays;

/**
 * simple delay line; shifts data on each access
 */
public class DelayedFloat implements FloatSupplier {

    final FloatSupplier input;

    public final float[] data;

    public DelayedFloat(FloatSupplier input, int history) {
        assert(history > 0);
        this.input = input;
        this.data = new float[history];
        Arrays.fill(data, input.asFloat()); //fill with initial value, HACK
    }


    @Override
    public float asFloat() {
        float v = data[data.length-1];
        return v;
    }

    public void next() {
        System.arraycopy(data, 0, data, 1, data.length-1);
        data[0] = input.asFloat();
    }
}
