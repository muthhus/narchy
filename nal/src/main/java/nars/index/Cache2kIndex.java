package nars.index;

import nars.nar.util.DefaultConceptBuilder;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.event.CacheEntryExpiredListener;
import org.cache2k.event.CacheEntryOperationListener;
import org.cache2k.event.CacheEntryRemovedListener;
import org.cache2k.integration.AdvancedCacheLoader;
import org.cache2k.integration.CacheLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * http://cache2k.org/#Integrating_cache2k_in_your_project
 */
public class Cache2kIndex extends MaplikeIndex  {

    final Cache data;

    public Cache2kIndex(long cap, Random rng) {
        super(new DefaultConceptBuilder(rng));

        data = Cache2kBuilder.forUnknownTypes().
                eternal(true).storeByReference(true).
                entryCapacity(cap).
                //keepDataAfterExpired(true).
                loader(new CacheLoader() {
                    @Override
                    public Object load(Object o) throws Exception {
                        return null;
                    }
                }).


//                addListener(new CacheEntryExpiredListener() {
//
//                    @Override
//                    public void onEntryExpired(Cache cache, CacheEntry cacheEntry) {
//                        System.err.println("expired: " + cacheEntry);
//                    }
//                }).
                //addListener(this).
                build();


    }

    @Override
    public @NotNull String summary() {
        return size() + " concepts+subterms";
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public int size() {
        return data.getTotalEntryCount();
    }

    @Override
    public int subtermsCount() {
        return 0;
    }

    @Override
    public Termed remove(Termed entry) {
        return (Termed) data.peekAndRemove(entry);
    }

    @Override
    public @Nullable Termed get(@NotNull Termed x) {
        return (Termed) data.get(x);
    }


    @Override
    public @Nullable void set(@NotNull Termed src, Termed target) {
        data.put(src, target);
    }

    @Override
    protected TermContainer putIfAbsent(TermContainer src, TermContainer target) {
        if (data.putIfAbsent(src, target)) {
            return target;
        }
        return (TermContainer) data.get(src); //HACK
    }

    @Override
    public void forEach(Consumer<? super Termed> cc) {
        StreamSupport.stream(data.spliterator(), false).filter(x -> x instanceof Termed).
                forEach(x -> cc.accept((Termed)x));
    }

//    @Override
//    public void onEntryExpired(Cache cache, CacheEntry cacheEntry) {
//        System.out.println("expire: " + cacheEntry);
//    }
//
//    @Override
//    public void onEntryRemoved(Cache cache, CacheEntry cacheEntry) {
//        System.out.println("removed: " + cacheEntry);
//    }
}
