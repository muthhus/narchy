package jcog.list;

import java.util.Iterator;
import java.util.function.Consumer;

public class ArrayIterator<E> implements Iterator<E> {

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
}
