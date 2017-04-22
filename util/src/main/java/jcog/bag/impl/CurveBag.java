package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<V> extends ArrayBag<V> {

    public CurveBag(int initialCapacity, @NotNull PriMerge mergeFunction, @NotNull Map<V, PLink<V>> map) {
        super(mergeFunction, map);
        capacity(initialCapacity);
    }


    @NotNull
    @Override
    public Bag<V, PLink<V>> sample(@NotNull Bag.BagCursor<? super PLink<V>> each) {
        int size = size();
        if (size > 0)
            sample(each, ThreadLocalRandom.current().nextInt(size));
        return this;
    }

}