package nars.bag;

import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * K=key, V = item/value of type Item
 * <p>
 * TODO remove unnecessary methods, documetn
 * TODO implement java.util.Map interface
 */
public interface Bag<V> extends Table<V, BLink<V>>, Consumer<V>, Iterable<BLink<V>> {





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
    BLink<V> sample();


    /**
     * TODO rename 'remove'
     *
     * @param key
     * @return
     */
    @Nullable
    @Override
    BLink<V> remove(@NotNull V x);


    /**
     * insert/merge with an initial / default budget
     */
    @Nullable
    default BLink<V> put(@NotNull V x) {
        return put(x, initialBudget(x), null);
    }
//        @Nullable
//        @Override public BLink<V> put(@NotNull V v) {
//            //TODO combine with CurveBag.put(v)
//            BLink<V> existing = get(v);
//            if (existing!=null) {
//                merge(existing, getDefaultBudget(v), 1f);
//                return existing;
//            } else {
//            }
//        }
//        @Nullable
//        @Override public BLink<V> put(@NotNull V v) {
//            BLink<V> existing = get(v);
//            return (existing != null) ?
//                    existing :
//                    put(v, getDefaultBudget(v));
//        }


    @NotNull
    default Budgeted initialBudget(@NotNull V v) {
        if (v instanceof Budgeted)
            return ((Budgeted)v);
        return UnitBudget.Zero;
//
//        return new BLink(v, 0,0,0);
    }

    @Override @Nullable
    default BLink<V> put(@NotNull V i, @NotNull BLink<V> b) {
        return put(i, b, 1f, null);
    }

    @Nullable
    default BLink<V> put(@NotNull V i, @NotNull Budgeted b) {
        return put(i, b, 1f, null);
    }


    @Nullable
    default BLink<V> put(@NotNull V i, @NotNull Budgeted b, @Nullable MutableFloat overflowing) {
        return put(i, b, 1f, overflowing);
    }

    @Nullable
    default BLink<V> put(@NotNull V v, @NotNull BLink<V> b, @Nullable MutableFloat overflowing) {
        return put(v, b, 1f, overflowing);
    }


    @Nullable
    BLink<V> put(@NotNull V i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing);




//    /**
//     * iterates in sequence starting from the top until predicate returns false, limited by max iterations (n)
//     */
//    abstract public void top(int n, Predicate<BagBudget<V>> each);
//        int[] toFire = { n };
//        top(c -> (each.test(c) && (toFire[0]--) > 0));


    @NotNull default Bag<V> sample(float percent, @NotNull Consumer<? super BLink<V>> target) {
        int n = (int)Math.ceil(percent * size());
        sample(n, target);
        return this;
    }

    @NotNull
    Bag<V> sample(int n, @NotNull Consumer<? super BLink<V>> target);
//    /**
//     * fills a collection with at-most N items, if an item passes the predicate.
//     * returns how many items added
//     */
//    public int sample(int n, Predicate<BagBudget<V>> each, Collection<BagBudget<V>> target) {
//        int startSize = target.size();
//        sample(n, x -> {
//            if (each.test(x)) {
//                target.add(x);
//            }
//            return true;
//        });
//        return target.size() - startSize;
//    }



//    /**
//     * Get an Item by key
//     *
//     * @param key The key of the Item
//     * @return The Item with the given key
//     */
//    @Override
//    public abstract V get(K key);

//    public abstract Set<K> keySet();


//    /**
//     * Choose an Item according to distribution policy and take it out of the Bag
//     * TODO rename removeNext()
//     *
//     * @return The selected Item, or null if this bag is empty
//     */
//    @Nullable
//    BLink<V> pop();

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

    default float summaryAvg() {
        int s = size();
        if (s == 0) return 0;
        return summarySum() / s;
    }

//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public Consumer<V> getOnRemoval() {  return null;    }
//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public void setOnRemoval(Consumer<V> onRemoval) { }

    /**
     * iterates all items in (approximately) descending priority
     * forEach may be used to avoid allocation of iterator instances
     */
    @Nullable
    @Override
    Iterator<BLink<V>> iterator();

    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    boolean contains(@NotNull V it);


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
     * commits the next set of changes and updates any sorting
     * should return this bag
     */
    @NotNull
    Bag<V> commit();

    /**
     * implements the Consumer<V> interface; invokes a put()
     */
    @Override
    default void accept(@NotNull V v) {
        put(v);
    }

//    /**
//     * implements the Supplier<V> interface; invokes a remove()
//     */
//    @Nullable
//    @Override
//    default BLink<V> get() {
//        return pop();
//    }



    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream p) {
        forEach(b -> p.println(b.toBudgetString() + ' ' + b.get()));
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

    default float summarySum() {
        float[] total = {0};
        forEach(v -> total[0] += v.summary());
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
    default void forEach(int max, @NotNull Consumer<? super BLink<V>> action) {

        Iterator<BLink<V>> ii = iterator();
        int n = 0;
        while (ii.hasNext() && (n++ < max)) {
            action.accept(ii.next());
        }

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
    default double[] getPriorityHistogram(int bins) {
        return getPriorityHistogram(new double[bins]);
    }

    @NotNull
    default double[] getPriorityHistogram(@NotNull double[] x) {
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


    @Nullable
    Bag<V> filter(Predicate<BLink> forEachIfFalseThenRemove);


//    @NotNull
//    default Bag<V> forEachThen(@NotNull Consumer<BLink<? extends V>> each) {
//        forEach(each);
//        return this;
//    }

    default void putAll(@NotNull Collection<? extends V> a) {
        a.forEach(this::put);
    }

    default Bag<V> commit(@NotNull Consumer<BLink> each) {
        forEach(each);
        commit();
        return this;
    }

}
