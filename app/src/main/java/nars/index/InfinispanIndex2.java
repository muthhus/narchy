package nars.index;

import com.google.common.base.Joiner;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.util.IO;
import org.infinispan.Cache;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class InfinispanIndex2 extends MaplikeIndex {

    private final Cache<ByteBuffer, Termed> concepts;
    private final Cache<ByteBuffer, TermContainer> subterms;

    private final IO.DefaultCodec codec;


    public InfinispanIndex2(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super(termBuilder, conceptBuilder);

        this.codec = new IO.DefaultCodec(this);

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
    public Termed remove(Termed x) {
        return concepts.remove(key(x.term()));
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return concepts.get(key(x.term()));
    }


    @Override
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return subterms.get(key(t));
    }

    protected Termed theCompoundCreated(@NotNull Compound x) {

        if (x.hasTemporal()) {
            return internCompoundSubterms(x.subterms(), x.op(), x.relation(), x.dt());
        }

        Termed yy = concepts.getAdvancedCache()
                //.withFlags(Flag.IGNORE_RETURN_VALUES)
                .withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_LOCKING)
                .withFlags(Flag.FORCE_SYNCHRONOUS)
        .computeIfAbsent(key(x.term()), xx -> {
            Termed y = internCompoundSubterms(x.subterms(), x.op(), x.relation(), x.dt());
            return internCompound(y);
        });
        return yy;

    }



    public ByteBuffer key(@NotNull Term x) {
        byte[] b = codec.asByteArray(x);
        return new ByteBufferImpl(b,0,b.length);
    }

    public ByteBuffer key(@NotNull TermContainer x) {
        byte[] b = codec.asByteArray(x);
        return new ByteBufferImpl(b,0,b.length);
    }


    @Override
    @Deprecated public @Nullable Termed set(@NotNull Termed src, Termed target) {
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
                .put(key(src.term()), target);

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
                .putIfAbsent(key(x), y);
    }

    @Override
    public @NotNull String summary() {
        return concepts.size() + " concepts, " + subterms.size() + " subterms";
    }
}
