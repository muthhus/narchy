package jcog.bag;

import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import jcog.table.Table;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static jcog.bag.Bag.BagCursorAction.Next;


/**
 * K=key, V = item/value of type Item
 */
public interface Bag<K, V> extends Table<K, V>, Iterable<V> {

    enum BagCursorAction {
        Next(/*false, */false),
        //Remove(true, false),
        Stop(/*false, */true)
        //RemoveAndStop(true, true)
        ;

        //public boolean remove;
        public boolean stop;

        BagCursorAction(/*boolean remove, */boolean stop) {
            //this.remove = remove;
            this.stop = stop;
        }
    }

    /**
     * used for sampling
     */
    @FunctionalInterface
    interface BagCursor<V> {
        @NotNull BagCursorAction next(@NotNull V x);
    }

    /**
     * returns the bag to an empty state
     */
    @Override
    void clear();

    /**
     * gets the next value without removing changing it or removing it from any index.  however
     * the bag is cycled so that subsequent elements are different.
     */
    @Nullable
    default V sample() {
        Object[] result = new Object[1];
        sample((x) -> {
            result[0] = x;
            return BagCursorAction.Stop;
        });
        return (V) result[0];
    }


    /**
     * TODO rename 'remove'
     *
     * @param key
     * @return
     */
    @Override
    @Nullable
    V remove(@NotNull K x);


    default V put(@NotNull V x) {
        return put(x, null);
    }

    //        @Nullable
//        @Override public PLink<V> put(@NotNull V v) {
//            //TODO combine with CurveBag.put(v)
//            PLink<V> existing = get(v);
//            if (existing!=null) {
//                merge(existing, getDefaultBudget(v), 1f);
//                return existing;
//            } else {
//            }
//        }
//        @Nullable
//        @Override public PLink<V> put(@NotNull V v) {
//            PLink<V> existing = get(v);
//            return (existing != null) ?
//                    existing :
//                    put(v, getDefaultBudget(v));
//        }


    V put(@NotNull V b, @Nullable MutableFloat overflowing);


    /* sample the bag, optionally removing each visited element as decided by the visitor's
     * return value */
    @NotNull Bag<K, V> sample(@NotNull Bag.BagCursor<? super V> each, boolean pop);
    default @NotNull Bag<K, V> sample(@NotNull Bag.BagCursor<? super V> each) { return sample(each, false); }

    default List<V> copyToList(int n) {
        if (n == 0)
            return Collections.emptyList();

        List<V> l = new FasterList(n);
        sample(n, x -> {
            l.add(x);
        });
        return l;
    }


    /** normalizes a priorty to within the present min/max range of this bag, and unitized to within 0..1.0 clipping if exceeded */
    default float normalizeMinMax(float pri) {
        return Util.unitize(Util.lerp(pri, priMin(), priMax()));
    }

    default void normalizeAll(float lerp) {

        int size = size();
        if (size == 0 || lerp < Priority.EPSILON)
            return;

        float min = priMin();
        float max = priMax();
        if (Util.equals(min, max, Priority.EPSILON)) {
            //flatten all to 0.5
            commit(x -> ((Priority) x).priLerp(0.5f, lerp));
        } else {
            float range = max - min;
            commit(x -> ((Priority) x).normalizePri(min, range, lerp));
        }
    }


    /**
     * convenience macro for using sample(BagCursor).
     * continues while either the predicate hasn't returned false and
     * < max true's have been returned
     */
    default Bag<K, V> sample(int max, Predicate<? super V> kontinue) {
        final int[] count = {max};
        return sample((x) -> {
            return (kontinue.test(x) && ((count[0]--) > 0)) ?
                    Next : Bag.BagCursorAction.Stop;
        });
    }


    default Bag<K, V> sample(int max, Consumer<? super V> each) {
        return sampleOrPop(false, max, each);
    }

    default Bag<K, V> pop(int max, Consumer<? super V> each) {
        return sampleOrPop(true, max, each);
    }

    default Bag<K, V> sampleOrPop(boolean sampleOrPop, int max, Consumer<? super V> each) {
        final int[] count = {max};
        return sample((x) -> {
            each.accept(x);
            return ((count[0]--) > 0) ? Next : Bag.BagCursorAction.Stop;
        }, sampleOrPop);
    }
    @Nullable
    default V maxBy(FloatFunction<V> rank) {
        final float[] best = {Float.NEGATIVE_INFINITY};
        final Object[] which = new Object[1];
        //TODO if (y>=best[0]) collect several if they are of equal rank, then choose randomly. better yet make this a feature of Bag
        forEach(x -> {
            float y = rank.floatValueOf(x);
            if (y > best[0]) {
                best[0] = y;
                which[0] = x;
            }
        });
        return (V) which[0];
    }

    @FunctionalInterface
    interface EarlyFailRanker<X> {
        /**
         * return the computed value or Float.NaN if it
         * terminated early by deciding that the currently calculated
         * value would never exceed the provided current best,
         * which is initialized to NEGATIVE_INFINITY
         */
        public float floatValueOf(X x, float mustExceed);
    }

