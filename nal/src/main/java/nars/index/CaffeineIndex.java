package nars.index;

import com.github.benmanes.caffeine.cache.*;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.util.signal.WiredConcept;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex implements RemovalListener {

//    @NotNull
//    public final Cache<Termed, Termed> atomics;
    private final Map<Termed,Termed> atomics;

    @NotNull
    public final Cache<Termed, Termed> compounds;

    @NotNull
    private final Cache<TermContainer, TermContainer> subs;


    private static final Weigher<Termlike, Termlike> complexityAndConfidenceWeigher = (k, v) -> {

        float scale;
        if (v instanceof CompoundConcept) {
            if (v instanceof WiredConcept) {
                return 0; //special concept implementation: dont allow removal
            }
            scale = 1f/ (1f + maxConfidence((CompoundConcept)v)); //discount factor for belief/goal confidence
        } else {
            scale = 1f;
        }
        float w = 1 + v.complexity() * 10f;


        return (int)(w * scale);
    };

    private static float maxConfidence(@NotNull CompoundConcept v) {
        //return Math.max(v.beliefs().confMax(), v.goals().confMax());
        return v.beliefs().confMax() + v.goals().confMax();
    }


    public CaffeineIndex(Concept.ConceptBuilder builder, long maxWeight) {
        this(builder, maxWeight, false);
    }


    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder, long maxWeight, boolean soft) {
        super(conceptBuilder);

        long maxSubtermWeight = maxWeight * 2; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine<Termed, Termed> builder = prepare(Caffeine.newBuilder(), soft);

        final ExecutorService executor =
                //ForkJoinPool.commonPool();
                //Executors.newFixedThreadPool(2);
                //Executors.newCachedThreadPool();
                Executors.newSingleThreadExecutor();

        builder
               .weigher(complexityAndConfidenceWeigher)
               .maximumWeight(maxWeight)
               .removalListener(this)
               .executor(executor)

               //.recordStats()
        ;
        compounds = builder.build();


//        Caffeine<Termed, Termed> buildera = prepare(Caffeine.newBuilder(), false);
//        buildera
//                .removalListener(this)
//                .executor(executor);
//        atomics = buildera.build();
        atomics = new ConcurrentHashMap<>(256 /* estimate */);


        Caffeine<TermContainer, TermContainer> builderSubs = prepare(Caffeine.newBuilder(), false);

        subs = builderSubs
                //.weakValues() //.softValues()
                .weigher(complexityAndConfidenceWeigher)
                .maximumWeight(maxSubtermWeight)
                .executor(executor)
                .build();

//        Caffeine<TermContainer, TermContainer> builderSubs = prepare(Caffeine.newBuilder(), soft);
//        subs = builderSubs
//                .weigher(complexityWeigher)
//                .maximumWeight(maxWeight)
//                .build();


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

    @Override
    public void remove(@NotNull Termed x) {
        Term tx = x.term();
        if (tx instanceof Compound)
            compounds.invalidate(tx);
        else
            atomics.remove(tx);
    }

    @Override
    public Termed get(@NotNull Termed x) {
        Term tx = x.term();
        if (tx instanceof Compound)
            return compounds.getIfPresent(tx);
        else
            return atomics.get(tx);
    }

//    @NotNull
//    private final Cache<Termed,Termed> cacheFor(Term x) {
//        return x instanceof Compound ? compounds : atomics;
//    }

    @Override
    public void set(@NotNull Termed src, @NotNull Termed target) {
        Term tx = src.term();
        if (tx instanceof Compound)
            compounds.put(tx, target);
        else
            atomics.put(tx, target);

        //Termed exist = data.getIfPresent(src);

        //data.put(src, target);
        //data.cleanUp();
        //return target;

//        Termed current = data.get(src, (s) -> target);
//        return current;
    }


    @Override
    public void clear() {
        compounds.invalidateAll();
        //atomics.invalidateAll();
        subs.invalidateAll();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        BiConsumer<Termed, Termed> e = (k, v) -> c.accept(v);

        atomics.values().forEach(c);
        //atomics.asMap().forEach(e);

        compounds.asMap().forEach(e);
    }

    @Override
    public int size() {
        return (int) (compounds.estimatedSize() + atomics.size());
    }

    @Override
    public int subtermsCount() {
        return (int) subs.estimatedSize();
    }


    @Override
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return subs.getIfPresent(t);
    }


    @NotNull
    @Override
    protected TermContainer put(@NotNull TermContainer s) {
        return subs.get(s, ss -> ss);
        //subs.put(s, s);
        //return s;
    }


    @NotNull
    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return atomics.computeIfAbsent(x, this::buildConcept);
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
        return atomics.size() + " atoms " + compounds.estimatedSize() + " compounds / " + subtermsCount() + " subterms";
    }

    @Override
    public final void onRemoval(Object key, Object value, @Nonnull RemovalCause cause) {
        if (value instanceof Concept) {
            ((Concept)value).delete();
        }
    }
}
