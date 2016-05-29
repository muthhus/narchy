package nars.index;

import com.google.common.base.Joiner;
import nars.concept.Concept;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 5/28/16.
 */
public class InfinispanIndex extends MaplikeIndex {

    private final Cache<Termed, Termed> concepts;
    private final Cache<TermContainer, TermContainer> subterms;


    public InfinispanIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super(termBuilder, conceptBuilder);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.unsafe().versioning().disable();
        cb.locking().concurrencyLevel(1);
        cb.jmxStatistics().disable();
        //cb.customInterceptors().addInterceptor();



        DefaultCacheManager cm = new DefaultCacheManager(cb.build());
        //DefaultCacheManager cm = new DefaultCacheManager();
        System.out.println(Joiner.on('\n').join(cm.getCacheManagerConfiguration().toString().split(", ")));

        this.concepts = cm.getCache("concepts");
        this.subterms = cm.getCache("subterms");

    }

    @Override
    public Termed remove(Termed entry) {
        return concepts.remove(entry);
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return concepts.get(x);
    }

    @Override
    public @Nullable Termed set(@NotNull Termed src, Termed target) {
        /*
        3.5.1. DecoratedCache

Another approach would be to use the DecoratedCache wrapper. This allows you to reuse flags. For example:

AdvancedCache cache = ...
DecoratedCache strictlyLocal = new DecoratedCache(cache, Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_STORE);
strictlyLocal.put("local_1", "only");
strictlyLocal.put("local_2", "only");
strictlyLocal.put("local_3", "only");
         */
        concepts.getAdvancedCache()
                .withFlags(Flag.IGNORE_RETURN_VALUES)
                .withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_LOCKING)
                .withFlags(Flag.FORCE_SYNCHRONOUS)
                .put(src, target);

        return target;
    }

    @Override
    public void clear() {
        concepts.clear();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        concepts.forEach( (k, v) -> c.accept(v) );
    }

    @Override
    public int size() {
        return concepts.size();
    }

    @Override
    public int subtermsCount() {
        return subterms.size();
    }

    @Override
    protected TermContainer putIfAbsent(TermContainer x, TermContainer y) {
        return subterms
                .getAdvancedCache()
                .withFlags(Flag.IGNORE_RETURN_VALUES)
                .withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_LOCKING)
                .withFlags(Flag.FORCE_SYNCHRONOUS)
                .putIfAbsent(x, y);
    }
}
