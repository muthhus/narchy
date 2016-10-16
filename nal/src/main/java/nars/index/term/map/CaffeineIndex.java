package nars.index.term.map;

import com.github.benmanes.caffeine.cache.*;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeTermIndex implements RemovalListener {

    private NAR nar;

//    @NotNull
//    public final Cache<Termed, Termed> atomics;
//    @NotNull
//    private final Map<Termed,Termed> atomics;


    /** holds compounds and subterm vectors */
    @NotNull public final Cache<Term, Termed> cache;

    private final Cache<TermContainer,TermContainer> subterms;

//    @NotNull
//    private final Cache<TermContainer, TermContainer> subs;


    private static final Weigher<Term, Termed> weigher = (k, v) -> {

        if (v instanceof PermanentConcept) {
            return 0; //special concept implementation: dont allow removal
        }

        //        float beliefCost = (v instanceof CompoundConcept) ?
//                    (1f - maxConfidence((CompoundConcept)v)) : //discount factor for belief/goal confidence
//                    0;

        //return v.complexity();
        return v.volume();

        //return Math.round( 1f + 100 * c * beliefCost);
        //return Math.round( 1f + 10 * (c*c) * (0.5f + 0.5f * beliefCost));
    };

    public CaffeineIndex(ConceptBuilder conceptBuilder, int capacity, int avgVolume, boolean soft, @NotNull Executor exe) {
        this(conceptBuilder, capacity * avgVolume, soft, exe, capacity*4);
    }

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(ConceptBuilder conceptBuilder, long maxWeight, boolean soft, @NotNull Executor exe) {
        this(conceptBuilder, maxWeight, soft, exe, 0);
    }

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(ConceptBuilder conceptBuilder, long maxWeight, boolean soft, @NotNull Executor exe, int maxSubterms) {
        super(conceptBuilder);


        //long maxSubtermWeight = maxWeight * 3; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine<Term, Termed> builder = Caffeine.newBuilder()
                .removalListener(this)
                .executor(exe);

        if (soft) {
            builder.softValues();
//                //.weakValues() //.softValues()
        } else {
           builder.weigher(weigher)
                .maximumWeight(maxWeight);
        }

        //.recordStats()

        cache = builder.build();


        if (maxSubterms > 0)
            this.subterms = Caffeine.newBuilder().maximumSize(maxSubterms).executor(exe).build();
        else
            this.subterms = null;

    }


    @Override
    public final TermContainer intern(TermContainer s) {
        return subterms!=null ? subterms.get(s, (ss) -> ss) :s;
    }

    @Override
    public void remove(@NotNull Term x) {
        cache.invalidate(x);
    }


    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        cache.asMap().merge(src, target, setOrReplaceNonPermanent);
    }


    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        cache.asMap().values().forEach(c::accept);
    }

    @Override
    public int size() {
        return (int)cache.estimatedSize();
    }


    @Override
    public Termed get(Term key, boolean createIfMissing) {
        if (createIfMissing) {
            return cache.get(key, conceptBuilder);
        } else {
            return cache.getIfPresent(key);
        }
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
        //CacheStats s = cache.stats();
        return cache.estimatedSize() + " concepts, " + (subterms!=null ? (subterms.estimatedSize() + " subterms") : "");
        //(" + n2(s.hitRate()) + " hitrate, " +
                //s.requestCount() + " reqs)";

    }

    /** this will be called from within a worker task */
    @Override public final void onRemoval(Object key, Object value, @NotNull RemovalCause cause) {
        if (value instanceof Concept) {
            delete(((Concept) value), nar);
        }
    }



    @Override
    public void start(NAR nar) {
        this.nar = nar;
    }


}
