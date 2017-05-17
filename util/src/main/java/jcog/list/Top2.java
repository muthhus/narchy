package jcog.list;

import org.eclipse.collections.api.block.function.Function;

import java.util.function.Consumer;

/**
 * Created by me on 3/14/17.
 */
public final class Top2<T> implements Consumer<T> {

    private final Function<T, Float> rank;
    public T a, b;
    public float aa = Float.NEGATIVE_INFINITY, bb = Float.NEGATIVE_INFINITY;

    public Top2(Function<T, Float> rank) {
        this.rank = rank;
    }

    public Top2(Function<T, Float> rank, Iterable<T> from) {
        this(rank);
        from.forEach(this);
    }

    @Override
    public void accept(T x) {
        float xx = rank.apply(x);
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
