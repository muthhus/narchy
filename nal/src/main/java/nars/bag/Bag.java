package nars.bag;

import nars.$;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.link.DependentBLink;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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
    @Nullable default BLink<V> sample() {
        BLink<V>[] result = new BLink[1];
        sample(1, (x) -> { result[0] = x; return true; } );
        return result[0];
    }


    /**
     * TODO rename 'remove'
     *
     * @param key
     * @return
     */
    @Override
    @Nullable
    BLink<V> remove(@NotNull V x);


    /**
     * insert/merge with an initial / default budget
     */
    default void put(@NotNull V x) {
        put(x, initialBudget(x), null);
    }
    default void putLink(@NotNull BLink<V> x) {
        put(x.get(), x);
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
        return Budget.Zero;
//
//        return new BLink(v, 0,0,0);
    }

    /** always returns null, which is different semantics than the supermethod it overrides */
    @Override default @Nullable BLink<V> put(@NotNull V i, @NotNull BLink<V> b) {
        put(i, b, 1f, null);
        return null;
    }

    default void put(@NotNull V i, @NotNull Budgeted b) {
        put(i, b, 1f, null);
    }


    default void put(@NotNull V i, @NotNull Budgeted b, @Nullable MutableFloat overflowing) {
        put(i, b, 1f, overflowing);
    }

    default void put(@NotNull V v, @NotNull BLink<V> b, @Nullable MutableFloat overflowing) {
        put(v, b, 1f, overflowing);
    }


    void put(@NotNull V i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing);




//    /**
//     * iterates in sequence starting from the top until predicate returns false, limited by max iterations (n)
//     */
//    abstract public void top(int n, Predicate<BagBudget<V>> each);
//        int[] toFire = { n };
//        top(c -> (each.test(c) && (toFire[0]--) > 0));


    @NotNull default Bag<V> sample(float percent, @NotNull Predicate<? super BLink<V>> target) {
        int n = (int)Math.ceil(percent * size());
        sample(n, target);
        return this;
    }

    /**
     * fills a collection with at-most N items, if an item passes the predicate.
     * returns how many items added
     */
    @NotNull
    Bag<V> sample(int n, @NotNull Predicate<? super BLink<V>> target);


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
    @NotNull
    @Override
    Iterator<BLink<V>> iterator();

    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    boolean contains(@NotNull V it);

    boolean isEmpty();

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
    default void onRemoved(@NotNull BLink<V> value) {

    }

    default void onAdded(BLink<V> v) {

    }

    default float pri(@NotNull V x, float valueIfMissing) {
        BLink y = get(x);
        if (y==null || y.isDeleted())
            return valueIfMissing;
        return y.pri();
    }

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


//    @NotNull
//    default Bag<V> forEachThen(@NotNull Consumer<BLink<? extends V>> each) {
//        forEach(each);
//        return this;
//    }

    default void putAll(@NotNull Collection<? extends V> a) {
        a.forEach(this::put);
    }

    @NotNull Bag<V> commit(@Nullable Consumer<BLink> each);



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
        public void put(@NotNull Object i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing) {
        }

        @Override
        public void put(ObjectFloatHashMap values, Budgeted in, float scale, MutableFloat overflow) {

        }

        @Override public Object add(Object c, float x) {
            return null;
        }


        @Override
        public Object mul(Object key, float boost) {
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
        public Bag commit(@Nullable Consumer each) {
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
        public void topWhile(@NotNull Predicate each, int n) {

        }


    };

    @NotNull
    default <Y> BLink<Y> newLink(@NotNull Y i, @NotNull Budgeted b) {

        if (i instanceof Budgeted)
            return new DependentBLink((Budgeted) i, b);
        else
            return new DefaultBLink(i, b);
    }

    @NotNull
    default <Y> BLink<Y> newLink(@NotNull Y i) {

        if (i instanceof Budgeted)
            return new DependentBLink((Budgeted) i);
        else
            return new DefaultBLink(i);
    }

    @Override
    default void forEachKey(Consumer<? super V> each) {
        forEach(b -> each.accept(b.get()));
    }

    default void put(@NotNull ObjectFloatHashMap<? extends V> values, @NotNull Budgeted in,/*, MutableFloat overflow*/float scale, MutableFloat overflow) {

        ObjectFloatProcedure<V> p = (k, v) -> {
            put(k, in, v * scale, overflow);
        };

        values.forEachKeyValue(p);
    }


    @Nullable V add(Object key, float x);

    /** gets the link if present, applies a priority multiplier factor, and returns the link */
    @Nullable V mul(Object key, float factor);

    /** samples and removes the sampled item. returns null if bag empty, or for some other reason the sample did not succeed  */
    @Nullable default BLink<V> pop() {
        BLink<V> x = sample();
        if (x!=null) {
            V v = x.get();
            if (v!=null) {
                remove(v);
                return x;
            }
        }
        return null;
    }

    /** apply a transformation to each value. if the function returns null, it indicates
     * the link is to be removed.  if it returns the original value, no change for that link.
     * changes are buffered until after list iteration completes.
     */
    default void compute(@NotNull Function<BLink<V>,BLink<V>> o) {
        List<BLink<V>[]> changed = $.newArrayList(size());
        forEach(x -> {
            BLink<V> y;

            if (x.isDeleted())
                y = null;
            else
                y = o.apply(x);

           if (y!=x) {
               changed.add(new BLink[]{ x, y });
           }
        });
        for (BLink<V>[] c : changed) {
            remove(c[0].get());

            BLink<V> toAdd = c[1];
            if (toAdd!=null)
                putLink(toAdd);
        }
    }

    default @NotNull Bag<V> transfer(int maxItemsToSend, @NotNull Bag target) {
        return this.sample(maxItemsToSend, t -> {
            target.putLink(t);
            return true; //assume it worked
        });
    }


}
