package nars.bag;

import jcog.Util;
import jcog.table.Table;
import nars.Param;
import nars.attention.Forget;
import nars.link.BLink;
import nars.link.PLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * K=key, V = item/value of type Item
 * <p>
 * TODO remove unnecessary methods, documetn
 * TODO implement java.util.Map interface
 */
public interface Bag<K,V extends PLink<K>> extends Table<K, V>, Iterable<V> {


    /**
     * returns the bag to an empty state
     */
    @Override
    void clear();

    /**
     * gets the next value without removing changing it or removing it from any index.  however
     * the bag is cycled so that subsequent elements are different.
     */
    @Nullable default V sample() {
        PLink[] result = new PLink[1];
        sample(1, (x) -> { result[0] = x; return true; } );
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
        return put(x, 1f, null);
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



    @Deprecated @Override default @Nullable V put(@NotNull K i, @NotNull V b) {
        assert(b.get().equals(i));
        return put(i, b, 1f, null);
    }


    default V put(@NotNull V b, float scale, @Nullable MutableFloat overflowing) {
        return put(b.get(), b, scale, overflowing);
    }

    @Deprecated V put(@NotNull K i, @NotNull V b, float scale, @Nullable MutableFloat overflowing);




//    /**
//     * iterates in sequence starting from the top until predicate returns false, limited by max iterations (n)
//     */
//    abstract public void top(int n, Predicate<BagBudget<V>> each);
//        int[] toFire = { n };
//        top(c -> (each.test(c) && (toFire[0]--) > 0));




    /**
     * fills a collection with at-most N items, if an item passes the predicate.
     * returns how many items added
     */
    @NotNull
    Bag<K,V> sample(int n, @NotNull Predicate<? super V> target);


    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    @Override int size();



    default float priAvg() {
        int s = size();
        if (s == 0) return 0;
        return priSum() / s;
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
    boolean contains(@NotNull K it);

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

    default void onAdded(V v) {

    }

    default float pri(@NotNull Object x, float valueIfMissing) {
        V y = get(x);
        if (y==null || y.isDeleted())
            return valueIfMissing;
        return y.pri();
    }



//    /**
//     * implements the Supplier<V> interface; invokes a remove()
//     */
//    @Nullable
//    @Override
//    default PLink<V> get() {
//        return pop();
//    }



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
        forEach(v -> total[0] += v.pri());
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
     * slow, probably want to override in subclasses
     */
    default float priMin() {
        float[] min = {Float.POSITIVE_INFINITY};
        forEach(b -> {
            float p = b.pri();
            if (p < min[0]) min[0] = p;
        });
        return min[0];
    }

    /**
     * slow, probably want to override in subclasses
     */
    default float priMax() {
        float[] max = {Float.NEGATIVE_INFINITY};
        forEach(b -> {
            float p = b.pri();
            if (p > max[0]) max[0] = p;
        });
        return max[0];
    }


    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    default void forEach(int max, @NotNull Consumer<? super V> action) {

        Iterator<V> ii = iterator();
        int n = 0;
        while (ii.hasNext() && (n++ < max)) {
            action.accept(ii.next());
        }

    }


    @Override
    boolean setCapacity(int c);


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
            float p = budget.pri();
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



    @Deprecated default Bag<K,V> commit() {
        return commit((b)->{
            float p = b instanceof ArrayBag ? ((ArrayBag) b).pressure : ((HijackBag)b).pressure; //HACK
            float m = b instanceof ArrayBag ? ((ArrayBag) b).mass(): ((HijackBag)b).mass; //HACK
            return forget(b.size(), p, m);
        });
    }

    @Nullable
    default Forget forget(int s, float p, float m) {

        float r = p > 0 ?
                -((s * Param.BAG_THRESHOLD) - p - m) / m :
                0;

        //float pressurePlusOversize = pressure + Math.max(0, expectedAvgMass * size - existingMass);
        //float r = (pressurePlusOversize) / (pressurePlusOversize + existingMass*4f /* momentum*/);

        //System.out.println(pressure + " " + existingMass + "\t" + r);
        return r >= Param.BUDGET_EPSILON ? forget(Util.unitize(r)) : null;
    }

    default Forget forget(float rate) {
        return new Forget(rate);
    }

    /**
     * commits the next set of changes and updates budgeting
     * @return this bag
     */
    @NotNull Bag<K,V> commit(Function<Bag,Consumer<BLink>> update);


    @Nullable Bag EMPTY = new Bag() {

        @Override
        public void clear() {

        }

        @Nullable
        @Override
        public BLink sample() {
            return null;
        }

        @Nullable
        @Override
        public BLink remove(@NotNull Object x) {
            return null;
        }

        @Override
        public PLink put(@NotNull Object i, @NotNull PLink b, float scale, @Nullable MutableFloat overflowing) {
            return null;
        }


        @Override public BLink add(Object c, float x) {
            return null;
        }


        @Override
        public BLink mul(Object key, float boost) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @NotNull
        @Override
        public Iterator<BLink> iterator() {
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

        @NotNull
        @Override
        public Bag commit() {
            return this;
        }

        @Override
        public boolean setCapacity(int c) {
            return false;
        }


        @NotNull
        @Override
        public Bag commit(Function update) {
            return this;
        }

        @NotNull
        @Override
        public Bag sample(int n, @NotNull Predicate target) {
            return this;
        }

        @Nullable
        @Override
        public Object get(@NotNull Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object put(@NotNull Object o, @NotNull Object o2) {
            return null;
        }

        @Override
        public void forEachKey(@NotNull Consumer each) {

        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void forEachWhile(@NotNull Predicate each, int n) {

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
        forEach(b -> each.accept(b.get()));
    }


    /** if key is present, adds a priority amount (quality unaffected) */
    @Nullable V add(Object key, float toAdd);

    /** if key is present, applies a priority multiplier factor, and returns the link */
    @Nullable V mul(Object key, float factor);

    /** samples and removes the sampled item. returns null if bag empty, or for some other reason the sample did not succeed  */
    @Nullable default V pop() {
        V x = sample();
        if (x!=null) {
            K k = x.get();
            remove(k);
            return x;
        }
        return null;
    }

//    /** apply a transformation to each value. if the function returns null, it indicates
//     * the link is to be removed.  if it returns the original value, no change for that link.
//     * changes are buffered until after list iteration completes.
//     */
//    default void compute(@NotNull Function<V,V> o) {
//        List<V[]> changed = $.newArrayList(size());
//        forEach(x -> {
//            V y;
//
//            if (x.isDeleted())
//                y = null;
//            else
//                y = o.apply(x);
//
//           if (y!=x) {
//               changed.add(new BLink[]{ x, y });
//           }
//        });
//        for (V[] c : changed) {
//            remove(c[0].get());
//
//            V toAdd = c[1];
//            if (toAdd!=null)
//                putLink(toAdd);
//        }
//    }

    default Bag<K,V> copy(@NotNull Bag target, int limit) {
        return this.sample(limit, t -> {
            target.put(t);
            return true; //assume it worked
        });
    }

    default Bag<K,V> capacity(int i) {
        setCapacity(i);
        return this;
    }

    default boolean putIfAbsent(V b) {
        K x = b.get();
        if (contains(x))
            return false;
        if (put(b, 1f, null)==null)
            return false;
        return true;
    }


    //TODO default @NotNull Bag<V> move(int limit, @NotNull Bag target) {

}
