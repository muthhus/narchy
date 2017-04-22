package jcog.bag.util;

import jcog.bag.Bag;
import jcog.pri.PLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * proxies to a delegate bag

 * TODO find any inherited methods which would return the proxied
 * bag instead of this instance
 */
abstract public class ProxyBag<X,Y> implements Bag<X,Y> {

    @NotNull Bag<X,Y> bag;

    public ProxyBag(Bag<X,Y> delegate) {
        set(delegate);
    }

    public void set(Bag<X,Y> delegate) {
        bag = delegate;
    }

    @Override
    public @Nullable Y get(@NotNull Object key) {
        return bag.get(key);
    }

    @Override
    public int capacity() {
        return bag.capacity();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super Y> each, int n) {
        bag.forEachWhile(each, n);
    }

    @Override
    public void forEach(Consumer<? super Y> action) {
        bag.forEach(action);
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        bag.forEachKey(each);
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super Y> action) {
        bag.forEachWhile(action);
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super Y> action) {
        throw new UnsupportedOperationException(); //typing issue, TODO
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Nullable
    @Override
    public Y remove(@NotNull X x) {
        return bag.remove(x);
    }

    @Override
    public Y put(@NotNull Y b, float scale, @Nullable MutableFloat overflowing) {
        return bag.put(b, scale, overflowing);
    }


    @NotNull
    @Override
    public Bag<X, Y> sample(@NotNull Bag.BagCursor<? super Y> each) {
        bag.sample(each);
        return this;
    }

    @Override
    public int size() {
        return bag.size();
    }

    @NotNull
    @Override
    public Iterator<Y> iterator() {
        return bag.iterator();
    }

    @Override
    public boolean contains(@NotNull X it) {
        return bag.contains(it);
    }

    @Override
    public boolean isEmpty() {
        return bag.isEmpty();
    }

    @Override
    public boolean setCapacity(int c) {
        return bag.setCapacity(c);
    }

    @Override
    public float priMax() {
        return bag.priMax();
    }

    @Override
    public float priMin() {
        return bag.priMin();
    }


    @NotNull
    @Override
    public Bag<X,Y> commit(Consumer<Y> update) {
        bag.commit(update);
        return this;
    }



    @Override
    public void onAdded(Y v) {
        bag.onAdded(v);
    }

    @Override
    public void onRemoved(@NotNull Y v) {
        bag.onRemoved(v);
    }

    @Override
    public Bag<X,Y> commit() {
        bag.commit();
        return this;
    }
}