    @Nullable
    default V maxBy(EarlyFailRanker<V> rank) {
        final float[] best = {Float.NEGATIVE_INFINITY};
        final Object[] which = new Object[1];
        forEach(x -> {
            float y = rank.floatValueOf(x, best[0]);
            if ((y == y) && (y > best[0])) {
                best[0] = y;
                which[0] = x;
            }
        });
        return (V) which[0];
    }

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    @Override
    int size();


    /**
     * when adjusting the priority of links directly, to correctly absorb the pressure difference, call this
     */
    default void pressurize(float f) {

    }

    default float priAvg() {
        int s = size();
        return (s == 0) ? 0 : (priSum() / s);
    }


//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public Consumer<V> getOnRemoval() {  return null;    }
//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public void setOnRemoval(Consumer<V> onRemoval) { }

    /**
     * iterates all items in (approximately) descending priority
     * forEach may be used to avoid allocation of iterator instances
     */
    @NotNull
    @Override
    Iterator<V> iterator();

    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    default boolean contains(@NotNull K it) {
        return get(it) != null;
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    //    /**
//     * if the next item is true via the predicate, then it is TAKEn out of the bag; otherwise the item remains unaffected
//     */
//    public final V remove(final Predicate<V> iff) {
//        V v = peekNext();
//
//        if (v == null) return null;
//        if (iff.apply(v)) {
//            remove(v.name());
//            return v;
//        }
//        return null;
//    }

    /**
     * @return null if this is an event which was rejected on input, non-null if it was a re
     */
    default void onRemoved(@NotNull V value) {

    }

    default void onAdded(@NotNull V v) {

    }

    /**
     * returns the priority of a value, or NaN if such entry is not present
     */
    float pri(@NotNull V key);

    default float pri(@NotNull Object key, float ifMissing) {
        V x = get(key);
        if (x == null)
            return ifMissing;
        else
            return priSafe(x, ifMissing);
    }

    default boolean active(@NotNull V key) {
        float x = pri(key);
        return (x==x);
        //return priSafe(key, -1) >= 0;
    }

    default float priSafeOrZero(@NotNull V key) {
        return priSafe(key, 0);
    }


    default float priSafe(@NotNull V key, float valueIfMissing) {
        float p = pri(key);
        return (p == p) ? p : valueIfMissing;
    }


    /**
     * resolves the key associated with a particular value
     */
    @NotNull K key(V value);


    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream p) {
        forEach(p::println);
    }

    /**
     * should visit items highest priority first, if possible.
     * for some bags this may not be possible.
     */
    //by default this will use iterator().forEach() but this can be used to make sure each implementation offers its best
    //@Override abstract public void forEach(final Consumer<? super V> action);
    default float priSum() {
        float[] total = {0};
        forEach(v -> total[0] = total[0] + priSafe(v, 0));
        return total[0];
    }


//    final public int forgetNext(float forgetCycles, final V[] batch, final long now) {
//        return forgetNext(forgetCycles, batch, 0, batch.length, now, batch.length/2 /* default to max 1.5x */);
//    }

//    /** warning: slow */
//    public double getStdDev(StandardDeviation target) {
//        target.clear();
//        forEachEntry(x -> target.increment(x.getPriority()));
//        return target.getResult();
//    }


    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMin() {
        float[] min = {Float.POSITIVE_INFINITY};
        forEach(b -> {
            float p = priSafe(b, Float.POSITIVE_INFINITY);
            if (p < min[0]) min[0] = p;
        });
        float m = min[0];
        if (Float.isFinite(m)) return m;
        else return 0;
    }

    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMax() {
        float[] max = {Float.NEGATIVE_INFINITY};
        forEach(b -> {
            float p = priSafe(b, Float.NEGATIVE_INFINITY);
            if (p > max[0]) max[0] = p;
        });
        float m = max[0];
        if (Float.isFinite(m)) return m;
        else return 0;
    }


