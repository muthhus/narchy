package jcog.bag.impl;

import jcog.bag.PLink;
import jcog.bag.RawPLink;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 3/29/17.
 */
public class HijackMemoize<K,V> extends HijackBag<K,PLink<Pair<K,V>>> implements Function<K,V> {

    final static float INITIAL_PRI = 0.5f;
    final static float CACHE_HIT_PRI = 0.1f;
    final static float sustainRate = 0.99f;
    final Function<K,V> func;

    public HijackMemoize(int initialCapacity, int reprobes, Random random, @NotNull Function<K, V> f) {

        super(initialCapacity, reprobes, random);
        this.func = f;
    }

    @Override
    @Nullable public V apply(@NotNull K k) {
        PLink<Pair<K, V>> exists = get(k);
        if (exists!=null) {
            exists.priAdd(CACHE_HIT_PRI);
            return exists.get().getTwo();
        }
        V v = func.apply(k);
        put(new RawPLink<>(Tuples.pair(k, v), INITIAL_PRI));
        return v;
    }

    @Override
    public float pri(@NotNull PLink<Pair<K,V>> key) {
        key.priMult(sustainRate);
        return key.pri();
    }

    @NotNull
    @Override
    public K key(PLink<Pair<K,V>> value) {
        return value.get().getOne();
    }

    @Override
    protected float merge(@Nullable PLink<Pair<K, V>> existing, @NotNull PLink<Pair<K, V>> incoming, float scale) {
        return 0;
    }

    @Override
    protected Consumer<PLink<Pair<K, V>>> forget(float rate) {
        return null;
    }


}
