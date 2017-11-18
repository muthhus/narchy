package jcog.memoize;

import jcog.data.map.MRUCache;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;



public class LinkedMRUMemoize<X, Y> extends MRUCache<X, Y> implements Memoize<X, Y> {

    private final Function<X, Y> f;

    public LinkedMRUMemoize(@NotNull Function<X, Y> f, int capacity) {
        super(capacity);
        this.f = f;
    }

    @Override
    public String summary() {
        return "size=" + super.size();
    }

    @Override
    public Y apply(X x) {
        return computeIfAbsent(x, f);
    }
}