    @Override
    void setCapacity(int c);


//    /**
//     * for bags which maintain a separate name index from the items, more fine-granied access methods to avoid redundancy when possible
//     */
//    @Deprecated
//    abstract public static class IndexedBag<E extends Item<K>, K> extends Bag<K, E> {
//
//        public E TAKE(final K key) {
//            return take(key, true);
//        }
//
//
//        /**
//         * registers the item
//         */
//        abstract protected void index(E value);
//
//        /**
//         * unregisters it
//         */
//        abstract protected E unindex(K key);
//
//        protected E unindex(E e) {
//            return unindex(e.name());
//        }
//
//        abstract public E take(final K key, boolean unindex);
//
//        public E TAKE(E value) {
//            return TAKE(value.name());
//        }
//
//
//        /**
//         * Insert an item into the itemTable, and return the overflow
//         *
//         * @param newItem The Item to put in
//         * @return null if nothing was displaced and if the item itself replaced itself,
//         * or the The overflow Item if a different item had to be removed
//         */
//        abstract protected E addItem(final E newItem, boolean index);
//
//        public E PUT(final E newItem) {
//            return addItem(newItem, true);
//        }
//
//
//        /**
//         * Add a new Item into the Bag via a BagSelector interface for lazy or cached instantiation of Bag items
//         *
//         * @return the item which was removed, which may be the input item if it could not be inserted; or null if nothing needed removed
//         * <p/>
//         * WARNING This indexing-avoiding version not completely working yet, so it is not used as of this commit
//         */
//
//        public E putInFast(BagSelector<K, E> selector) {
//
//            E item = take(selector.name(), false);
//
//            if (item != null) {
//                item = (E) item.merge(selector);
//                final E overflow = addItem(item, false);
//                if (overflow == item) {
//                    unindex(item.name());
//                }
//                return overflow;
//            } else {
//                item = selector.newItem();
//
//                //compare by reference, sanity check
//                if (item.name() != selector.name())
//                    throw new RuntimeException("Unrecognized selector and resulting new instance have different name()'s: item=" + item.name() + " selector=" + selector.name());
//
//                // put the (new or merged) item into itemTable
//                return PUT(item);
//            }
//
//
//        }
//    }


    @NotNull
    default double[] priHistogram(@NotNull double[] x) {
        int bins = x.length;
        forEach(budget -> {
            float p = priSafe(budget, 0);
            int b = Util.bin(p, bins - 1);
            x[b]++;
        });
        double total = 0;
        for (double e : x) {
            total += e;
        }
        if (total > 0) {
            for (int i = 0; i < bins; i++)
                x[i] /= total;
        }
        return x;
    }

    @NotNull
    public static double[] priHistogram(Iterable<? extends PriReference> pp, @NotNull double[] x) {
        int bins = x.length;
        final double[] total = {0};

        pp.forEach(y -> {
            float p = y.priSafe(0);
            if (p > 1f) p = 1f; //just to be safe
            int b = Util.bin(p, bins);
            x[b]++;
            total[0]++;
        });

        double t = total[0];
        if (t > 0) {
            for (int i = 0; i < bins; i++)
                x[i] /= t;
        }
        return x;
    }

    /**
     * double[histogramID][bin]
     */
    @NotNull
    public static <X, Y> double[][] histogram(@NotNull Iterable<PriReference<Y>> pp, @NotNull BiConsumer<PriReference<Y>, double[][]> each, @NotNull double[][] d) {

        pp.forEach(y -> {
            each.accept(y, d);
            //float p = y.priSafe(0);
            //x[Util.bin(p, bins - 1)]++;
        });

        for (double[] e : d) {
            double total = 0;
            for (int i = 0, eLength = e.length; i < eLength; i++) {
                total += e[i];
            }
            if (total > 0) {
                for (int i = 0, eLength = e.length; i < eLength; i++) {
                    double f = e[i];
                    e[i] /= total;
                }
            }
        }

        return d;
    }

    @Deprecated
    Bag<K, V> commit();


    /**
     * commits the next set of changes and updates budgeting
     *
     * @return this bag
     */
    @NotNull Bag<K, V> commit(Consumer<V> update);


    @Nullable Bag EMPTY = new Bag() {

        @NotNull
        @Override
        public Bag sample(@NotNull Bag.@NotNull BagCursor each, boolean pop) {
            return this;
        }

        @Override
        public void clear() {
        }

        @Override
        public Object key(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float pri(@NotNull Object key) {
            return 0;
        }


        @Nullable
        @Override
        public Object remove(@NotNull Object x) {
            return null;
        }

        @Override
        public Object put(@NotNull Object b, @Nullable MutableFloat overflowing) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @NotNull
        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(@NotNull Object it) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }


        @Override
        public void setCapacity(int c) {

        }

        @Override
        public Bag commit() {
            return this;
        }

        @NotNull
        @Override
        public Bag commit(Consumer update) {
            return this;
        }


        @Nullable
        @Override
        public Object get(@NotNull Object key) {
            return null;
        }

        @Override
        public void forEachKey(@NotNull Consumer each) {

        }

        @Override
        public int capacity() {
            return 0;
        }

    };


//    @NotNull
//    default <Y> PLink<Y> newLink(@NotNull Y i) {
//
//        if (i instanceof Budgeted)
//            return new DependentBLink((Budgeted) i);
//        else
//            return new DefaultBLink(i);
//    }

    @Override
    default void forEachKey(Consumer<? super K> each) {
        forEach(b -> each.accept(key(b)));
    }

    default Bag<K, V> capacity(int i) {
        setCapacity(i);
        return this;
    }


//    default boolean putIfAbsent(V b) {
//        K x = b.get();
//        if (contains(x))
//            return false;
//        if (put(b, 1f, null)==null)
//            return false;
//        return true;
//    }


    //TODO default @NotNull Bag<V> move(int limit, @NotNull Bag target) {

}
