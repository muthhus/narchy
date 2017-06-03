package jcog.math;

import jcog.Texts;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * float tensor
 */
public class TensorF {

    final float[] f;
    private final int[] shape;
    private final int[] stride;

    public TensorF(int... shape) {
        int size = shape[0];
        if (shape.length > 1) {
            this.stride = new int[size - 1];
            int striding = shape[0];
            for (int i = 1, dimsLength = shape.length; i < dimsLength; i++) {
                size *= shape[i];
                this.stride[i-1] = striding;
                striding *= shape[i];
            }
        } else {
            this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        }

        this.shape = shape; //TODO intern shape for fast compare
        this.f = new float[size];
    }

    @Override
    public String toString() {
        return Arrays.toString(shape) + "<" + Texts.n4(f) + ">";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (obj instanceof TensorF && (
                    Arrays.equals(f, ((TensorF)obj).f) &&
                    Arrays.equals(shape, ((TensorF)obj).shape)
                ));
    }

    public float get(int... cell) {
        return f[index(cell)];
    }

    public float get(int linearCell) {
        return f[linearCell];
    }

    public void set(float newValue, int linearCell) {
        f[linearCell] = newValue;
    }

    public void set(float newValue, int... cell) {
        f[index(cell)] = newValue;
    }

    public float[] snapshot() {
        return f.clone();
    }

    public int index(int... i) {
        int f = i[0];
        for (int s = 1, iLength = i.length; s < iLength; s++) {
            f += stride[s-1] * i[s];
        }
        return f;
    }

    public void copyTo(float[] target, int targetOffset, int... subset) {
        //TODO
    }

}
