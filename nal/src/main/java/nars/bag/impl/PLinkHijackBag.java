package nars.bag.impl;

import jcog.bag.PLink;
import jcog.bag.impl.HijackBag;
import nars.attention.PForget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 2/17/17.
 */
public class PLinkHijackBag<X> extends HijackBag<X, PLink<X>> {

    public PLinkHijackBag(int initialCapacity, int reprobes, Random random) {
        super(initialCapacity, reprobes, random);
    }

    @Override
    public float pri(@NotNull PLink<X> key) {
        return key.pri();
    }

    @NotNull
    @Override
    public X key(PLink<X> value) {
        return value.get();
    }

    @Override
    protected float merge(@Nullable PLink<X> existing, @NotNull PLink<X> incoming, float scale) {
        if (existing == null) {
            incoming.priMult(scale);
            return incoming.priSafe(0);
        } else {
            float pBefore = existing.priSafe(0);
            existing.priAdd(incoming.priSafe(0) * scale);
            return existing.priSafe(0) - pBefore;
        }
    }

    @Override
    public PForget forget(float rate) {
        float memoryForget = 1f;
        return new PForget(rate * memoryForget);
    }
}
