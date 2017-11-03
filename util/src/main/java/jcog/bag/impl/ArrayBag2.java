package jcog.bag.impl;

import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.PriMap;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;

import java.util.List;

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

        List<X> trash = null;

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
                trash = sort(from, to); //change occurred
            } else {
                return true; //no change
            }
        }

        //after synch:

        if (trash != null) {
            trash.forEach(this::onRemoved);
        }

        if (from == -1) {
            onAdded(x);
        }

        return true;
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
            this.sorted = ArrayUtils.EMPTY_SHORT_ARRAY;
            this.min = this.max = -1;
        }
    }

    /**
     * from and to are the range of values that would have changed, so that a partial sort can be isolated to the sub-range of the list that has changed
     * returns trashed items, if any, or null if none
     */
    protected List<X> sort(short from, short to) {
        assert(size > 0);

        int toRemove = size - capacity;
        Object[] keys = this.keys;

        List<X> trash = null;
        for (int i = 0; i < toRemove; i++) {


            short lowest = sorted[sorted.length - 1 - i];

            X rem = removeIt(lowest);
            if (rem != null) {
                if (trash == null)
                    trash = new FasterList(1);
                trash.add(rem);
            } else {
                assert (false);
            }

        }

        short[] s;
        boolean refill;
        int slen = Math.min(size, capacity); //TODO prealloc once and fill remainder with empties
        if (sorted.length != slen) {
            s = this.sorted = new short[slen];
            refill = true;
        } else {
            s = this.sorted;
            refill = false;
        }


        if (refill || from==-1 /* new entry */) {
            int i = 0;
            for (short index = 0, keysLength = (short) keys.length; index < keysLength; index++) {
                Object o = keys[index];
                if (isNonSentinel(o))// o != null && o!=REM)
                    s[i++] = index;
            }
            assert (i == size);
        }

        //TODO partial sort the affected range
        sort(sorted, 0, s.length - 1, (x) -> values[x]); //descending
        max = values[highestIndex()];
        this.min = values[lowestIndex()];
        return trash;
    }

    public X lowest() {
        synchronized (this) {
            short i = lowestIndex();
            return i < 0 ? null : (X) keys[i];
        }
    }

    public X highest() {
        synchronized (this) {
            short i = highestIndex();
            return i < 0 ? null : (X) keys[i];
        }
    }

    private short lowestIndex() {
        int s = this.size;
        return s > 0 ? sorted[s - 1] : -1;
    }

    private short highestIndex() {
        return size > 0 ? sorted[0] : -1;
    }

    @Override
    public void remove(Object key) {
        throw new UnsupportedOperationException();
    }

    protected X removeIt(int index) {
        boolean removed;
        X x;
        synchronized (this) {
            x = removeAtIndex(index);
        }
        return x;
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
                if (j-- == left)
                    break;
            }
            a[j + 1] = ai;
        }
//        }
    }

    public boolean contains(X b) {
        return containsKey(b);
    }

}
