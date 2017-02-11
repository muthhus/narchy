package nars.bag;

import nars.link.BLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * proxies to a delegate bag
 */
public class BagAdapter<X> implements Bag<X,BLink<X>> {

    @NotNull Bag<X,BLink<X>> bag;

    public BagAdapter(Bag<X,BLink<X>> delegate) {
        set(delegate);
    }

    public void set(Bag<X,BLink<X>> delegate) {
        bag = delegate;
    }

    @Override
    public @Nullable BLink<X> get(@NotNull Object key) {
        return bag.get(key);
    }

    @Override
    public int capacity() {
        return bag.capacity();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super BLink<X>> each, int n) {
        bag.forEachWhile(each, n);
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        bag.forEach(action);
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        bag.forEachKey(each);
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super BLink<X>> action) {
        bag.forEachWhile(action);
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super BLink<X>> action) {
        throw new UnsupportedOperationException(); //typing issue, TODO
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Nullable
    @Override
    public BLink<X> remove(@NotNull X x) {
        return bag.remove(x);
    }

    @Override
    public BLink<X> put(@NotNull X i, @NotNull BLink<X> b, float scale, @Nullable MutableFloat overflowing) {
        return bag.put(i, b, scale, overflowing);
    }

    @NotNull
    @Override
    public Bag<X,BLink<X>> sample(int n, @NotNull Predicate<? super BLink<X>> target) {
        bag.sample(n, target);
        return this;
    }

    @Override
    public int size() {
        return bag.size();
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
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
    public Bag<X,BLink<X>> commit(Function<Bag, Consumer<BLink>> update) {
        bag.commit(update);
        return this;
    }

    @Nullable
    @Override
    public BLink<X> add(Object key, float toAdd) {
        return bag.add(key, toAdd);
    }

    @Nullable
    @Override
    public BLink<X> mul(Object key, float factor) {
        return bag.mul(key, factor);
    }

    @Override
    public void onAdded(BLink<X> v) {
        bag.onAdded(v);
    }

    @Override
    public void onRemoved(@NotNull BLink<X> v) {
        bag.onRemoved(v);
    }

    @Override
    public Bag<X,BLink<X>> commit() {
        bag.commit();
        return this;
    }
}
