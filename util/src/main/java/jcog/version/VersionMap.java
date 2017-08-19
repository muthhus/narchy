package jcog.version;

import jcog.list.ArrayUnenforcedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;


public class VersionMap<X, Y> extends AbstractMap<X, Y> {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;
    public final int elementStackSizeDefault; //stackSizePerElement


    public VersionMap(Versioning context) {
        this(context, 0);
    }

    public VersionMap(Versioning context, int mapCap) {
        this(context, mapCap, 0);
    }

    /**
     * @param context
     * @param mapCap  initial capacity of map (but can grow
     * @param eleCap  initial capacity of map elements (but can grow
     */
    public VersionMap(Versioning context, int mapCap, int eleCap) {
        this(context,
                new HashMap(mapCap)
                //new LinkedHashMap<>(elementStackSizeDefault)
                //new UnifiedMap(elementStackSizeDefault)
                , eleCap
        );
    }

    public VersionMap(Versioning<Y> context, Map<X, Versioned<Y>/*<Y>*/> map, int elementStackSizeDefault) {
        this.context = context;
        this.map = map;
        this.elementStackSizeDefault = elementStackSizeDefault;
    }


    @Nullable
    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        return x != null ? x.get() : null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new RuntimeException("unimpl yet");
    }

    @Override
    public void clear() {
//        if (size()==0)
//            return;
        throw new RuntimeException("unimpl yet");
    }

    @Override
    public final int size() {
        final int[] count = {0};
        map.forEach((k, v) -> {
            if (v.get() != null)
                count[0]++;
        });
        return count[0];
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
        //throw new UnsupportedOperationException();
    }


    /**
     * avoid using this if possible because it involves transforming the entries from the internal map to the external form
     */
    @NotNull
    @Override
    public Set<Entry<X, Y>> entrySet() {
        ArrayUnenforcedSet<Entry<X, Y>> e = new ArrayUnenforcedSet<>();
        map.forEach((k, v) -> {
            Y vv = v.get();
            if (vv != null) {
                //TODO new LazyMapEntry(k, v)
                e.add(new SimpleEntry<>(k, vv));
            }
        });
        return e;
    }

//    public static class LazyMapEntry<K,V>  implements Map.Entry<K,V> {
//
//        @Override
//        public K getKey() {
//            return key;
//        }
//
//        @Override
//        public V getValue() {
//            return null;
//        }
//
//        @Override
//        public V setValue(V value) {
//            return null;
//        }
//    }

    /**
     * records an assignment operation
     * follows semantics of set()
     */
    @Override
    public final Y put(X key, Y value) {
        throw new UnsupportedOperationException("use tryPut(k,v)");
    }

    public boolean tryPut(X key, Y value) {
        return getOrCreateIfAbsent(key).set(value) != null;
    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);
    }

    @NotNull
    protected Versioned<Y> newEntry(X ignored) {
        return new Versioned<>(context, elementStackSizeDefault);
        //return cache(k) ? new Versioned(context) :
        //return new RemovingVersionedEntry(k);
    }

    public boolean forEachVersioned(BiPredicate<? super X, ? super Y> each) {
        Set<Entry<X, Versioned<Y>>> ee = map.entrySet();
        for (Entry<X, Versioned<Y>> e : ee) {
            Y y = e.getValue().get();
            if (y != null) {
                if (!each.test(e.getKey(), y)) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public Y get(/*X*/Object key) {
        Versioned<Y> v = map.get(key);
        return v != null ? v.get() : null;
    }

//    @Nullable
//    public Y get(X key, @NotNull Supplier<Y> ifAbsentPut) {
//        //TODO use compute... Map methods
//        Y o = get(key);
//        if (o == null) {
//            o = ifAbsentPut.get();
//            put(key, o);
//        }
//        return o;
//    }


    @Override
    public final boolean containsKey(Object key) {
        throw new UnsupportedOperationException(); //requires filtering
        //return map.containsKey(key);
    }

    @NotNull
    @Override
    public Set<X> keySet() {
        throw new UnsupportedOperationException(); //requires filtering
        //return map.keySet();
    }

    public static final VersionMap Empty = new VersionMap(new Versioning<>(1, 0), 0, 0) {

        @Override
        public boolean tryPut(Object key, Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }
    };


}
