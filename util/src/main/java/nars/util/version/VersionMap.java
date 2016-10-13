package nars.util.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.util.ArrayUnenforcedSet;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;


public class VersionMap<X,Y> extends AbstractMap<X, Y>  {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;
    final static int elementStackSize = 8;

    public VersionMap(Versioning context, int initialSize) {
        this(context,
            //new UnifiedMap(initialSize)
            //new LinkedHashMap<>(initialSize)
            new HashMap(initialSize)
            //new ConcurrentHashMap(initialSize)
            //new ConcurrentHashMapUnsafe<>(initialSize)
        );
    }

    public VersionMap(Versioning context, Map<X, Versioned<Y>/*<Y>*/> map) {
        this.context = context;
        this.map = map;
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

//    @Override
//    public final void putAll(Map<? extends X, ? extends Y> m) {
//        m.forEach(this::put);
//    }

    /** avoid using this if possible because it involves transforming the entries from the internal map to the external form */
    @NotNull
    @Override public Set<Entry<X, Y>> entrySet() {
//        throw new UnsupportedOperationException("inefficient");
        ArrayUnenforcedSet<Entry<X,Y>> e = new ArrayUnenforcedSet<>(size());
        map.forEach( (k, v) -> e.add(new SimpleEntry<>(k, v.get())));
        return e;
    }

    @Override
    public void putAll(Map<? extends X, ? extends Y> m) {
        throw new UnsupportedOperationException();
//        if (m instanceof VersionMap) {
//            VersionMap<X,Y> o = (VersionMap)m;
//            o.map.forEach((k,v) -> put(k, v.get()));
//        }
//        else {
//            //default
//            super.putAll(m);
//        }
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
        map.put(key, new Versioned<>(value));
    }




    public final Versioned getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);
    }

    @NotNull
    public final Versioned<Y> newEntry(X k) {
        return new Versioned(context, elementStackSize);
        //return cache(k) ? new Versioned(context) :
        //return new RemovingVersionedEntry(k);
    }

    public final boolean computeAssignable(X x, @NotNull BiFunction r) {
        return map.compute(x, r)!=null;
    }

    public void forEachVersioned(@NotNull BiConsumer<? super X, ? super Y> each) {
        map.forEach((x,v)-> {
           Y y = v.get();
           if (y!=null)
               each.accept(x,y);
        });
    }


//    /** this implementation removes itself from the map when it is reverted to
//     *  times prior to its appearance in the map */
//    final class RemovingVersionedEntry extends Versioned<Y> {
//
//        private final X key;
//
//        public RemovingVersionedEntry(X key) {
//            super(context);
//            this.key = key;
//        }
//
//        @Override
//        boolean revertNext(int before) {
//            boolean v = super.revertNext(before);
//            if (size == 0)
//                removeFromMap();
//            return v;
//        }
//
//
//        private void removeFromMap() {
//            VersionMap.this.remove(key);
//        }
//
//
//    }



//    public boolean cache(X key) {
//        return false;
//    }

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

    public static class Reassigner<X, Y> implements BiFunction<X, Versioned<Y>, Versioned<Y>> {

        protected Y y;
        protected final VersionMap map;
        private final BiPredicate<X, Y> assigner;

        public Reassigner(BiPredicate<X, Y> assigner, final VersionMap map) {
            this.map = map;
            this.assigner = assigner;
        }

        @Override
        public Versioned<Y> apply(X x, @Nullable Versioned<Y> vy) {
            final Y y = this.y;
            BiPredicate<X, Y> a = this.assigner;
            if (vy == null) {
                return a.test(x, y) ?  map.newEntry(x).set(y) : null;
            } else {
                Y yy = vy.get();
                if (yy == null) {
                    if (!a.test(x, y) || (vy.set(y)==null))
                        return null;
                } else if (!Objects.equals(yy, y)) {
                    return null; //conflict
                }
                return vy;
            }
        }

        /** should not be used by multiple threads at once! */
        public final boolean compute(@NotNull X x, @NotNull Y y) {
            this.y = y;
            return map.computeAssignable(x, this);
        }

    }


}
