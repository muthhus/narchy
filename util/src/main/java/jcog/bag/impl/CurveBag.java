package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.Priority;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static jcog.Util.clamp;
import static jcog.Util.rng;

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