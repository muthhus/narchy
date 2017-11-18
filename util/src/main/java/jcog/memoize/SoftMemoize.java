package jcog.memoize;

import jcog.data.map.CustomConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/** WARNING dont call get() directly; use apply() */
public class SoftMemoize<X, Y> extends CustomConcurrentHashMap<X, Y> implements Memoize<X, Y> {

    private final Function<X, Y> f;

    public SoftMemoize(@NotNull Function<X, Y> f, int expSize, boolean softOrWeak) {
        super(STRONG, EQUALS, softOrWeak ? SOFT : WEAK, EQUALS, expSize);
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
