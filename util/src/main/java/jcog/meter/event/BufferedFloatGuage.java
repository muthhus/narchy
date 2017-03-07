package jcog.meter.event;

import jcog.math.RecycledSummaryStatistics;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import java.io.Serializable;

/** buffers the result to avoid returning an incomplete value */
public class BufferedFloatGuage implements FloatProcedure, Serializable {

    final RecycledSummaryStatistics data = new RecycledSummaryStatistics();
    float mean = 0, sum = 0;

    public final String id;

    public BufferedFloatGuage(String id) {
        super();
        this.id = id;
    }

    public float getSum() {
        return sum;
    }
    public float getMean() {
        return mean;
    }

    /** records current values and clears for a new cycle */
    public void clear() {
        mean = (float) data.getMean();
        sum = (float) data.getSum();
        data.clear();
    }

    public void accept(float v) {
        data.accept(v);
    }

    @Override
    public final void value(float v) {
        accept(v);
    }
}
