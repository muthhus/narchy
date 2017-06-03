package jcog.tensor;

import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.function.Supplier;

import static java.lang.System.arraycopy;

public class BufferedTensor extends ArrayTensor {

    private final Tensor from;

    public BufferedTensor(Tensor from) {
        super(from.shape());
        this.from = from;
    }

    /** creates snapshot */
    public float[] get() {
        float[] x = from.get(); //trigger any updates
        arraycopy(x, 0, data, 0, x.length);
        return data;
    }

}
