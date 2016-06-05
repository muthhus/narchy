package nars.util.version;

import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.util.ArrayUnenforcedSet;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Supplier;


public class VersionMap<X,Y> extends AbstractMap<X, Y>  {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;

    public VersionMap(Versioning context, int initialSize) {
        this(context,
            //new UnifriedMap(initialSize)
            //new LinkedHashMap<>(initialSize)
            new HashMap(initialSize)
        );
    }

    public VersionMap(Versioning context, Map<X, Versioned<Y>/*<Y>*/> map) {
        this.context = context;
        this.map = map;
    }

    @Override
    public final boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @NotNull
    @Override
    public Set<X> keySet() {
        return map.keySet();
    }

    //    @Override
//    public final void forEach(BiConsumer<? super X, ? super Y> action) {
//        map.forEach((BiConsumer<? super X, ? super Versioned<Y>>) action);
//    }

    @Nullable
    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        if (x != null) {
            Y value = x.get();
            context.delete(x);
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
        throw new RuntimeException("unimpl yet");
    }

    @Override
    public final int size() {
        return map.size();
    }

    @Override
    public final boolean isEmpty() {
        return map.isEmpty();
    }

//    @Override
//    public final void putAll(Map<? extends X, ? extends Y> m) {
//        m.forEach(this::put);
//    }

    /** avoid using this if possible because it involves transforming the entries from the internal map to the external form */
    @NotNull
    @Override public Set<Entry<X, Y>> entrySet() {
//        throw new UnsupportedOperationException("inefficient");
        ArrayUnenforcedSet<Entry<X,Y>> e = new ArrayUnenforcedSet<>(size());
        map.forEach( (k, v) -> {
            e.add(new AbstractMap.SimpleEntry<>(k, v.get()));
        });
        return e;
    }

    @Override
    public void putAll(Map<? extends X, ? extends Y> m) {
        if (m instanceof VersionMap) {
            VersionMap<X,Y> o = (VersionMap)m;
            o.map.forEach((k,v) -> put(k, v.get()));
        }
        else {
            //default
            super.putAll(m);
        }
    }

    /**
     * records an assignment operation
     * follows semantics of set()
     */
    @Override
    public final Y put(X key, Y value) {
        getOrCreateIfAbsent(key).set(value);
        return null;
    }


    public final Versioned getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);
    }

    @NotNull
    public final Versioned<Y> newEntry(X k) {
        //return new Versioned(context);
        //return cache(k) ? new Versioned(context) :
        return new RemovingVersionedEntry(k);
    }

    public final boolean computeAssignable(X x, @NotNull Reassigner<X,Y> r) {
        return map.compute(x, r)!=null;
    }

    public void forEachVersioned(BiConsumer<? super X, ? super Versioned<Y>> each) {
        map.forEach(each);
    }


    /** this implementation removes itself from the map when it is reverted to
     *  times prior to its appearance in the map */
    final class RemovingVersionedEntry extends Versioned<Y> {

        private final X key;

        public RemovingVersionedEntry(X key) {
            super(context);
            this.key = key;
        }

        @Override
        boolean revertNext(int before) {
            boolean v = super.revertNext(before);
            if (size == 0)
                removeFromMap();
            return v;
        }


        private void removeFromMap() {
            VersionMap.this.remove(key);
        }

      
    }



//    public boolean cache(X key) {
//        return false;
//    }

    @Override
    public final Y get(/*X*/Object key) {
        Versioned<Y> v = version((X) key);
        return v != null ? v.get() : null;
    }

    @Nullable
    public Y get(X key, @NotNull Supplier<Y> ifAbsentPut) {
        //TODO use compute... Map methods
        Y o = get(key);
        if (o == null) {
            o = ifAbsentPut.get();
            put(key, o);
        }
        return o;
    }

    public final Versioned<Y> version(X key) {
        //return map.computeIfPresent(key, (k, v) -> v == null || v.isEmpty() ? null : v);
        return map.get(key);
    }

    public static final class Reassigner<X, Y> implements BiFunction<X, Versioned<Y>, Versioned<Y>> {

        private Y y;
        private final VersionMap map;
        private final BiPredicate<X, Y> assigner;

        public Reassigner(BiPredicate<X, Y> assigner, final VersionMap map) {
            this.map = map;
            this.assigner = assigner;
        }

        @Override
        public Versioned<Y> apply(X X, @Nullable Versioned<Y> py) {
            final Y y = this.y;
            BiPredicate<X, Y> a = this.assigner;
            if (py == null) {
                return a.test(X, y) ? map.newEntry(X).set(y) : null;
            } else {
                Y yy = py.get();
                if (yy == null) {
                    if (a.test(X, y))
                        py.set(y);
                    else
                        return null;
                } else if (!yy.equals(y)) {
                    return null; //conflict
                }
                return py;
            }
        }

        /** should not be used by multiple threads at once! */
        public final boolean compute(@NotNull X x, @NotNull Y y) {
            this.y = y;
            boolean b = map.computeAssignable(x, this);
            this.y = null;
            return b;
        }
    }
}
