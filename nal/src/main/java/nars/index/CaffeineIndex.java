package nars.index;

import com.github.benmanes.caffeine.cache.*;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.WeakHashMap;
import java.util.function.Consumer;


public class CaffeineIndex extends MaplikeIndex implements RemovalListener {

    @NotNull
    public final Cache<Object, Object> data;
    public final WeakHashMap<TermContainer,TermContainer> subs = new WeakHashMap<>();

    private final Weigher<Object, Object> conceptWeigher = (k,v) -> {
        if (v instanceof Atomic) {
            return 0; //dont allow removal of atomic
        } else {
            int w;
            if (v instanceof Concept) {
                if (!(v instanceof CompoundConcept)) {
                    //special implementation, dont allow removal
                    return 0;
                }
            }

            w = ((Termed)v).complexity();// * weightFactor;

            //w/=(1f + maxConfidence((CompoundConcept)v));

            return (int)w;
        }
    };

    private static float maxConfidence(CompoundConcept v) {
        return Math.max(v.beliefs().confMax(), v.goals().confMax());
    }


    public CaffeineIndex(Concept.ConceptBuilder builder) {
        this(builder, false);
    }

    public CaffeineIndex(Concept.ConceptBuilder conceptBuilder, boolean soft) {
        super(conceptBuilder);

        Caffeine<Object, Object> builder = prepare(Caffeine.newBuilder(), soft);

        final int maxWeight = 50000 * 4;
        builder
               .weigher(conceptWeigher)
               .maximumWeight(maxWeight)
               .removalListener(this)


               //.recordStats()
        ;


        data = builder.build();

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
            data.invalidate(entry);
        }
        return t;
    }

    @Override
    public Termed get(@NotNull Termed x) {
        return (Termed) data.getIfPresent(x);
    }

    @Override
    public @Nullable void set(@NotNull Termed src, @NotNull Termed target) {
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
    public void forEach(@NotNull Consumer<? super Termed> c) {
        data.asMap().forEach((k, v) -> {
            if (v instanceof Termed)
                c.accept((Termed)v);
        });
    }

    @Override
    public int size() {
        return (int) data.estimatedSize();
    }

    @Override
    public int subtermsCount() {
        return subs.size();
    }

    @Override
    protected TermContainer putIfAbsent(@NotNull TermContainer s) {
        return subs.putIfAbsent(s, s);
    }

    @Override
    protected TermContainer getSubterms(@NotNull TermContainer t) {

        return subs.get(t);
    }


    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return (Termed) data.get(x, (interned) ->
                buildConcept((Atomic)interned));
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
        return data.estimatedSize() + " concepts / " + subs.size() + " subterms";
    }

    @Override
    public void onRemoval(Object key, Object value, @Nonnull RemovalCause cause) {
        if (value instanceof Concept) {
            ((Concept)value).delete();
        }
    }
}
