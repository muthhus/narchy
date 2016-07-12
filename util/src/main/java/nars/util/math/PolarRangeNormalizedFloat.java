package nars.util.math;

/** balances at zero, balanced normalization of positive and negative ranges (radius)
 *  output is normalized to range 0..1.0
 * */
public class PolarRangeNormalizedFloat extends RangeNormalizedFloat {

    public PolarRangeNormalizedFloat(FloatSupplier in) {
        super(in);
        min = 0; //origin
    }

    @Override
    public float normalize(float raw) {
        float araw = Math.abs(raw);
        float n = super.normalize(araw);
        if (raw < 0) n = -n;
        return n/2f + 0.5f;
    }

    public PolarRangeNormalizedFloat radius(float radius) {
        this.min = 0;
        this.max = radius;
        return this;
    }

    //    final RangeNormalizedFloat positive;
//    final RangeNormalizedFloat negative;
//    private final FloatSupplier in;
//
//
//    public PolarRangeNormalizedFloat(FloatSupplier in) {
//        this.in = in;
//        positive = new RangeNormalizedFloat(null);
//        negative = new RangeNormalizedFloat(null);
//    }
//
//    public void reset() {
//        positive.reset();
//        negative.reset();
//    }
//
//    @Override
//    public float asFloat() {
//
//        float v = in.asFloat();
//        if (!Float.isFinite(v))
//            return Float.NaN;
//
//        float w;
//
//        float nonpolarity =
//                (positive.ranged() && negative.ranged()) ?
//                Math.abs(positive.min() - negative.min()) / Math.abs(positive.max() + negative.max()) :
//                0
//                ;
//
//        if (!Float.isFinite(nonpolarity))
//            nonpolarity = 0; //HACK
//
//        if (v < 0) {
//            w = -expand(negative.normalize(-v), nonpolarity);
//        } else if (v > 0) {
//            w = expand(positive.normalize(v), nonpolarity);
//        } else {
//            //TODO within threshold
//            //normalize to set range, but return 0 regardless
//            positive.normalize(0);
//            negative.normalize(0);
//            w = 0;
//        }
//
//
//        if (!Float.isFinite(w))
//            throw new MathArithmeticException();
//
//        return w;
//    }
//
//    public float expand(float v, float nonpolarity) {
//        return v*(1f-nonpolarity) + nonpolarity;
//    }
}
