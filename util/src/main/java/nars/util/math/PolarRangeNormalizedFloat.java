package nars.util.math;

/** balances at zero, normalizes positive and negative ranges separately */
public class PolarRangeNormalizedFloat implements FloatSupplier {

    final RangeNormalizedFloat positive;
    final RangeNormalizedFloat negative;
    private final FloatSupplier in;

    /** defines a deadzone around zero TODO auto-tune this based on the relative positive/negative dynamic ranges */
    @Deprecated float nonpolarity = 0.5f;

    public PolarRangeNormalizedFloat(FloatSupplier in) {
        this.in = in;
        positive = new RangeNormalizedFloat(null);
        negative = new RangeNormalizedFloat(null);
    }

    public void reset() {
        positive.reset();
        negative.reset();
    }

    @Override
    public float asFloat() {
        float v = in.asFloat();
        float w;

        if (v < 0) {
            w = -expand(negative.normalize(-v), nonpolarity);
        } else if (v > 0) {
            w = expand(positive.normalize(v), nonpolarity);
        } else {
            w = 0;
        }

        return w/2f + 0.5f;
    }

    public float expand(float v, float nonpolarity) {
        return v*(1f-nonpolarity) + nonpolarity;
    }
}
