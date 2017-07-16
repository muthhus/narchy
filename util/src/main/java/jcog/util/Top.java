package jcog.util;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Top<T> implements Consumer<T> {
    private final FloatFunction<T> rank;
    public T the;
    public float score = Float.NEGATIVE_INFINITY;

    public Top(FloatFunction<T> rank) {
        this.rank = rank;
    }

    @Override
    public void accept(T x) {
        float xs = rank.floatValueOf(x);
        if (xs > score) {
            the = x;
            score = xs;
        }
    }

    @NotNull
    public List<T> toList() {
        if (the == null) return Collections.emptyList();
        else return Collections.singletonList(the);
    }

    public Top<T> of(Iterator<T> iterator) {
        iterator.forEachRemaining(this);
        return this;
    }

}
