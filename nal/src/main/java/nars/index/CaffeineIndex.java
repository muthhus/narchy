package nars.index;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nars.concept.Concept;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex {

    @NotNull
    final Cache<Termed, Termed> concepts;
    @NotNull
    final Cache<TermContainer, TermContainer> subterms;

    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder) {
        this(Terms.terms, conceptBuilder);
    }

    public CaffeineIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super(termBuilder, conceptBuilder);

        concepts = Caffeine.newBuilder()
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

    @Nullable
    @Override
    public Termed remove(@NotNull Termed entry) {
        Termed t = get(entry);
        if (t!=null) {
            concepts.invalidate(entry);
        }
        return t;
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return concepts.getIfPresent(x);
    }

    @Override
    public @Nullable void set(@NotNull Termed src, @NotNull Termed target) {
        concepts.put(src, target);
        //Termed exist = data.getIfPresent(src);

        //data.put(src, target);
        //data.cleanUp();
        //return target;

//        Termed current = data.get(src, (s) -> target);
//        return current;
    }


    @Override
    public void clear() {
        concepts.invalidateAll();
        subterms.invalidateAll();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.asMap().forEach((k, v) -> c.accept(v));
    }

    @Override
    public int size() {
        return (int) concepts.estimatedSize();
    }

    @Override
    public int subtermsCount() {
        return -1;
    }

    @Override
    protected TermContainer putIfAbsent(@NotNull TermContainer s, TermContainer s1) {
        return subterms.get(s, t -> s1);
    }

    @Override
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return subterms.getIfPresent(t);
    }


    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return concepts.get(x, this::build);
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
        return concepts.estimatedSize() + " concepts, " + subterms.estimatedSize() + " subterms";
    }
}
