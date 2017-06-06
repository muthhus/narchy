package jcog.tensor;

public class BufferedTensor extends ArrayTensor {

    private final Tensor from;

    public BufferedTensor(Tensor from) {
        super(from.shape());
        this.from = from;
    }

    /** creates snapshot */
    public float[] get() {
        /*float[] x = */from.get();
        from.writeTo(data);//trigger any updates but using the iterator HACK, not:
            //arraycopy(x, 0, data, 0, x.length);
        return data;
    }

}
