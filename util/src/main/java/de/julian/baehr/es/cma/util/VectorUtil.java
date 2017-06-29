package de.julian.baehr.es.cma.util;

import jcog.tensor.ArrayTensor;
import jcog.tensor.FuncTensor;
import jcog.tensor.Tensor;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.Arrays;
import java.util.Random;


public class VectorUtil {

    public static Tensor vectorFromTo(int start, int end) {

        return vectorFromToBy(start, end, 1);
    }

    public static Tensor vectorFromToBy(int start, int end, int steps) {

        int elements = (end - start + steps) / steps;
        float[] values = new float[elements];

        for (int i = 0; i < elements; i++)
            values[i] = start + i * steps;

        return new ArrayTensor((float[])values);
    }

    public static Tensor empty(int dimension) {
        return vectorOf(0, dimension);
    }

    public static Tensor vectorOf(float value, int dimension) {
        float[] values = new float[dimension];
        Arrays.fill(values, value);
        return new ArrayTensor(values);
    }

    public static Tensor forEach(Tensor vector, FloatToFloatFunction operator) {
        return new FuncTensor(vector, operator);
    }

//    private static float[] forEach(float[] values, FloatToFloatFunction operator) {
//
//        for (int i = 0; i < values.length; i++)
//            values[i] = operator.valueOf(values[i]);
//
//        return values;
//    }

    public static Tensor logEach(Tensor vector) {
        return forEach(vector, d -> (float)Math.log(d));
    }

    public static Tensor sqrtEach(Tensor vector) {
        return forEach(vector, d -> (float)Math.sqrt(d));
    }

    public static Tensor powEach(Tensor vector, double power) {
        return forEach(vector, d -> (float)Math.pow(d, power));
    }

    public static float[] copyVectorValues(Tensor vector) {
        return vector.snapshot();
    }

    public static Tensor scale(Tensor vector, Tensor scale) {

        float[] values = copyVectorValues(vector);

        for (int i = 0; i < values.length; i++)
            values[i] *= scale.get(i);

        return new ArrayTensor(values);
    }

    public static Tensor normalize(Tensor vector) {
        return vector.scale(1f / sum(vector));
    }

    public static float sum(Tensor vector) {
        return vector.sum();
    }

    public static Tensor randomVector(int dimension, float min, float max) {
        final Random random = new Random();
        return forEach(new ArrayTensor(new float[dimension]),
                        d -> (float)random.nextDouble() * (max - min) + min);
    }

    public static Tensor randomVectorGauss(int dimension, float mean, float standardDeviation) {
        final Random random = new Random();
        return forEach(new ArrayTensor(dimension),
                        d -> (float)random.nextGaussian() * standardDeviation + mean);
    }

}
