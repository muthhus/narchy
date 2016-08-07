package nars.util.math;

import java.util.function.LongSupplier;


public class FirstOrderDifferenceFloat implements FloatSupplier {

    final FloatSupplier in;

    float lastValue = Float.NaN;
    private long lastUpdate;
    private final LongSupplier clock;

    public FirstOrderDifferenceFloat(LongSupplier clock, FloatSupplier in) {
        this.in = in;
        this.clock = clock;

        this.lastUpdate = clock.getAsLong();
    }

    @Override
    public float asFloat() {

        long now = clock.getAsLong();
        float currentValue = in.asFloat();

        float result;
        if (lastValue==lastValue) {
            result = currentValue - lastValue;
        } else {
            result = 0;
        }

        if (now!=lastUpdate) {
            lastUpdate = now;
            lastValue = currentValue;
        }


        return result;
    }
}
