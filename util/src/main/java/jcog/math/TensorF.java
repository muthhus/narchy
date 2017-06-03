package jcog.math;

import org.jetbrains.annotations.Nullable;

/** float tensor */
public class TensorF {

    final float[] f;
    private final int[] dims;
    private final int[] stride;

    public TensorF(int... dims) {
        int size = 1;
        this.stride = new int[size-1];
        int striding = 1;
        for (int i = 0, dimsLength = dims.length; i < dimsLength; i++) {
            size *= dims[i];
            if (i < size-1)
                stride[i] = striding;
            striding*= dims[i];
        }
        this.dims = dims;

        f = new float[size];
    }

    public float get(int... cell) {
        return f[index(cell)];
    }

    public int index(int... i) {
        int f = 0;
        for (int s = 0, iLength = i.length; s < iLength; s++) {
            f += stride[s] * i[s];
        }
        return f;
    }

    public void copyTo(float[] target, int targetOffset, int... subset) {
        //TODO
    }

}
