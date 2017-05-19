package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X> extends ArrayBag<X> {

    public CurveBag(int initialCapacity, @NotNull PriMerge mergeFunction, @NotNull Map<X, PLink<X>> map) {
        super(mergeFunction, map);
        capacity(initialCapacity);
    }



    /** optimized point sample impl */
    @Nullable
    @Override public PLink<X> sample() {
        Object[] i = items.array();
        if (i.length == 0)
            return null;

        return (PLink<X>) i[ThreadLocalRandom.current().nextInt(i.length)];
    }

    @NotNull
    @Override
    public Bag<X, PLink<X>> sample(@NotNull Bag.BagCursor<? super PLink<X>> each) {
        sample(each, -1, false);
        return this;
    }

}