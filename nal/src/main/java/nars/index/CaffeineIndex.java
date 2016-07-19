package nars.index;

import com.github.benmanes.caffeine.cache.*;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex implements RemovalListener {

    @NotNull
    public final Cache<Termed, Termed> atomics;

    @NotNull
    public final Cache<Termed, Termed> compounds;

    @NotNull
    public final Cache<TermContainer, TermContainer> subs;

    private static final Weigher<Termlike, Termlike> complexityWeigher = (k, v) -> {
//        if (v instanceof Atomic) {
//            return 0; //dont allow removal of atomic
//        } else {
            if (v instanceof Concept) {

                if (!(v instanceof CompoundConcept)) {
                    //special implementation, dont allow removal
                    return 0;
                }

                Concept c = (Concept)v;
                if (c.active())
                    return 0; //disallow removal of active concepts
            }

            int w = v.complexity();// * weightFactor;

            //w/=(1f + maxConfidence((CompoundConcept)v));

            return (int)w;
        //}
    };

    private static float maxConfidence(@NotNull CompoundConcept v) {
        return Math.max(v.beliefs().confMax(), v.goals().confMax());
    }


    public CaffeineIndex(Concept.ConceptBuilder builder, int maxWeight) {
        this(builder, maxWeight, false);
    }


    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder, int maxWeight, boolean soft) {
        super(conceptBuilder);

        Caffeine<Object, Object> builder = prepare(Caffeine.newBuilder(), soft);

        builder
               .weigher(complexityWeigher)
               .maximumWeight(maxWeight)
               .removalListener(this)
               //.recordStats()
        ;
        compounds = builder.build();


        Caffeine<Object, Object> buildera = prepare(Caffeine.newBuilder(), false);
        buildera
                .removalListener(this);
        atomics = buildera.build();


        Caffeine<TermContainer, TermContainer> builderSubs = prepare(Caffeine.newBuilder(), false);
        subs = builderSubs
                //.softValues()
                .weigher(complexityWeigher)
                .maximumWeight(maxWeight)
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
        cacheFor(x).invalidate(x);
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return cacheFor(x).getIfPresent(x);
    }

    private final Cache<Termed,Termed> cacheFor(Termed x) {
        if (x.term() instanceof Compound)
            return compounds;
        else
            return atomics;
    }

    @Override
    public void set(@NotNull Termed src, @NotNull Termed target) {

        cacheFor(src).
                //.get(src, s -> target);
                put(src, target);

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
        atomics.invalidateAll();
        subs.invalidateAll();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        BiConsumer<Termed, Termed> e = (k, v) -> c.accept(v);
        atomics.asMap().forEach(e);
        compounds.asMap().forEach(e);
    }

    @Override
    public int size() {
        return (int) (compounds.estimatedSize() + atomics.estimatedSize());
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
        subs.put(s, s);
        return s;
        //return subs.get(s, (ss) -> s); //HACK
    }


    @NotNull
    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return atomics.get(x, this::buildConcept);
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
        return size() + " concepts / " + subtermsCount() + " subterms";
    }

    @Override
    public void onRemoval(Object key, Object value, @Nonnull RemovalCause cause) {
        if (value instanceof Concept) {
            ((Concept)value).delete();
        }
    }
}
