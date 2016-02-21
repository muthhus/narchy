package nars.term;

import javassist.scopedpool.SoftValueHashMap;
import nars.Op;
import nars.budget.Budget;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import nars.term.index.MapIndex;
import nars.term.index.MapIndex2;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.*;

/**
 *
 */
public interface TermIndex extends TermBuilder {



    /** universal zero-length product */
    Compound Empty = new GenericCompound(Op.PRODUCT, -1, TermVector.Empty);
    TermVector EmptyList = new TermVector();
    TermSet EmptySet = TermSet.the();

    void clear();

    void forEach(Consumer<? super Termed> c);

    //Termed get(Object t);

    @Nullable
    Termed getTermIfPresent(Termed t);

    /** # of contained terms */
    int size();

    /**
     * implications, equivalences, and interval
     */
    int InvalidEquivalenceTerm = or(IMPLICATION, EQUIV);
    /**
     * equivalences and intervals (not implications, they are allowed
     */
    int InvalidImplicationPredicate = or(EQUIV);




    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, @NotNull Budget budget) {
        return spawn(parent, content, punctuation, truth, occ, budget.pri(), budget.dur(), budget.qua());
    }

    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, float p, float d, float q) {
        return new MutableTask(content, punctuation)
                .truth(truth)
                .budget(p, d, q)
                .parent(parent)
                .occurr(occ);
    }


    /** gets an existing item or applies the builder to produce something to return */
    @Nullable
    default <K extends Term> Termed<K> apply(K key, @NotNull Function<K,Termed> builder) {
        Termed existing = getTermIfPresent(key);
        if (existing == null) {
            putTerm(existing = builder.apply(key));
        }
        return existing;
    }

    @Nullable
    TermContainer internSub(TermContainer s);



    void putTerm(Termed termed);

    @Nullable
    default TermContainer unifySubterms(TermContainer s) {
        TermVector t = (TermVector)s;
        Term[] x = t.terms();
        for (int i = 0; i < x.length; i++) {
            Term xi = x[i];
            Termed u = the(xi); //since they are equal this will not need re-hashed
            if (u == null) return null;
            //if (u.equals(xi))
                x[i] = u.term(); //HACK use unified, otherwise keep original
        }
        return s;
    }


    default Termed makeAtomic(Term t) {
        return t; /* as-is */
    }

    @Nullable
    default Termed makeTerm(Term t) {
        return t instanceof Compound ?
                makeCompound((Compound) t)
                : makeAtomic(t);
    }


    int subtermsCount();

//    default TermContainer internSubterms(Term[] t) {
//        return new TermVector<>(t, this::the);
//    }


    @Nullable
    default Termed makeCompound(@NotNull Compound t) {
        return make(t.op(), t.relation(), t.subterms(), t.dt());
    }









//    class ImmediateTermIndex implements TermIndex {
//
//        @Override
//        public Termed get(Object key) {
//            return (Termed)key;
//        }
//
//        @Override
//        public TermContainer getIfAbsentIntern(TermContainer s) {
//            return s;
//        }
//
//        @Override
//        public Termed internAtomic(Term t) {
//            return t;
//        }
//
//        @Override
//        public void forEach(Consumer<? super Termed> c) {
//
//        }
//
//        @Override
//        public Termed getTermIfPresent(Termed t) {
//            return t;
//        }
//
//        @Override
//        public Termed intern(Term tt) {
//            return tt;
//        }
//
//        @Override
//        public int subtermsCount() {
//            return 0;
//        }
//
//
//        @Override
//        public void clear() {
//
//        }
//
//
//        @Override
//        public void putTerm(Termed termed) {
//
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//
//    }

//    class GuavaIndex extends GuavaCacheBag<Term,Termed> implements TermIndex {
//
//
//        public GuavaIndex(CacheBuilder<Term, Termed> data) {
//            super(data);
//        }
//
//        @Override
//        public void forEachTerm(Consumer<Termed> c) {
//            data.asMap().forEach((k,v) -> {
//                c.accept(v);
//            });
//        }
//
//
//
//    }

    /** default memory-based (Guava) cache */
    @NotNull
    static TermIndex memory(int capacity) {
//        CacheBuilder builder = CacheBuilder.newBuilder()
//            .maximumSize(capacity);
        return new MapIndex(
            new HashMap(capacity),new HashMap(capacity*2)
            //new UnifriedMap()
        );
//        return new MapIndex2(
//                new HashMap(capacity)
//                //new UnifriedMap()
//        );
    }
    /** default memory-based (Guava) cache */
    @NotNull
    static TermIndex softMemory(int capacity) {
//        CacheBuilder builder = CacheBuilder.newBuilder()
//            .maximumSize(capacity);
        return new MapIndex2(
                new SoftValueHashMap(capacity)
                //new WeakHashMap()
        );
//        return new MapIndex2(
//                new HashMap(capacity)
//                //new UnifriedMap()
//        );
    }
//    static GuavaIndex memoryGuava(Clock c, int expirationCycles) {
//        return new GuavaIndex(c, expirationCycles);
////        return new MapIndex(
////
////                new WeakValueHashMap(capacity),
////                new WeakValueHashMap(capacity*2)
////        );
//    }

    default void print(@NotNull PrintStream out) {
        forEach(out::println);
    }

//    /** for long-running processes, this uses
//     * a weak-value policy */
//    static TermIndex memoryAdaptive(int capacity) {
//        CacheBuilder builder = CacheBuilder.newBuilder()
//            .maximumSize(capacity)
//            .recordStats()
//            .weakValues();
//        return new GuavaIndex(builder);
//    }
}
