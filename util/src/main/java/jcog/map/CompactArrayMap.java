package jcog.map;

import jcog.TODO;
import jcog.list.FasterList;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Optimised for very small data sets, allowing compact size and fast puts at
 * the expense of O(n) lookups. This implementation may store duplicate entries
 * for the same key. TODO: explain more.
 *
 * @author The Stajistics Project
 */
public class CompactArrayMap<K, V> extends AbstractMap<K, V> implements Serializable {

    protected FasterList entries;

    public CompactArrayMap() {
        this(0);
    }

    public CompactArrayMap(int initialCapacity) {
        if (initialCapacity == 0)
            entries = null;
        else
            entries = new FasterList<>(initialCapacity);
    }

    public CompactArrayMap(Map<K, V> map) {
        this(map.size());
        putAll(map);
    }

    @Override
    public int size() {
        FasterList e = this.entries;
        return e != null ? e.size() : 0;
    }



    @Override
    public boolean containsValue(Object aValue) {
        throw new TODO();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }


    @Override
    public V get(Object key) {
        FasterList x = this.entries;
        if (x != null) {
            int s = x.size();
            Object[] a = x.array();
            for (int i = 0; i < s; ) {
                if (keyEquals(a[i], key))
                    return (V) a[i + 1];
                i += 2;
            }
        }
        return null;
    }


    /**
     * Note: contract broken! Always returns null.
     */
    @Override
    public synchronized V put(K key, V value) {
        FasterList x = entries;
        if (x == null) {
            x = new FasterList(1);
            this.entries = x;
        } else {
            int s = x.size();
            Object[] a = x.array();
            for (int i = 0; i < s; ) {
                if (keyEquals(a[i], key)) {
                    a[i+1] = value;
                    return null;
                }
                i += 2;
            }
        }
        x.add(key);
        x.add(value);
        return null;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V e = get(key);
        if (e!=null)
            return e;

        V v = mappingFunction.apply(key);
        put(key, v);
        return v;
    }

    @Override
    public V remove(Object key) {
        throw new TODO();
//        int i = indexOf(key);
//        if (i != -1) {
//
//        }
//        return null;
    }

    public boolean keyEquals(Object a, Object b) {
        return a.equals(b);
    }

    @Override
    public void clear() {
        entries = null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new TODO();
    }
}
