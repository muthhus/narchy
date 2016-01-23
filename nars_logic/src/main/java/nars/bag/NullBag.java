package nars.bag;

import nars.budget.Budget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
    public BLink<V> remove(V key) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> put(Object newItem) {
        return null;
    }

    @Nullable
    @Override
    public BLink<V> put(Object i, Budget b, float scale) {
        return null;
    }


    @NotNull
    @Override
    public Bag<V> sample(int n, Predicate<? super BLink<V>> each, Collection<? super BLink<V>> target) {
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

    @Override
    public void commit() {

    }


    @Override
    public void topWhile(Predicate<BLink<V>> each) {

    }


    @Override
    public void setCapacity(int c) {

    }
}
