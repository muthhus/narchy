package jcog.math.tensor;

public interface TensorTo {
    void set(float newValue, int linearCell);

    void set(float newValue, int... cell);
}
