package jcog.math;

public class FloatHighPass implements FloatSupplier {
    float prev;
    final FloatSupplier in;
    private float factor = 0.1f;

    public FloatHighPass(FloatSupplier in) {
        this.in = in;
    }

    @Override
    public float asFloat() {
        float x = in.asFloat();
        if (x!=x) return Float.NaN; //pass-through

        float f = this.factor;
        prev = x * f + this.prev * (1.0f - f);
        return x - prev;
    }
}
