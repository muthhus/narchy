package nars.index.term.map;

import com.github.benmanes.caffeine.cache.*;
import nars.NAR;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.concept.WiredConcept;
import nars.conceptualize.ConceptBuilder;
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


    final static Weigher<? super Term, ? super Termed> w = (k,v) -> {
        if (v instanceof PermanentConcept) return 0;
        int vol = v.volume();
        //return vol;
        return vol;
    };

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(@NotNull ConceptBuilder conceptBuilder, long capacity, @Nullable Executor exe) {
        this(conceptBuilder, capacity, -1, exe);
    }

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(@NotNull ConceptBuilder conceptBuilder, long capacity, long subCapacity, @Nullable Executor exe) {
        super(conceptBuilder);


        //long maxSubtermWeight = maxWeight * 3; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine<Term, Termed> builder = Caffeine.newBuilder().removalListener(this);
        if (capacity > 0) {
            //builder.maximumSize(capacity);
            builder.maximumWeight(capacity*10);
            builder.weigher(w);
        } else
            builder.softValues();

        if (Param.DEBUG)
            builder.recordStats();


        if (exe!=null) {
            builder.executor(exe);
        }

        Caffeine<Object, Object> subTermsBuilder = subCapacity > 0 ? Caffeine.newBuilder() : null;

        if (subCapacity > 0) {
            subTermsBuilder.executor(exe);

            if (subCapacity > 0)
                subTermsBuilder.maximumSize(subCapacity);
            else {
                //subTermsBuilder.weakValues();
                subTermsBuilder.softValues();
            }
        }


        this.concepts = builder.build();
        this.subterms = subTermsBuilder!=null ? subTermsBuilder.build() : null;

        //else
          //  this.subterms = null;

    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        nar.onCycle(this::cleanUp);
    }

    protected void cleanUp() {
        concepts.cleanUp();
        if (subterms!=null)
            subterms.cleanUp();
    }

    @NotNull
    @Override
    public final TermContainer intern(@NotNull Term[] a) {

        TermContainer v = super.intern(a);

        if (subterms!=null) {
            int len = a.length;
            if (len < 1)
                return v; //dont intern small or empty containers

            //        //HACK
            //        if (x instanceof EllipsisTransform || y instanceof EllipsisTransform)
            //            return new TermVector2(x, y);

            //        DynByteSeq d = new DynByteSeq(4 * len /* estimate */);
            //        try {
            //            IO.writeTermContainer(d, a);
            //        } catch (IOException e) {
            //            throw new RuntimeException(e);
            //        }

            return subterms.get(v, vv -> vv);
        } else {
            return v;
        }


        //return subterms!=null ? subterms.get(s, (ss) -> ss) :s;
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

//    @Override
//    public void commit(Concept c) {
//        //concepts.getIfPresent(c.term());
//    }

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
            s += ' ' + concepts.stats().toString();

        return s + '\n' + super.summary();
        //(" + n2(s.hitRate()) + " hitrate, " +
                //s.requestCount() + " reqs)";

    }

    /** this will be called from within a worker task */
    @Override public final void onRemoval(Term key, Termed value, @NotNull RemovalCause cause) {

        if (value!=null)
            onRemove(value);
    }


}
