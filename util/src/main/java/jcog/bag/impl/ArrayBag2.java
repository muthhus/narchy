//package jcog.bag.impl;
//
//import edu.virginia.cs.skiptree.ConcurrentSkipTreeMap;
//import edu.virginia.cs.skiptree.ConcurrentSkipTreeSet;
//import jcog.bag.Bag;
//import jcog.data.graph.AdjGraph;
//import jcog.pri.PriReference;
//import jcog.pri.Priority;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Iterator;
//import java.util.concurrent.ConcurrentSkipListMap;
//import java.util.function.Consumer;
//
//public class ArrayBag2<X> implements Bag<X,PriReference<X>> {
//
//    @Override
//    public void clear() {
//        map.clear();
//    }
//
//    @Nullable
//    @Override
//    public PriReference<X> get(@NotNull Object key) {
//        return map.get(key);
//    }
//
//    @Nullable
//    @Override
//    public PriReference<X> remove(@NotNull X x) {
//        return map.remove(x);
//    }
//
//    @Override
//    public PriReference<X> put(@NotNull PriReference<X> p, @Nullable MutableFloat overflowing) {
//        map.size()
//        PNode k = new PNode(p.get(), p.priElseZero());
//        map.merge(k, k, (a, b) -> {
//            return a;
//        });
//    }
//
//    @NotNull
//    @Override
//    public Bag<X, PriReference<X>> sample(BagCursor<? super PriReference<X>> each) {
//        return null;
//    }
//
//    @Override
//    public int size() {
//        return 0;
//    }
//
//    @Override
//    public int capacity() {
//        return 0;
//    }
//
//    @NotNull
//    @Override
//    public Iterator<PriReference<X>> iterator() {
//        return null;
//    }
//
//    @Override
//    public float pri(@NotNull PriReference<X> key) {
//        return 0;
//    }
//
//    @Override
//    public X key(PriReference<X> value) {
//        return null;
//    }
//
//    @Override
//    public void setCapacity(int c) {
//
//    }
//
//    @Override
//    public Bag<X, PriReference<X>> commit() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public Bag<X, PriReference<X>> commit(Consumer<PriReference<X>> update) {
//        return null;
//    }
//
//    public static class PNode<X> implements PriReference<X>, Comparable<PNode> {
//
//        @Override
//        public X get() {
//            return null;
//        }
//
//        @Override
//        public float setPri(float p) {
//            return 0;
//        }
//
//        @Override
//        public @Nullable Priority clonePri() {
//            return null;
//        }
//
//        @Override
//        public float pri() {
//            return 0;
//        }
//
//        @Override
//        public boolean delete() {
//            return false;
//        }
//
//        @Override
//        public boolean isDeleted() {
//            return false;
//        }
//
//        @Override
//        public int compareTo(@NotNull ArrayBag2.PNode o) {
//            return 0;
//        }
//    }
//
//    final ConcurrentSkipTreeMap<PNode<X>,PNode<X>> map = new ConcurrentSkipTreeMap<>();
//    //final ConcurrentSkipListMap<PNode<X>,PNode<X>> map = new ConcurrentSkipListMap<>();
//}
