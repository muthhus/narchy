package jcog.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.util.ArrayUnenforcedSet;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;


public class VersionMap<X,Y> extends AbstractMap<X, Y>  {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;
    final static int elementStackSize = 8;

    public VersionMap(Versioning context, int initialSize) {
        this(context,
            new HashMap(initialSize)
            //new UnifiedMap(initialSize)
            //new LinkedHashMap<>(initialSize)
        );
    }

    public VersionMap(Versioning context, Map<X, Versioned<Y>/*<Y>*/> map) {
        this.context = context;
        this.map = map;
    }


    @Nullable
    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        if (x != null) {
            Y value = x.get();
            x.clear();
            return value;
        } else {
            return null;
        }
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
        map.forEach((k,v)->{
           if (v.get()!=null)
               count[0]++;
        });
        return count[0];
    }

    @Override
    public final boolean isEmpty() {
        return size()==0;
    }


    /** avoid using this if possible because it involves transforming the entries from the internal map to the external form */
    @NotNull
    @Override public Set<Entry<X, Y>> entrySet() {
//        throw new UnsupportedOperationException("inefficient");
        ArrayUnenforcedSet<Entry<X,Y>> e = new ArrayUnenforcedSet<>(size());
        map.forEach( (k, v) -> e.add(new SimpleEntry<>(k, v.get())));
        return e;
    }



    /**
     * records an assignment operation
     * follows semantics of set()
     */
    @Override
    public final Y put(X key, Y value) {
        throw new UnsupportedOperationException("use tryPut(k,v)");
    }

    public final boolean tryPut(X key, Y value) {
        return getOrCreateIfAbsent(key).set(value)!=null;
    }

    public final void putConstant(X key, Y value) {
        map.put(key, new Versioned(value));
    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);
    }

    @NotNull
    public final Versioned<Y> newEntry(X keyIgnoredk) {
        return new Versioned<>(context, elementStackSize);
        //return cache(k) ? new Versioned(context) :
        //return new RemovingVersionedEntry(k);
    }

    public final boolean computeAssignable(X x, @NotNull BiFunction r) {
        return map.compute(x, r)!=null;
    }

    public boolean forEachVersioned(@NotNull BiPredicate<? super X, ? super Y> each) {
        Set<Entry<X, Versioned<Y>>> ee = map.entrySet();
        for (Entry<X, Versioned<Y>> e : ee) {
            Y y = e.getValue().get();
            if (y!=null) {
                X x = e.getKey();
                if (!each.test(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public final Y get(/*X*/Object key) {
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

    public final Versioned<Y> version(X key) {
        //return map.computeIfPresent(key, (k, v) -> v == null || v.isEmpty() ? null : v);
        return map.get(key);
    }




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

}
