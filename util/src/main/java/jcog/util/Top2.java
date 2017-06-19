package jcog.util;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Consumer;

/**
 * Created by me on 3/14/17.
 */
public final class Top2<T> implements Consumer<T> {

    private final FloatFunction<T> rank;
    public T a, b;
    public float aa = Float.NEGATIVE_INFINITY, bb = Float.NEGATIVE_INFINITY;

    public Top2(FloatFunction<T> rank) {
        this.rank = rank;
    }

    public Top2(FloatFunction<T> rank, Iterable<T> from) {
        this(rank);
        from.forEach(this);
    }

    @Override
    public void accept(T x) {
        float xx = rank.floatValueOf(x);
        if (xx > aa) {
            b = a;
            bb = aa; //shift down
            a = x;
            aa = xx;
        } else if (xx > bb) {
            b = x;
            bb = xx;
        }
    }

}
