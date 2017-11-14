package jcog.math.tensor;

public class BufferedTensor extends BatchArrayTensor {

    protected final Tensor from;

    public BufferedTensor(Tensor from) {
        super(from.shape());
        this.from = from;
    }

    @Override protected void update() {
        from.get();
        from.writeTo(data);//trigger any updates but using the iterator HACK, not:
    }

}
