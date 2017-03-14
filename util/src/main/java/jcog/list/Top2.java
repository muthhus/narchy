package jcog.list;

import org.eclipse.collections.api.block.function.Function;

import java.util.List;

/**
 * Created by me on 3/14/17.
 */
public final class Top2<T> {

    public T a = null, b = null;
    public float aa = Float.NEGATIVE_INFINITY, bb = Float.NEGATIVE_INFINITY;

    public Top2(Function<T, Float> rank, List<T> from) {
        int s = from.size();
        assert (s > 1);
        for (int i = 0; i < s; i++) {
            T x = from.get(i);
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
}
