package nars.index;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex  {

    @NotNull
    public final Cache<Termed, Termed> concepts;
    @NotNull
    public final Cache<TermContainer, TermContainer> subterms;


    private final Weigher<Termed, Termed> conceptWeigher = (k,v) -> {
        if (v instanceof Atomic) {
            return 1;
        } else {
            if (!(v instanceof CompoundConcept)) {
                //special implementation, dont allow removal
                return 0;
            }
            int w = v.volume();// * weightFactor;
            //w/=(1f + maxConfidence((CompoundConcept)v));

            return (int)w;
        }
    };

    private float maxConfidence(CompoundConcept v) {
        return Math.max(v.beliefs().confMax(), v.goals().confMax());
    }

    private final Weigher<TermContainer, TermContainer> subtermWeigher = new Weigher<TermContainer, TermContainer>() {
        @Override
        public int weigh(@Nonnull TermContainer key, @Nonnull TermContainer value) {
            return (int)value.volume();// * weightFactor);
        }
    };

    public CaffeineIndex(int maxWeight, Concept.ConceptBuilder builder) {
        this(maxWeight, builder, false);
    }

    public CaffeineIndex(int maxWeight, Concept.ConceptBuilder conceptBuilder, boolean soft) {
        super(conceptBuilder);

        Caffeine<Termed, Termed> builder = prepare(Caffeine.newBuilder(), soft);

        builder.weigher(conceptWeigher)
               .maximumWeight(maxWeight)
               .recordStats();

        concepts = builder.build();


        Caffeine<TermContainer, TermContainer> subBuilder = prepare(Caffeine.newBuilder(), soft);
        subBuilder.weigher(subtermWeigher)
                .maximumWeight(maxWeight);

        subterms = subBuilder.build();
    }


    private Caffeine prepare(Caffeine<Object, Object> builder, boolean soft) {

        //builder = builder.initialCapacity(initialSize);

        if (soft) {
            //builder = builder.softValues();
            builder = builder.weakValues();


        }

        //.softValues()
        //.maximumSize(10_000)
        //.expireAfterAccess(5, TimeUnit.MINUTES)
        //.refreshAfterWrite(1, TimeUnit.MINUTES)
        //.refreshAfterWrite(1, TimeUnit.NANOSECONDS)
        //.maximumSize(32*1024)
        //.build(key -> createExpensiveGraph(key));

        return builder;
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
        return (int) subterms.estimatedSize();
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
        return concepts.get(x, this::buildConcept);
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
