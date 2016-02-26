package nars.term.index;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import nars.Op;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import org.cache2k.Cache;
import org.cache2k.CacheSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** TermIndex implemented with Cache2K with
 * optional WeakRef policy.
 * suitable for running indefnitely and obeying AIKR
 * principles
 * TODO not ready yet
 * */
public class Cache2KIndex extends CacheLoader<Termlike,IntObjectHashMap<Termed>> implements TermIndex {
    //http://cache2k.org/#Getting_started

    Cache<Termlike, IntObjectHashMap> data;


    public Cache2KIndex() {
        data = org.cache2k.CacheBuilder.newCache(Termlike.class,IntObjectHashMap.class)
                .newCache(Termlike.class, IntObjectHashMap.class)
                .eternal(true)
                .heapEntryCapacity(1000)
                .build();


//        CacheSource<Term,Termed> dataBuilder =
//                new CacheSource<Term,Termed>() {
//                    @Override
//                    public Termed get(Term o) throws Throwable {
//                        return o;
//                    }
//                };
//        Cache<Term,Termed> c =
//                CacheBuilder.newCache(Term.class, Termed.class)
//                        //.source()
//                        .eternal(true)
//                        .maxSize()
//                        .build();
    }

    @Override
    public IntObjectHashMap<Termed> load(Termlike key) throws Exception {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public void forEach(Consumer<? super Termed> c) {

    }

    @Override
    public
    @Nullable
    Termed getIfPresent(Termed t) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public
    @Nullable
    TermContainer internSub(TermContainer s) {
        return null;
    }

    @Override
    public void putTerm(Termed termed) {

    }

    @Override
    public int subtermsCount() {
        return 0;
    }

    @Override
    public
    @Nullable
    Termed make(Op op, int relation, TermContainer subterms, int dt) {
        return null;
    }
}
