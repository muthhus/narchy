//package nars.bag.impl;
//
//import nars.bag.WeakBudget;
//import nars.budget.RawBudget;
//import nars.budget.merge.BudgetMerge;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.lang.ref.ReferenceQueue;
//import java.util.AbstractMap;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Consumer;
//
///**
// * Created by me on 7/2/16.
// */
//public class WeakBudgetMap<X> extends AbstractMap implements Map {
//
//    /**
//     * Returns a set of the mappings contained in this hash table.
//     */
//    @NotNull
//    @Override
//    public Set entrySet() {
//        processQueue();
//        return hash.entrySet();
//    }
//
//    /* Hash table mapping WeakKeys to values */
//    @Nullable
//    private Map<X, WeakBudget<X>> hash;
//
//    /* Reference queue for cleared WeakKeys */
//    @Nullable
//    private ReferenceQueue queue = new ReferenceQueue();
//
//    /*
//     * Remove all invalidated entries from the map, that is, remove all entries
//     * whose values have been discarded.
//     */
//    private void processQueue() {
//        WeakBudget ref;
//        ReferenceQueue q = this.queue;
//        Map h = this.hash;
//        while ((ref = (WeakBudget) q.poll()) != null) {
//            Object rr = ref.get();
//
////                if (ref == (WeakBudget) h.get(rr)) {
////                    // only remove if it is the *exact* same WeakValueRef
////                    //
//            h.remove(rr);
//            //}
//        }
//    }
//
///* -- Constructors -- */
//
//    /**
//     * Constructs a new, empty <code>WeakHashMap</code> with the given initial
//     * capacity and the given load factor.
//     *
//     * @param initialCapacity The initial capacity of the <code>WeakHashMap</code>
//     * @param loadFactor      The load factor of the <code>WeakHashMap</code>
//     * @throws IllegalArgumentException If the initial capacity is less than zero, or if the load
//     *                                  factor is nonpositive
//     */
//    public WeakBudgetMap(@NotNull Map<X, WeakBudget<X>> internal) {
//        this.hash = internal;
//        if (!internal.isEmpty())
//            putAll(internal);
//    }
//
//
//    /**
//     * Returns the number of key-value mappings in this map. <strong>Note:</strong>
//     * <em>In contrast with most implementations of the
//     * <code>Map</code> interface, the time required by this operation is
//     * linear in the size of the map.</em>
//     */
//    @Override
//    public int size() {
//        processQueue();
//        return hash.size();
//    }
//
//
//    public final void forEachBudget(@NotNull Consumer<WeakBudget<X>> action) {
//        hash.forEach((k, v) -> action.accept(v));
//    }
//
//    /**
//     * Returns <code>true</code> if this map contains no key-value mappings.
//     */
//    @Override
//    public boolean isEmpty() {
//        processQueue();
//        return hash.isEmpty();
//    }
//
//    /**
//     * Returns <code>true</code> if this map contains a mapping for the
//     * specified key.
//     *
//     * @param key The key whose presence in this map is to be tested.
//     */
//    @Override
//    public boolean containsKey(Object key) {
//        processQueue();
//        return hash.containsKey(key);
//    }
//
///* -- Lookup and modification operations -- */
//
//    /**
//     * Returns the value to which this map maps the specified <code>key</code>.
//     * If this map does not contain a value for this key, then return
//     * <code>null</code>.
//     *
//     * @param key The key whose associated value, if any, is to be returned.
//     */
//    @Nullable
//    @Override
//    public X get(Object key) {
//        processQueue();
//        WeakBudget<X> ref = (WeakBudget) hash.get(key);
//        if (ref != null)
//            return ref.get();
//        return null;
//    }
//
//    /**
//     * Updates this map so that the given <code>key</code> maps to the given
//     * <code>value</code>. If the map previously contained a mapping for
//     * <code>key</code> then that mapping is replaced and the previous value
//     * is returned.
//     *
//     * @param key   The key that is to be mapped to the given <code>value</code>
//     * @param value The value to which the given <code>key</code> is to be
//     *              mapped
//     * @return The previous value to which this key was mapped, or
//     * <code>null</code> if if there was no mapping for the key
//     */
//    @Nullable
//    public Object put(X key, float p, float d, float q, @NotNull BudgetMerge mergeFunction) {
//        processQueue();
//
//        WeakBudget<X> rtn = hash.get(key);
//        if (rtn == null) {
//            hash.put(key, rtn = new WeakBudget<>(key, queue, p, d, q));
//        } else {
//            mergeFunction.merge(rtn, new RawBudget(p, d, q), 1f);
//        }
//        return rtn;
//    }
//
//    /**
//     * Removes the mapping for the given <code>key</code> from this map, if
//     * present.
//     *
//     * @param key The key whose mapping is to be removed.
//     * @return The value to which this key was mapped, or <code>null</code> if
//     * there was no mapping for the key.
//     */
//    @Override
//    public Object remove(Object key) {
//        processQueue();
//        return hash.remove(key);
//    }
//
//    /**
//     * Removes all mappings from this map.
//     */
//    @Override
//    public void clear() {
//        processQueue();
//        hash.clear();
//    }
//
//    public void delete() {
//        queue = null;
//        hash = null;
//    }
//
//    private float sum;
//
//    public final float mass() {
//
//        sum = 0;
//
//        processQueue();
//
//        if (!hash.isEmpty()) {
//            hash.forEach((k, b) -> {
//                sum += b.priIfFiniteElseZero();
//            });
//        }
//
//        return sum;
//
//    }
//}
