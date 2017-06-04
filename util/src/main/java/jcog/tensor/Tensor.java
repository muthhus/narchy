package jcog.tensor;

import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Tensor extends Supplier<float[]> {


    float get(int... cell);

    float get(int linearCell);

    float[] snapshot();

    //void copyTo(float[] target, int targetOffset, int... subset);

    int[] shape();

//    //TODO
//    default Tensor noised(float noiseFactor, Random rng) {
//        return new
//    }
//    ..etc

    /** total # cells */
    default int volume() {
        int[] s = shape();
        int v = s[0];
        for (int i = 1; i < s.length; i++)
            v *= s[i];
        return v;
    }

    /** receives the pair: linearIndex,value (in increasing order) */
    default void forEach(IntFloatProcedure sequential) {
        forEach(sequential, 0, volume());
    }

    /** receives the pair: linearIndex,value (in increasing order within provided subrange, end <= volume()) */
    void forEach(IntFloatProcedure  sequential, int start, int end);

    default void writeTo(float[] target) {
        forEach((i,v)->{
           target[i] = v;
        });
    }

    default void writeTo(float[] target, int offset) {
        forEach((i,v)->{
           target[i + offset] = v;
        });
    }

//    int[] coord(int index, int[] coord);

//    default void forEachWithCoordinate(FloatObjectProcedure<int[]> coord ) {
//
//    }


}
