package jcog.bag.impl;

import jcog.Util;
import jcog.pri.PriMap;
import jcog.util.FloatFloatToFloatFunction;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;

/**
 * lighter-weight 2nd-generation arraybag
 * TODO not finished
 */
public class ArrayBag2<X> extends PriMap<X> {
    public int capacity;

    /**
     * holds the hash indices of the items in sorted order
     */
    public short[] sorted = ArrayUtils.EMPTY_SHORT_ARRAY;

    public final ShortShortToShortFunction merge = (x, y) -> (short) (x + y);
    short min, max;
    int pressure = 0;
    int mass = 0;

    public ArrayBag2(int capacity) {
        super(0);
        setCapacity(capacity);
    }

    public boolean put(X x, float pri) {
        return put(x, shortPri(pri));
    }

    public boolean put(X x, short pri) {
        short from, to;
        synchronized (this) {
            pressure += pri;
            if (isFull()) {
                assert (min >= 0);
                if (pri < min && !containsKey(x)) {
                    return false; //rejected
                }
            }
            int ch = update(x, pri, merge);
            from = Util.intFromShorts(ch, true);
            to = Util.intFromShorts(ch, false);
            if (from != to) {
                sort(from, to); //change occurred
            }
        }

        if (from == -1) {
            onAdded(x);
        }
        return true;
    }

    /**
     * from and to are the range of values that would have changed, so that a partial sort can be isolated to the sub-range of the list that has changed
     */
    protected void sort(short from, short to) {
        int toRemove = size - capacity;
        Object[] keys = this.keys;
        for (int i = 0; i < toRemove; i++) {
            int s = size;
            short lowest = lowestIndex();
            remove(keys[lowest]);
            assert (size < s);
        }
        short[] s;
        if (sorted.length != size) {
            s = this.sorted = new short[size];
            int i = 0;
            for (short index = 0, keysLength = (short) keys.length; index < keysLength; index++) {
                Object o = keys[index];
                if (o != null)
                    s[i++] = index;
            }
            assert (i == size);
        } else {
            s = this.sorted;
        }

        //TODO partial sort the affected range
        sort(sorted, 0, s.length-1, (x) -> {
            return values[x]; //descending
        });

    }

    public X lowest() {
        synchronized (this) {
            short i = lowestIndex();
            if (i < 0)
                return null;
            else
                return (X) keys[i];
        }
    }

    public X highest() {
        synchronized (this) {
            short i = highestIndex();
            if (i < 0)
                return null;
            else
                return (X) keys[i];
        }
    }

    private short lowestIndex() {
        return size > 0 ? sorted[size - 1] : -1;
    }
    private short highestIndex() {
        return size > 0 ? sorted[0] : -1;
    }



    @Override
    public void remove(Object key) {
        boolean removed;
        X x = (X) key;
        synchronized (this) {
            removed = super.removeKey(x);
        }
        if (removed) {
            onRemoved(x);
        }
    }

    public void onAdded(X x) {

    }

    public void onRemoved(X x) {

    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isFull() {
        return size() == capacity;
    }


    static void sort(short[] a, int left, int right, ShortToShortFunction v) {
//        // Use counting sort on large arrays
//        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
//            int[] count = new int[NUM_BYTE_VALUES];
//
//            for (int i = left - 1; ++i <= right;
//                 count[a[i] - Byte.MIN_VALUE]++
//                    )
//                ;
//            for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
//                while (count[--i] == 0) ;
//                byte value = (byte) (i + Byte.MIN_VALUE);
//                int s = count[i];
//
//                do {
//                    a[--k] = value;
//                } while (--s > 0);
//            }
//        } else { // Use insertion sort on small arrays
        for (int i = left, j = i; i < right; j = ++i) {
            short ai = a[i + 1];
            while (v.valueOf(ai) > v.valueOf(a[j])) {
                a[j + 1] = a[j];
                if (j-- == left) {
                    break;
                }
            }
            a[j + 1] = ai;
        }
//        }
    }
}
