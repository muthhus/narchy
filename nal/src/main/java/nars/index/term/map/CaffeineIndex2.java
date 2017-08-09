package nars.index.term.map;

import com.github.benmanes.caffeine.cache.*;
import nars.Op;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.index.util.TermContainerToOpMap;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.True;


public class CaffeineIndex2 extends MaplikeTermIndex implements RemovalListener<TermContainer, TermContainerToOpMap<Termed>> {


//    @NotNull
//    public final Cache<Termed, Termed> atomics;
//    @NotNull
//    private final Map<Termed,Termed> atomics;


    /**
     * holds compounds and subterm vectors
     */
    @NotNull
    public final Cache<TermContainer, TermContainerToOpMap<Termed>> vectors;


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



    final static Weigher<TermContainer, TermContainerToOpMap> w = (k, v) -> {
        //return k.volume();
        return (k.complexity() + k.volume())/2;
    };

    public CaffeineIndex2(long capacity) {
        this(capacity, null /*MoreExecutors.directExecutor()*/);
    }

    /**
     * use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms
     */
    public CaffeineIndex2(long capacity, @Nullable Executor exe) {
        super();

        //long maxSubtermWeight = maxWeight * 3; //estimate considering re-use of subterms in compounds and also caching of non-compound subterms

        Caffeine builder = Caffeine.newBuilder().removalListener(this);
        if (capacity > 0) {
            //builder.maximumSize(capacity);
            builder.maximumWeight(capacity * 10);
            builder.weigher(w);
        } else
            builder.softValues();

        if (Param.DEBUG)
            builder.recordStats();


        if (exe != null) {
            builder.executor(exe);
        }


        this.vectors = builder.build();

        //else
        //  this.subterms = null;

    }

    @Override
    public Stream<Termed> stream() {
        return vectors.asMap().values().stream().flatMap(x -> IntStream.range(0, TermContainerToOpMap.CAPACITY).mapToObj(x::get).filter(Objects::nonNull));
    }

    //    @Override
//    public void start(NAR nar) {
//        super.start(nar);
//        //nar.onCycle(this::cleanUp);
//    }

//private static final long cleanPeriod = 16 /* cycles */;
//    protected void cleanUp() {
//        if (nar.time() % cleanPeriod == 0) {
//            concepts.cleanUp();
//            if (subterms != null)
//                subterms.cleanUp();
//        }
//    }

//    @NotNull
//    @Override
//    public final TermContainer intern(@NotNull Term[] a) {
//
//        TermContainer v = super.intern(a);
//
//        if (subterms!=null) {
//            int len = a.length;
//            if (len < 1)
//                return v; //dont intern small or empty containers
//
//            //        //HACK
//            //        if (x instanceof EllipsisTransform || y instanceof EllipsisTransform)
//            //            return new TermVector2(x, y);
//
//            //        DynByteSeq d = new DynByteSeq(4 * len /* estimate */);
//            //        try {
//            //            IO.writeTermContainer(d, a);
//            //        } catch (IOException e) {
//            //            throw new RuntimeException(e);
//            //        }
//
//            return subterms.get(v, vv -> vv);
//        } else {
//            return v;
//        }
//
//
//        //return subterms!=null ? subterms.get(s, (ss) -> ss) :s;
//    }

    @Override
    public void remove(@NotNull Term x) {

        TermContainerToOpMap<Termed> v = vectors.getIfPresent(vector(x));
        if (v != null) {
            v.set(x.op().id, null);

//            if (v.getAndSet(x.op().id, null) != null) {
//                if (v.isEmpty()) {
//                    vectors.invalidate(v);
//                }
//            }
        }

    }

    static TermContainer vector(Term x) {
        TermContainer xs = x.subterms();
        if (xs.size() == 0) {
            //atomic
            return TermVector.the(x, True); //to distinguish from: (x)
        } else {
            return xs;
        }
        //return TermVector.the(ArrayUtils.add(xs.theArray() /* SAFE COPY */, x));
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        vectorOrCreate(src).set(src.op().id, target);
    }

    private TermContainerToOpMap<Termed> vectorOrCreate(@NotNull Term x) {
        return vectors.get(vector(x), (v) -> {
            return new TermContainerToOpMap<>(v);
        });
    }


    @Override
    public void clear() {
        vectors.invalidateAll();
    }


    @Override
    public int size() {
        return (int) vectors.estimatedSize(); /* warning: underestimate */
    }


    @Override
    public Termed get(Term x, boolean createIfMissing) {


        assert (!(x instanceof Variable)) : "variables should not be stored in index";

        Op op = x.op();
        TermContainerToOpMap<Termed> v;
        if (createIfMissing) {
            v = vectors.get(vector(x), k -> {

                TermContainerToOpMap<Termed> t = new TermContainerToOpMap<>(k);

                Termed p = conceptBuilder.apply(x);

                if (p != null)
                    t.compareAndSet(op.id, null, p);
                //else: ?

                return t;
            });
        } else {
            v = vectors.getIfPresent(vector(x));
        }

        return v != null ? v.get(op.id) : null;

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
        String s = vectors.estimatedSize() + " vectors, ";

        return s + ' ' + super.summary();
        //(" + n2(s.hitRate()) + " hitrate, " +
        //s.requestCount() + " reqs)";

    }


    @Override
    public void onRemoval(TermContainer key, TermContainerToOpMap<Termed> value, RemovalCause cause) {
        value.forEach(this::onRemove);
    }
}
