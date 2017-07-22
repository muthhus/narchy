package jcog.tensor;

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import jcog.Texts;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.Iterator;
import java.util.function.Supplier;

public interface Tensor extends Supplier<float[]> {

    default float[] get() {
        return snapshot();
    }

    float get(int... cell);

    float get(int linearCell);

    int index(int... cell);

    default void set(float newValue, int linearCell) {
        throw new UnsupportedOperationException("read-only");
    }

    default void set(float newValue, int... cell) {
        set(newValue, index(cell));
    }


    float[] snapshot();

    //void copyTo(float[] target, int targetOffset, int... subset);

    int[] shape();

//    //TODO
//    default Tensor noised(float noiseFactor, Random rng) {
//        return new
//    }
//    ..etc

    /**
     * hypervolume, ie total # cells
     */
    default int volume() {
        int[] s = shape();
        int v = s[0];
        for (int i = 1; i < s.length; i++)
            v *= s[i];
        return v;
    }

    /**
     * receives the pair: linearIndex,value (in increasing order)
     */
    default void forEach(IntFloatProcedure sequential) {
        get();
        forEach(sequential, 0, volume());
    }

    /**
     * receives the pair: linearIndex,value (in increasing order within provided subrange, end <= volume())
     */
    void forEach(IntFloatProcedure sequential, int start, int end);

    default void writeTo(float[] target) {
        forEach((i, v) -> {
            target[i] = v;
        });
    }

    default void writeTo(float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = v;
        });
    }

    default void writeTo(FloatToFloatFunction perElement, float[] target) {
        writeTo(perElement, target, 0);
    }

    default void writeTo(FloatToFloatFunction perElement, float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = perElement.valueOf(v);
        });
    }

    default void writeTo(FloatFloatToFloatFunction perElement, float[] target) {
        writeTo(perElement, target, 0);
    }

    default void writeTo(FloatFloatToFloatFunction perElement, float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = perElement.apply(target[i + offset], v);
        });
    }

    default FuncTensor apply(FloatToFloatFunction f) {
        return new FuncTensor(this, f);
    }

    default FuncTensor add(float v) {
        return apply((x) -> x + v);
    }

    default FuncTensor scale(float v) {
        return apply((x) -> x * v);
    }

    default float max() {
        final float[] max = {Float.MIN_VALUE};
        forEach((i, v) -> {
            if (max[0] < v)
                max[0] = v;
        });
        return max[0];
    }

    default float min() {
        final float[] min = {Float.MAX_VALUE};
        forEach((i, v) -> {
            if (min[0] > v)
                min[0] = v;
        });
        return min[0];
    }

    default float sum() {
        final float[] sum = {0};
        forEach((i,x) -> {
            sum[0] += x;
        });
        return sum[0];
    }

    default void add(Tensor pc) {
        int v = pc.volume();
        assert(v == volume());
        for (int i = 0; i < v; i++) {
            this.set( get(i) + pc.get(i), i );
        }
    }

    /** produces a string which is separated by tab characters (for .TSV) and each
     * value is rounded to 4 digits of decimal precision
     */
    default String tsv4() {
        return Joiner.on('\t').join(iterator(Texts::n4));
    }

    /** produces a string which is separated by tab characters (for .TSV) and each
     * value is rounded to 2 digits of decimal precision
     */
    default String tsv2() {
        return Joiner.on('\t').join(iterator(Texts::n2));
    }

    default <X> Iterator<X> iterator(FloatToObjectFunction<X> map) {
        return new AbstractIterator<X>() {
            int j = 0;
            final int limit = volume();
            @Override
            protected X computeNext() {
                if (j == limit) return endOfData();
                return map.valueOf(get(j++));
            }
        };
    }



//    int[] coord(int index, int[] coord);

//    default void forEachWithCoordinate(FloatObjectProcedure<int[]> coord ) {
//
//    }


}
