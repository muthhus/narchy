package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<K,V> extends HijackBag<K,PLink<Pair<K,V>>> implements Function<K,V> {

    float CACHE_HIT_BOOST;
    float CACHE_DENY_DAMAGE;

    final Function<K,V> func;

    public final AtomicLong hit = new AtomicLong(), miss = new AtomicLong(), evict = new AtomicLong();

    public HijackMemoize(int initialCapacity, int reprobes, Random random, @NotNull Function<K, V> f) {

        super(initialCapacity, reprobes);
        this.func = f;
    }

    public void statisticsReset() {
        hit.set(0);
        miss.set(0);
        evict.set(0);
    }


    @Override
    public int capacity() {
        int i = super.capacity();
        this.CACHE_HIT_BOOST = 1f/(1+i);
        this.CACHE_DENY_DAMAGE = -CACHE_HIT_BOOST/reprobes;
        return i;
    }

    @Override
    @Nullable public V apply(@NotNull K k) {
        PLink<Pair<K, V>> exists = get(k);
        if (exists!=null) {
            exists.priAdd(CACHE_HIT_BOOST);
            return exists.get().getTwo();
        }
        V v = func.apply(k);
        put(new RawPLink<>(Tuples.pair(k, v), CACHE_HIT_BOOST));
        return v;
    }

    @Override
    protected boolean replace(PLink<Pair<K, V>> incoming, PLink<Pair<K, V>> existing,Random random) {
        if (!super.replace(incoming, existing,random)) {
            existing.priSub(CACHE_DENY_DAMAGE);
            return true;
        }
        return false;
    }

    @Override
    public float pri(@NotNull PLink<Pair<K,V>> key) {
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
