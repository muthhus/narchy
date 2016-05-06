package nars.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;


/**
 * adapter to a Map for coordinating changes in a Map with another Collection
 */
public abstract class CollectorMap<K, V>  {

    private final Map<K, V> map;

    protected CollectorMap(Map<K, V> map) {
        this.map = map;
    }

    abstract public K key(V v);

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * implementation for adding the value to another collecton (called internally)
     */
    protected abstract V addItem(V e);

    /**
     * implementation for removing the value to another collecton (called internally)
     */
    @Nullable
    protected abstract V removeItem(V e);

    public final void forEach(@NotNull BiConsumer<K, V> each) {
        map.forEach(each);
    }


//    @Nullable
//    public V putIfAbsent(K key, V value) {
//        V existing = putKeyIfAbsent(key, value);
//        if (existing != null) {
//            return existing;
//        }
//        addItem(value);
//        return null;
//    }



    public final V put(K key, V value) {

        /*synchronized (nameTable)*/
        V removed = putKey(key, value);
        if (removed != null) {

            if (removed == value) {
                //rejected input
                return value;
            }
            else {
                //displaced other
                V remd = removeItem(removed);
                if (remd == null)
                    throw new RuntimeException("unable to remove item corresponding to key " + key);

            }
        }

        V displaced = addItem(value);

        if (removed != null && displaced != null) {
            throw new RuntimeException("Only one item should have been removed on this insert; both removed: " + removed + ", " + displaced);
        }
        if (displaced != null) { //&& (!key(removed2).equals(key))) {
            removeKeyForValue(displaced);
            removed = displaced;
        }


        return removed;
    }

    @Nullable
    public V remove(K x) {

        V e = removeKey(x);
        if (e != null) {
            V removed = removeItem(e);
//            if (removed == null) {
//                /*if (Global.DEBUG)
//                    throw new RuntimeException(key + " removed from index but not from items list");*/
//                //return null;
//            }
            if (removed != e) {
                removeItem(e);
                throw new RuntimeException(x + " removed " + e + " but item removed was " + removed);
            }
            return removed;
        }

        return null;
    }


    public final V removeKey(K key) {
        return map.remove(key);
    }

    public final V removeKeyForValue(V value) {
        return removeKey(key(value));
    }

    public int size() {
        return map.size();
    }

//    public final boolean containsValue(V it) {
//        return map.containsValue(it);
//    }

    public void clear() {
        map.clear();
    }

    public final V get(@NotNull Object key) {
        return map.get(key);
    }

    public boolean containsKey(K name) {
        return map.containsKey(name);
    }

    @NotNull
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    public Collection<V> values() {
        return map.values();
    }


    /**
     * put key in index, do not add value
     */
    protected final V putKey(K key, V value) {
        return map.put(key, value);
    }
//    public final V putKeyIfAbsent(K key, V value) {
//        return map.putIfAbsent(key, value);
//    }


}
