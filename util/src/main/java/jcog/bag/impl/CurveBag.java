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
        PLink<X>[] i = items.array();
        if (i.length == 0)
            return null;

        return i[ThreadLocalRandom.current().nextInt(i.length)];
    }

    @NotNull
    @Override
    public Bag<X, PLink<X>> sample(@NotNull Bag.BagCursor<? super PLink<X>> each) {
        int size = size();
        if (size > 0)
            sample(each, ThreadLocalRandom.current().nextInt(size));
        return this;
    }

}