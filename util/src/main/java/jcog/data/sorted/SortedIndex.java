package jcog.data.sorted;


import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

/**
 * Stores items with highest priority at index = 0,
 * lowest = size()-1
 */
public abstract class SortedIndex<T> implements Collection<T> {

    @Override
    public boolean add(T t) {
        throw new RuntimeException("Use insert method which can return a displaced object");
    }

    /** similar semantics as Map.put ; the displaced task will be returned, which may be the input if it could not be inserted */
    public abstract T insert(T i);

    /** current index of existing item */
    public abstract int pos(T o);

    /** numeric access */
    public abstract T get(int i);
    public abstract T remove(int i);


    public final T getLast() {  return get(size()-1);    }
    public final T getFirst() { return get(0); }

    //public abstract Iterator<T> descendingIterator();
    public abstract void setCapacity(int capacity);

    public abstract List<T> list();
    

    public abstract boolean isSorted();

    public abstract int locate(Object o);

    public abstract int capacity();

    public void print(PrintStream out) {
        forEach(out::println);
    }

    public abstract float score(T v);


    public final boolean scoreBetween(int currentIndex, int size, T v) {
        final float newScore = score(v);
        return (newScore < scoreAt(currentIndex+1, size)) || //score of item below
                (newScore > scoreAt(currentIndex-1, size)); //score of item above
    }

    public final float scoreAt(int i, int size) {
        if (i == -1) return Float.POSITIVE_INFINITY;
        if (i == size) return Float.NEGATIVE_INFINITY;
        return score(get(i));
    }

    public final void reinsert(int currentIndex, T v) {
        remove(currentIndex);
        insert(v); //reinsert
    }
}