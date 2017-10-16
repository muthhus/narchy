package jcog.pri;

import com.google.common.collect.Iterators;
import jcog.exe.Loop;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * next-gen bag experiment
 * hybrid strong/weak + prioritized + forgetting bounded cache
 */
public class PriMap<X, Y> extends AbstractMap<X, Y> {

    public enum Hold {
        STRONG, WEAK, SOFT
    }

    private Hold defaultMode = Hold.STRONG;

    abstract public static class TLink<X, Y> implements Supplier<Y> {
        public final X key;
        public final int hash;

        public TLink(X key) {
            this.key = key;
            this.hash = key.hashCode();
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj || (key == obj || key.equals(obj));
        }

    }

    public static class BasicTLink<X, Y> extends TLink<X, Y> {
        private Y value;

        public BasicTLink(X key) {
            super(key);
        }

        public TLink<X, Y> setValue(Y newValue) {
            this.value = newValue;
            return this;
        }

        @Override
        public Y get() {
            return value;
        }
    }


    TLink<X, Y> link(@Nullable TLink<X, Y> t, X k, Y v, Hold mode) {

        switch (mode) {
            case STRONG:
                if (t == null)
                    t = new BasicTLink(k);
                ((BasicTLink) t).setValue(v);
                break;

            case SOFT:
                return new ProxyTLink(k, new MySoftReference(k, v, refq));

            case WEAK:
                return new ProxyTLink(k, new MyWeakReference(k, v, refq));

            default:
                throw new UnsupportedOperationException();
        }
        return t;
    }

    public static class ProxyTLink<X, Y> extends TLink<X, Y> {

        private final Supplier<Y> value;

        public ProxyTLink(X key, Supplier<Y> provider) {
            super(key);
            this.value = provider;
        }

        @Override
        public Y get() {
            return value.get();
        }
    }

    private static final class MyWeakReference<X, Y> extends WeakReference<Y> implements Supplier<Y> {
        public final X key;

        public MyWeakReference(X key, Y val, ReferenceQueue q) {
            super(val, q);
            this.key = key;
        }
    }

    private static final class MySoftReference<X, Y> extends SoftReference<Y> implements Supplier<Y> {
        public final X key;

        public MySoftReference(X key, Y val, ReferenceQueue q) {
            super(val, q);
            this.key = key;
        }
    }

    final Map<X, TLink<X, Y>> map = new ConcurrentHashMap<>();

    private final LongSupplier clock;
    public final ReferenceQueue refq = new ReferenceQueue();
    private final Loop cleaner;

    public PriMap(LongSupplier clock) {
        this.clock = clock;

        this.cleaner = new Loop(200) {
            @Override
            public boolean next() {

                try {
                    Object ref;
                    for (ref = refq.remove(); ref != null; ref = refq.poll()) {
                        removeGC(ref);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }
        };

    }

    public Y get(Object x) {
        TLink<X, Y> t = map.get(x);
        return t != null ? t.get() : null;
    }

    public Y computeIfAbsent(X x, Hold mode, Function<X, Y> builder) {
        TLink<X, Y> r = map.computeIfAbsent(x, (xx) -> link(null, xx, builder.apply(xx), mode));
        return r!=null ? r.get() : null;
    }

    @Override
    public Y merge(X x, Y value, BiFunction<? super Y, ? super Y, ? extends Y> remap) {
        TLink<X, Y> r = map.compute(x, (xx, pv) -> link(null, xx, remap.apply(pv != null ? pv.get() : null, value), defaultMode));
        return r!=null ? r.get() : null;
    }

    public Y put(X x, Hold mode, Y y) {
        TLink<X, Y> r = map.compute(x, (xx, prev) -> link(prev, xx , y, mode));
        return r!=null ? r.get() : null;
    }

    @Override
    public Y put(X x, Y y) {
        return put(x, defaultMode, y);
    }

    protected void removeGC(Object x) {
        Object key;
        if (x instanceof MyWeakReference) {
            key = ((MyWeakReference) x).key;
        } else if (x instanceof MySoftReference) {
            key = ((MySoftReference) x).key;
        } else {
            throw new UnsupportedOperationException();
        }
        map.remove(key);
    }

    @Override
    public Set<Entry<X, Y>> entrySet() {

        return new MyEntrySet();
    }

    private class MyEntrySet extends AbstractSet<Entry<X, Y>> {

        final Set<Entry<X, TLink<X, Y>>> e = map.entrySet();

        @Override
        public Iterator iterator() {
            return Iterators.transform(e.iterator(), ee -> new SimpleEntry(ee.getKey(), ee.getValue().get()));
        }

        @Override
        public int size() {
            return e.size();
        }
    }


}
