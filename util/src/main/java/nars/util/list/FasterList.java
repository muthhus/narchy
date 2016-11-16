package nars.util.list;

import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Less-safe faster FastList with direct array access
 * <p>
 * TODO override the array creation to create an array
 * of the actual type necessary, so that .array()
 * can provide the right array when casted
 */
public class FasterList<X> extends FastList<X> {

    static final Object[] ZERO_SIZED_ARRAY = new Object[0];


    public FasterList() {
        super();
    }

    public FasterList(int capacity) {
        super(capacity);
    }

    public FasterList(Collection<X> x) {
        super(x);
    }

    /**
     * uses array directly
     */
    public FasterList(int size, X[] x) {
        super(size, x);
    }

    @SafeVarargs
    public FasterList(X... x) {
        super(x);
    }

    /**
     * quickly remove the final elements without nulling them by setting the size pointer
     * this directly manipulates the 'size' value that the list uses to add new items at. use with caution
     * if index==-1, then size will be zero, similar to calling clear(),
     * except the array items will not be null
     * <p>
     * returns the next value
     */
    public final void popTo(int index) {
        this.size = index + 1;
    }


    public X removeLast() {
        return this.items[--size];
    }

    @Nullable
    public X removeLastElseNull() {
        int s = size;
        return s == 0 ? null : removeLast();
    }

    @Override
    public final X get(int index) {
        //if (index < this.size) {
        return items[index];
        //}
    }


    public final X[] toArrayExact(X[] array) {
        System.arraycopy(this.items, 0, array, 0, this.size);
        return array;
    }



    public final boolean addIfCapacity(X newItem) {
        X[] ii = this.items;
        int s;
        if (ii.length <= (s = this.size++)) {
            return false;
        }
        ii[s] = newItem;
        return true;
    }


    /**
     * use with caution.
     * --this could become invalidated so use it as a snapshot
     * --dont modify it
     * --when iterating, expect to encounter a null
     * at any time, and if this happens, break your loop
     * early
     * *
     */
    public final X[] array() {
        return items;
    }

//    /** use this to get the fast null-terminated version;
//     *  slightly faster; use with caution
//     * */
//    public <E> E[] toNullTerminatedArray(E[] array) {
//        array = toArrayUnpadded(array);
//        final int size = this.size;
//        if (array.length > size) {
//            array[size] = null;
//        }
//        return array;
//    }

    @Override
    public final boolean removeIf(Predicate<? super X> filter) {
        int s = size();
        int ps = s;
        X[] a = this.items;
        for (int i = 0; i < s; ) {
            if (filter.test(a[i])) {
                s--;
                System.arraycopy(a, i+1, a, i, s - i);
                Arrays.fill(a, s, ps,null);
            } else {
                i++;
            }
        }
        if (ps!=s) {
            this.size = s;
            return true;
        }

        return false;
    }

    public final boolean removeIf(Predicate<? super X> filter, List<X> displaced) {
        int s = size();
        int ps = s;
        X[] a = this.items;
        for (int i = 0; i < s; ) {
            X ai = a[i];
            if (ai == null || (filter.test(ai) && displaced.add(ai))) {
                s--;
                System.arraycopy(a, i+1, a, i, s - i);
                Arrays.fill(a, s, ps,null);
            } else {
                i++;
            }
        }
        if (ps!=s) {
            this.size = s;
            return true;
        }

        return false;
    }

    public X[] toArray(IntFunction<X[]> arrayBuilder) {
//HACK broken return the internal array if of the necessary size, otherwise returns a new array of precise size
//        X[] current = this.array();
//        if (size() == current.length)
//            return current;
        return fillArray(arrayBuilder.apply(size()));
    }


//    /** does not pad the remaining values in the array with nulls */
//    X[] toArrayUnpadded(X[] array) {
//        if (array.length < this.size)        {
//            //resize larger
//            array = (X[]) Array.newInstance(array.getClass().getComponentType(), this.size);
//        }
//        return fillArray(array);
//    }

    public final X[] fillArrayNullPadded(X[] array) {
        int s = size;
        int l = array.length;
        if (array == null || array.length < (s + 1)) {
            array = (X[]) Array.newInstance(array.getClass().getComponentType(), s + 1);
        }
        System.arraycopy(items, 0, array, 0, s);
        if (s < l)
            Arrays.fill(array, s, l, null); //pad remainder
        return array;
    }

    public final X[] fillArray(X[] array) {
        int s = size;
        int l = array.length;
        System.arraycopy(items, 0, array, 0, s);
        if (s < l)
            Arrays.fill(array, s, l, null); //pad remainder
        return array;
    }


//    public final X[] toNullTerminatedUnpaddedArray(X[] array) {
//        final int s = this.size; //actual size
//        if (array.length < (s+1)) {
//            array = (X[]) Array.newInstance(array.getClass().getComponentType(), s+1);
//        }
//        System.arraycopy(this.items, 0, array, 0, s);
//        array[s] = null;
//        return array;
//    }

    @Override
    public final void forEach(Consumer c) {
        for (Object j : items) {
            if (j == null)
                break; //end of list
            c.accept(j);
        }
    }


    public void clearFast() {
        popTo(-1);
    }


    public final void clear0() {
        this.items = (X[]) ZERO_SIZED_ARRAY;
        this.size = 0;
    }

    /** remove, but with Map.remove semantics */
    public X removed(@NotNull X object) {
        int index = this.indexOf(object);
        if (index >= 0) {
            X r = get(index);
            this.remove(index);
            return r;
        }
        return null;
    }

    public final boolean addIfNotNull(@Nullable Supplier<X> x) {
        return addIfNotNull(x.get());
    }

    public final boolean addIfNotNull(@Nullable X x) {
        if (x!=null)
            return add(x);
        return false;
    }

    public int forEach(int offset, IntObjectPredicate each) {
        int n = offset;
        for (Object j : items) {
            if (j == null)
                break; //end of list
            each.accept(n++, j);
        }
        return size();
    }

    public void addAll(X... x) {
        for (X y : x)
            add(y);
    }
}