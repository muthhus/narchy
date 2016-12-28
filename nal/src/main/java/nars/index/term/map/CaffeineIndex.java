package nars.index.term.map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import nars.Param;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.reason.ConceptBuilder;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeTermIndex implements RemovalListener<Term,Termed> {


//    @NotNull
//    public final Cache<Termed, Termed> atomics;
//    @NotNull
//    private final Map<Termed,Termed> atomics;


    /** holds compounds and subterm vectors */
    @NotNull public final Cache<Term, Termed> concepts;

    @Nullable
    private final Cache<TermContainer,TermContainer> subterms;

//    @NotNull
//    private final Cache<TermContainer, TermContainer> subs;


//    private static final Weigher<Term, Termed> weigher = (k, v) -> {
//
//        if (v instanceof PermanentConcept) {
//            return 0; //special concept implementation: dont allow removal
//        }
//
//        //        float beliefCost = (v instanceof CompoundConcept) ?
////                    (1f - maxConfidence((CompoundConcept)v)) : //discount factor for belief/goal confidence
////                    0;
//
//        //return v.complexity();
//        return v.volume();
//
//        //return Math.round( 1f + 100 * c * beliefCost);
//        //return Math.round( 1f + 10 * (c*c) * (0.5f + 0.5f * beliefCost));
//    };


    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(@NotNull ConceptBuilder conceptBuilder, long capacity, boolean soft, @Nullable Executor exe) {
        this(conceptBuilder, capacity, soft, exe, 0);
    }

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(@NotNull ConceptBuilder conceptBuilder, long capacity, boolean soft, @Nullable Executor exe, int maxSubterms) {
        super(conceptBuilder);


        //long maxSubtermWeight = maxWeight * 3; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine<Term, Termed> builder = Caffeine.newBuilder().removalListener(this);
        if (exe!=null)
                builder.executor(exe);

        if (soft) {
            builder.softValues();
//                //.weakValues() //.softValues()
        } else {
           //builder.weigher(weigher)
                //.maximumWeight(capacity);
        }

        if (capacity > 0)
            builder.maximumSize(capacity);

        if (Param.DEBUG)
            builder.recordStats();

        concepts = builder.build();


        if (maxSubterms > 0)
            this.subterms = Caffeine.newBuilder().maximumSize(maxSubterms).executor(exe).build();
        else
            this.subterms = null;

    }


    @NotNull
    @Override
    public final TermContainer intern(@NotNull TermContainer s) {
        return subterms!=null ? subterms.get(s, (ss) -> ss) :s;
    }

    @Override
    public void remove(@NotNull Term x) {
        concepts.invalidate(x);
    }


    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.asMap().merge(src, target, setOrReplaceNonPermanent);
    }


    @Override
    public void clear() {
        concepts.invalidateAll();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.asMap().values().forEach(c::accept);
    }

    @Override
    public int size() {
        return (int) concepts.estimatedSize();
    }


    @Override
    public Termed get(Term key, boolean createIfMissing) {
        if (createIfMissing) {
            return concepts.get(key, conceptBuilder);
        } else {
            return concepts.getIfPresent(key);
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
        String s = concepts.estimatedSize() + " concepts, " + (subterms!=null ? (subterms.estimatedSize() + " subterms") : "");

        if (Param.DEBUG)
            s += " " + concepts.stats().toString();

        return s;
        //(" + n2(s.hitRate()) + " hitrate, " +
                //s.requestCount() + " reqs)";

    }

    /** this will be called from within a worker task */
    @Override public final void onRemoval(Term key, Termed value, @NotNull RemovalCause cause) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(key, value);
            } else {
                delete(((Concept) value), nar);
            }
        }
    }




}
