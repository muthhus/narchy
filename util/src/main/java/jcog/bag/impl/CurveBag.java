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
        Object[] i = items.array();
        if (i.length == 0)
            return null;

        return (PriReference<X>) i[ThreadLocalRandom.current().nextInt(i.length)];
    }

    @NotNull
    @Override
    public Bag<X, PriReference<X>> sample(@NotNull Bag.BagCursor<? super PriReference<X>> each) {
        sample(each, -1, false);
        return this;
    }

}