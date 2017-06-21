package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X> extends ArrayBag<X> {

    public CurveBag(int initialCapacity, @NotNull PriMerge mergeFunction, @NotNull Map<X, PriReference<X>> map) {
        super(mergeFunction, map);
        capacity(initialCapacity);
    }



    /** optimized point sample impl */
    @Nullable
    @Override public PriReference<X> sample() {
        Object[] ii = items.array();
        if (ii.length == 0)
            return null;

        int size = Math.min(ii.length, this.size());
        if (size == 0)
            return null;

        if (size == 1)
            return (PriReference<X>) ii[0];

        for (int i = 0; i < size /* max # of trials */; i++) {
            Object n = ii[ThreadLocalRandom.current().nextInt(size)];
            if (n != null)
                return (PriReference<X>) n;
        }
        return null;
    }

    @NotNull
    @Override
    public Bag<X, PriReference<X>> sample(@NotNull Bag.BagCursor<? super PriReference<X>> each) {
        sample(each, -1, false);
        return this;
    }

}