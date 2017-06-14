package jcog.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.function.Function;

import static jcog.Texts.n2;

public class CaffeineMemoize<K,V> implements Memoize<K,V> {

    private final Cache<K, V> cache;
    private final Function<K, V> func;

    public CaffeineMemoize(Cache<K, V> cache, Function<K, V> compute) {
        this.cache = cache;
        this.func = compute;
    }

    public static <K,V> Memoize<K,V> build(Function<K,V> compute) {
        return new CaffeineMemoize(Caffeine.newBuilder()
                //.maximumSize(512*1024)
                .weakValues()
                //.softValues()
                .recordStats()
                .build(), compute);
    }

    @Override
    public V apply(K k) {
        return cache.get(k, func);
    }

    @Override
    public String summary() {
        CacheStats stats = cache.stats();
        return n2(stats.hitRate()*100f) + "% hits, " + cache.estimatedSize() + " size";
    }
}
