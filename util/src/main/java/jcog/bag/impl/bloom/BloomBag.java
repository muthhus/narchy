package jcog.bag.impl.bloom;

import com.google.common.collect.Iterators;
import il.technion.tinytable.TinyCountingTable;
import jcog.Util;
import jcog.bag.Bag;
import jcog.list.CircularArrayList;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

import static jcog.bag.Bag.BagCursorAction.Next;

/** experimental implementation backed by a TinyCountingTable and a circular array of items */
public class BloomBag<X> implements Bag<X,PriReference<X>> {

    CircularArrayList<X> list;

    TinyCountingTable pri;
    //StableBloomFilter<X> pri;

    private final Function<X, byte[]> hash;

    /** discretization level */
    final float resolution = 32;

    public BloomBag(int capacity, Function<X, byte[]> hashing) {
        this.hash = hashing;
        setCapacity(capacity);
    }


    protected TinyCountingTable newTable() {
        return new TinyCountingTable(6, 40, 100);
    }

    @Override
    public boolean setCapacity(int c) {
        if (list == null || capacity()!=c) {
            this.list = new CircularArrayList<X>(c);
            this.pri = newTable();
            return true;
        }
        return false;
    }


    @Override
    public void clear() {
        pri = newTable();
        list.clear();
    }

    @Nullable
    @Override
    public PLink<X> get(@NotNull Object f) {
        return new PLink(f, p((X) f));
    }

    PLink<X> get(Object f, @NotNull byte[] b) {
        return new PLink(f, p(b));
    }

//    public MutablePLink<X> get(@NotNull X f, MutablePLink<X> m) {
//        return m.set(f, p(f));
//    }
//    /** caution when referring to values returned by this, they may have changed in the next iteration */
//    public Iterator<PLink<X>> iteratorFast() {
//        MutablePLink<X> p = new MutablePLink<>();
//        return Iterators.transform(list.iterator(), (x -> get(x, p)));
//    }

    @Nullable
    @Override
    public PriReference<X> remove(X x) {
        if (x == null)
            return null;
        byte[] hx = byteHash(x);
        @Nullable PriReference<X> z = get(x, hx);
        if (z.priSafe(0) > 0)
            pri.set(hx, 0);
        //TODO remove x from the collection?
        onRemoved(z);
        return z;
    }

    @Override
    public PriReference<X> put(@NotNull PriReference<X> b, @Nullable MutableFloat overflowing) {
        X c = b.get();

        byte[] hc = byteHash(c);
        float e = p(hc);
        float bp = b.priSafe(0);
        if (bp <= 0)
            return null;

        float v;
        if (e > 0) {
            v = merge(e, bp);
            b.setPri( v );
        } else {
            v = bp;
            if (list.isFull()) {
                remove(list.poll());
            }
            list.offer(c);
        }

        p(hc, v);
        return b;
    }


    protected float merge(float exist, float income) {
        return Util.unitize(exist + income);
    }

    @NotNull
    @Override
    public Bag<X, PriReference<X>> sample(@NotNull Bag.BagCursor<? super PriReference<X>> each, boolean pop) {
        Iterator<X> ii = list.iterator();
        BagCursorAction next = Next;
        while (next==Next && ii.hasNext()) {
            X x = ii.next();
            if (x!=null) {
                next = each.next(get(x));
            }
            if (pop || x == null) ii.remove();
        }
        return this;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public int capacity() {
        return list.capacity();
    }

    @NotNull
    @Override
    public Iterator<PriReference<X>> iterator() {
        return Iterators.transform(list.iterator(), this::get);
    }


    /** any changes to the re-used PLink are ignored */
    @Override public void forEach(Consumer<? super PriReference<X>> action) {
        list.forEach(x -> action.accept(get(x)));
    }

    @Override
    public float pri(@NotNull PriReference<X> key) {
        return p(key.get());
    }

    protected void p(X key, float next) {
        byte[] h = byteHash(key);
        p(h, next);
    }

    private void p(byte[] h, float next) {
        assert(next == next && next >= 0 && next <= 1);
        this.pri.set(h, Math.round(next * resolution));
    }

    public float p(X key) {
        return p(byteHash(key));
    }

    protected byte[] byteHash(X x) {
        return hash.apply(x);
    }

    public float p(byte[] hash) {
        return Util.unitize(this.pri.get(hash) / resolution);
    }

    @NotNull
    @Override
    public X key(PriReference<X> value) {
        return value.get();
    }



    @Override
    public Bag<X, PriReference<X>> commit() {
        return this;
    }

    @NotNull @Override public Bag<X, PriReference<X>> commit(Consumer<PriReference<X>> update) {
        forEach(update);
        return this;
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        list.forEach(each);
    }
}
