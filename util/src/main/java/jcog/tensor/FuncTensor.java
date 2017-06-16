package jcog.tensor;

import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

public class FuncTensor extends BatchArrayTensor {

    private final FloatToFloatFunction func;
    private final Tensor from;

    public FuncTensor(Tensor from, FloatToFloatFunction func) {
        super(from.shape());
        this.from = from;
        this.func = func;
    }


    @Override public void update() {
        from.get();
        from.writeTo(func, data);//trigger any updates but using the iterator HACK, not:
    }
}
