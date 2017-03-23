package jcog.bag.impl;

import jcog.bag.PForget;
import jcog.bag.PLink;
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
        incoming.priMult(scale);
        float pAdd = incoming.priSafe(0);
        if (existing != null) {
            existing.priAdd(pAdd);
        }
        return pAdd;
    }

    @Override
    protected float priEpsilon() {
        return PLink.EPSILON_DEFAULT;
    }

    @Override
    protected float temperature() {
        return 0.5f;
    }

    @Override
    public PForget forget(float rate) {
        float memoryForget = 1f;
        return new PForget(rate * memoryForget);
    }
}
