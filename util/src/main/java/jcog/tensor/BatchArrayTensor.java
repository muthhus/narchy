package jcog.tensor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * disallows non-batch accessor methods
 */
abstract public class BatchArrayTensor extends ArrayTensor {

    final AtomicBoolean busy = new AtomicBoolean(false);

    public BatchArrayTensor(int[] shape) {
        super(shape);
    }

    @Override
    public void set(float v, int cell) {
        throw new UnsupportedOperationException("only batch operations available");
    }

    @Override
    public float get(int cell) {
        throw new UnsupportedOperationException("only batch operations available");
    }
    @Override public float[] get() {
        //TODO maybe synchronize to be sure
        if (busy.compareAndSet(false, true)) {
            try {
                update();
            } finally {
                busy.set(false);
            }
        }
        return data;
    }

    protected abstract void update();
}
