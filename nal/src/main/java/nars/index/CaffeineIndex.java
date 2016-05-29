package nars.index;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex {

    final Cache<Termed, Termed> data;
    final Cache<TermContainer, TermContainer> subterms;

    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder) {
        this(Terms.terms, conceptBuilder);
    }

    public CaffeineIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super(termBuilder, conceptBuilder);

        data = Caffeine.newBuilder()
                .softValues()
                //.weakValues()
                //.maximumSize(10_000)
                //.expireAfterAccess(5, TimeUnit.MINUTES)
                //.refreshAfterWrite(1, TimeUnit.MINUTES)
                //.refreshAfterWrite(1, TimeUnit.NANOSECONDS)
                //.maximumSize(32*1024)
                .build();
                //.build(key -> createExpensiveGraph(key));

        subterms = Caffeine.newBuilder()
                .softValues()
                //.weakValues()
                //.maximumSize(10_000)
                //.expireAfterAccess(5, TimeUnit.MINUTES)
                //.refreshAfterWrite(1, TimeUnit.MINUTES)
                //.refreshAfterWrite(1, TimeUnit.NANOSECONDS)
                //.maximumSize(32*1024)
                .build();
    }

    @Override
    public Termed remove(Termed entry) {
        Termed t = get(entry);
        if (t!=null) {
            data.invalidate(entry);
        }
        return t;
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return data.getIfPresent(x);
    }

    @Override
    public @Nullable void set(@NotNull Termed src, Termed target) {
        data.put(src, target);
        //Termed exist = data.getIfPresent(src);

        //data.put(src, target);
        //data.cleanUp();
        //return target;

//        Termed current = data.get(src, (s) -> target);
//        return current;
    }

    @Override
    public void clear() {
        data.invalidateAll();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        data.asMap().forEach((k,v) -> c.accept(v));
    }

    @Override
    public int size() {
        return (int)data.estimatedSize();
    }

    @Override
    public int subtermsCount() {
        return -1;
    }

    @Override
    protected TermContainer putIfAbsent(TermContainer s, TermContainer s1) {
        return subterms.get(s, t -> s1);
    }

//    protected Termed theCompoundCreated(@NotNull Compound x) {
//
//        if (x.hasTemporal()) {
//            return internCompoundSubterms(x.subterms(), x.op(), x.relation(), x.dt());
//        }
//
//        Termed yyy = data.get(x, xx -> {
//            Compound y = (Compound)xx;
//            Termed yy = internCompoundSubterms(y.subterms(), y.op(), y.relation(), y.dt());
//            return internCompound(yy);
//        });
//        return yyy;
//
//    }

    @Override
    public @NotNull String summary() {
        return data.estimatedSize() + " concepts, " + subterms.estimatedSize() + " subterms";
    }
}
