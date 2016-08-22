package nars.index;

import com.github.benmanes.caffeine.cache.*;
import nars.NAR;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex implements RemovalListener {

    private NAR nar;

//    @NotNull
//    public final Cache<Termed, Termed> atomics;
    @NotNull
    private final Map<Termed,Termed> atomics;


    /** holds compounds and subterm vectors */
    @NotNull public final Cache<Termlike, Termlike> compounds;

//    @NotNull
//    private final Cache<TermContainer, TermContainer> subs;


    private static final Weigher<Termlike, Termlike> complexityAndConfidenceWeigher = (k, v) -> {

        if (v instanceof WiredConcept) {
            return 0; //special concept implementation: dont allow removal
        }

        return weigh(v);
    };

    private static int weigh(Termlike v) {
        float beliefDiscount = (v instanceof CompoundConcept) ?
                    (1f + maxConfidence((CompoundConcept)v)) : //discount factor for belief/goal confidence
                    1;
        return Math.round( 1f + 100 * v.complexity() / beliefDiscount);
    }

    private static float maxConfidence(@NotNull CompoundConcept v) {
        //return Math.max(v.beliefs().confMax(), v.goals().confMax());
        return ((v.beliefs().confMax()) + (v.goals().confMax()));
    }


    public CaffeineIndex(Concept.ConceptBuilder builder, long maxWeight) {
        this(builder, maxWeight, false,
                //ForkJoinPool.commonPool()
                //Executors.newFixedThreadPool(1)
                Executors.newSingleThreadExecutor()
        );
    }


    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder, long maxWeight, boolean soft, @NotNull Executor executor) {
        super(conceptBuilder);

        //long maxSubtermWeight = maxWeight * 3; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine<Termlike, Termlike> builder = prepare(Caffeine.newBuilder(), soft);
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
        atomics = new ConcurrentHashMap<>(256 /* estimate */); //TODO this should probably be a Caffeine again, with a limit and restriction on removing WiredConcept's


//        Caffeine<TermContainer, TermContainer> builderSubs = prepare(Caffeine.newBuilder(), false);

//        subs = builderSubs
//                //.weakValues() //.softValues()
//                .weigher(complexityAndConfidenceWeigher)
//                .maximumWeight(maxSubtermWeight)
//                .executor(executor)
//                .build();

//        Caffeine<TermContainer, TermContainer> builderSubs = prepare(Caffeine.newBuilder(), soft);
//        subs = builderSubs
//                .weigher(complexityWeigher)
//                .maximumWeight(maxWeight)
//                .build();


    }


    private static Caffeine prepare(Caffeine<Object, Object> builder, boolean soft) {

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
            return (Termed) compounds.getIfPresent(tx);
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
            compounds.put(tx, (Termlike)target);
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
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        //BiConsumer<Termed, Termed> e = (k, v) -> c.accept(v);

        atomics.values().forEach(c);
        //atomics.asMap().forEach(e);

        compounds.asMap().values().forEach(x -> {
           if (x instanceof Termed)
               c.accept((Termed)x);
        });
    }

    @Override
    public int size() {
        return (int) (compounds.estimatedSize() + atomics.size());
    }

    @Override
    public int subtermsCount() {
        return -1; //not calculated when they share the same cache
        //return (int) subs.estimatedSize();
    }


    @NotNull @Override
    public TermContainer internSubterms(@NotNull TermContainer t) {
        return (TermContainer) compounds.get(t, tt -> tt);
    }


    /**
     * default lowest common denominator impl, subclasses may reimpl for more efficiency
     */
    @NotNull
    protected Termed getConceptCompound(@NotNull Compound x) {
        return (Termed) compounds.get(conceptualize(x), y -> (Termlike)buildConcept((Compound)y));
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
        return atomics.size() + " atoms " + compounds.estimatedSize() + " compounds";// / " + subtermsCount() + " subterms";
    }

    /** this will be called from within a worker task */
    @Override public final void onRemoval(Object key, Object value, @Nonnull RemovalCause cause) {
        if (value instanceof Concept) {
            ((Concept) value).delete(nar);
        }
    }

    @Override
    public void start(NAR nar) {
        this.nar = nar;
    }


}
