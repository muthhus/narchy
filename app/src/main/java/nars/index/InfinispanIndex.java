package nars.index;

import nars.IO;
import nars.Param;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.DecoratedCache;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class InfinispanIndex extends MaplikeIndex {

    private final Cache<ByteBuffer, Termed> concepts;
    private final DecoratedCache<ByteBuffer,Termed> conceptsLocal;
    private final Cache<ByteBuffer, TermContainer> subterms;
    private final DecoratedCache<ByteBuffer,TermContainer> subtermsLocal;

    private final IO.DefaultCodec codec;
    private final AdvancedCache<ByteBuffer,TermContainer> subtermsLocalNoResult;
    private final AdvancedCache<ByteBuffer,Termed> conceptsLocalNoResult;



    public InfinispanIndex(Concept.ConceptBuilder conceptBuilder) {
        super(conceptBuilder);

        this.codec = new IO.DefaultCodec(this);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.unsafe().versioning().disable();
        cb.locking().concurrencyLevel(1);
        cb.jmxStatistics().disable();
        //cb.customInterceptors().addInterceptor();



        DefaultCacheManager cm = new DefaultCacheManager(cb.build());
        //DefaultCacheManager cm = new DefaultCacheManager();
        //System.out.println(Joiner.on('\n').join(cm.getCacheManagerConfiguration().toString().split(", ")));

        this.concepts = cm.getCache("concepts");
        this.conceptsLocal = new DecoratedCache<>(
                concepts.getAdvancedCache(),
                Flag.CACHE_MODE_LOCAL, /*Flag.SKIP_LOCKING,*/ Flag.SKIP_OWNERSHIP_CHECK,
                Flag.SKIP_REMOTE_LOOKUP);
        this.conceptsLocalNoResult = conceptsLocal.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);
        this.subterms = cm.getCache("subterms");
        this.subtermsLocal = new DecoratedCache<>(
                subterms.getAdvancedCache(),
                Flag.CACHE_MODE_LOCAL, /*Flag.SKIP_LOCKING,*/ Flag.SKIP_OWNERSHIP_CHECK,
                Flag.SKIP_REMOTE_LOOKUP);
        this.subtermsLocalNoResult = subtermsLocal.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);


    }

    @Override
    public void remove(Termed x) {
        conceptsLocal.remove(key(x.term()));
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return conceptsLocal.get(key(x.term()));
    }


    @NotNull
    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return conceptsLocal.computeIfAbsent(key(x.term()), xx -> buildConcept(x));
    }

    @Override
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return subtermsLocal.get(key(t));
    }

    @Override
    protected Termed getNewCompound(@NotNull Compound x) {

        if (!canBuildConcept(x)) {
            return buildCompound(x);
        } else {
            return conceptsLocal.computeIfAbsent(key((Term)(x.term())), xx -> buildConcept(buildCompound(x)));
        }
    }

    @NotNull
    private Termed buildCompound(@NotNull Compound x) {
        return buildCompound(x.op(), x.dt(), x.subterms());
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
    @Deprecated public @Nullable void set(@NotNull Termed src, Termed target) {
        /*
        3.5.1. DecoratedCache

Another approach would be to use the DecoratedCache wrapper. This allows you to reuse flags. For example:

AdvancedCache cache = ...
DecoratedCache strictlyLocal = new DecoratedCache(cache, Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_STORE);
strictlyLocal.put("local_1", "only");
strictlyLocal.put("local_2", "only");
strictlyLocal.put("local_3", "only");
         */
        conceptsLocalNoResult
                .put(key(src.term()), target);

    }

    @Override
    public void clear() {
        conceptsLocal.clear();
        subtermsLocal.clear();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        conceptsLocal.forEach( (k, v) -> c.accept(v) );
    }

    @Override
    public int size() {
        return conceptsLocal.size();
    }

    @Override
    public int subtermsCount() {
        return subtermsLocal.size();
    }

    @Override
    protected TermContainer put(TermContainer x) {
        return subtermsLocal.putIfAbsent(key(x), x);
    }

    @Override
    public @NotNull String summary() {
        if (Param.DEBUG) {
            return conceptsLocal.size() + " concepts, " + subtermsLocal.size() + " subterms (WARNING: slow to count)";
        }
        else {
            return "";
        }
    }
}
