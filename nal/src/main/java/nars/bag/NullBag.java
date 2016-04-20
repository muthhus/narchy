package nars.bag;

import nars.budget.Budgeted;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Bag which holds nothing
 */
public final class NullBag<V> implements Bag<V> {

    @Override
    public void clear() {

    }

    @Override
    public void forEachKey(Consumer<? super V> each) {

    }

    @Nullable
    @Override
    public Bag<V> filter(Predicate<BLink<? extends V>> forEachIfFalseThenRemove) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> get(Object key) {
        return null;
    }

    @Override
    public BLink<V> sample() {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> remove(V x) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> put(@NotNull V v, @NotNull BLink<V> vbLink) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> put(@NotNull V i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> put(Object x) {
        return null;
    }



    @NotNull
    @Override
    public Bag<V> sample(int n, Consumer<? super BLink<V>> target) {
        return null;
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public BLink<V> pop() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Iterator<BLink<V>> iterator() {
        return null;
    }

    @NotNull
    @Override
    public Bag<V> commit() {
        return this;
    }


    @Override
    public void topWhile(Predicate<BLink<V>> each) {

    }

    @Override
    public boolean isFull() {
        return false;
    }


    @Override
    public void setCapacity(int c) {

    }
}
