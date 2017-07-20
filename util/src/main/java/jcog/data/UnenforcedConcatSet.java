package jcog.data;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;


public class UnenforcedConcatSet<X> extends AbstractSet<X> {
    final Set<X> a, b;

    public static final Set emptySet = Set.of();

    public UnenforcedConcatSet(@NotNull Set<X> a, @NotNull Set<X> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<X> iterator() {
        return Iterators.concat(a.iterator(), b.iterator());
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return Joiner.on(',').join(iterator());
    }
}
