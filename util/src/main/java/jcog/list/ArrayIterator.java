package jcog.list;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class ArrayIterator<E> implements Iterator<E>, Iterable<E> {

    private final E[] _array;
    int _index;

    public ArrayIterator(E[] array) {
        _array = array;
    }

    @Override
    public boolean hasNext() {
        return _index < _array.length;
    }

    @Override
    public E next() {
        return _index < _array.length ? _array[_index++] : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (_index != 0)
            throw new RuntimeException("iterator() method can only be called once");
        return this;
    }
}
