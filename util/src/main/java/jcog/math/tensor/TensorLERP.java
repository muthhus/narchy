package jcog.math.tensor;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.FloatFloatToFloatFunction;

/** rate between 0 and 1 */
public class TensorLERP extends TensorMerge implements FloatFloatToFloatFunction {

    private final FloatSupplier rate;
    private float currentRate;

    public TensorLERP(Tensor from, float rate) {
        this(from, ()->rate);
    }

    public TensorLERP(Tensor from, FloatSupplier rate) {
        super(from);
        this.rate = rate;
        this.currentRate = rate.asFloat();
    }

    @Override
    public float[] get() {
        currentRate = rate.asFloat();
        return super.get();
    }

    @Override
    public float apply(float current, float next) {
        return Util.lerp(currentRate, current, next);
    }

}
