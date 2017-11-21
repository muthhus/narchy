package jcog.list;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;

public class ArrayIterator<E> implements Iterator<E>, Iterable<E> {

    private final E[] array;
    int index;

    public ArrayIterator(E[] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public E next() {
        return index < array.length ? array[index++] : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (index != 0)
            throw new RuntimeException("iterator() method can only be called once");
        return this;
    }

    public static <E> Iterator<E> get(E[] e) {
        return ArrayIterator.get(e, e.length);
    }

    public static <E> Iterator<E> get(E[] e, int size) {
        if (size == 0)
            return Collections.emptyIterator();
        else if (size == e.length)
            return new ArrayIterator(e);
        else
            return new BoundedArrayIterator(e, size);
    }

    static class BoundedArrayIterator<E> extends ArrayIterator<E> {

        private final int size;

        public BoundedArrayIterator(E[] array, int size) {
            super(array);
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }
    }

}
