package jcog.bag.impl;

import jcog.TODO;
import jcog.Util;
import jcog.data.LightObjectFloatPair;
import jcog.list.FasterList;
import jcog.pri.PriMap;
import jcog.pri.Prioritized;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectFloatPredicate;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * lighter-weight 2nd-generation arraybag
 * https://en.wikipedia.org/wiki/Bag
 * http://www.dictionary.com/browse/baggie
 */
public class Baggie<X> extends PriMap<X> {
    public int capacity;

    /**
     * holds the hash indices of the items in sorted order
     */
    public short[] sorted = ArrayUtils.EMPTY_SHORT_ARRAY;

    public final ShortShortToShortFunction merge = (x, y) -> clamp(x + y);
    short min, max;
    int pressure = 0;
    int mass = 0;

    public Baggie(int capacity) {
        super(0);
        setCapacity(capacity);
    }

    public boolean put(X x, float pri) {
        return put(x, shortPri(pri));
    }

    public boolean put(@NotNull X x, short pri) {
        assert(pri >= 0);


        short from, to;

        List<X> trash = null;

        synchronized (this) {
            pressure += pri;
            if (isFull()) {
                //assert (min >= 0);
                if (pri < min && !containsKey(x)) {
                    return false; //rejected
                }
            }

            int sbefore = size;  //for debugging
            int ch = update(x, pri, merge);
            from = Util.intFromShorts(ch, true);
            assert(from!=-1 ? size==sbefore : size == sbefore+1); //for debugging

            to = Util.intFromShorts(ch, false);
            if (from != to) {
                trash = update(from, to); //change occurred
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
    protected List<X> update(short from, short to) {
        assert (size > 0);

        int toRemove = size - capacity;

        List<X> trash;
        if (toRemove > 0) {
            trash = new FasterList<>(toRemove);
            for (int i = 0; i < toRemove; i++) {
                short lowest = sorted[sorted.length - 1 - i];
                trash.add((X)keys[lowest]);
            }

            //remove each by key because each removal will have changed the indexing that sorted refers to
            trash.forEach(this::remove);
        } else {
            trash = null;
        }

        boolean refill;


//        if (refill || from == -1 /* new entry */) {
            reBuildSort(from, to);
//        } else {
//            reSort(from, to);
//        }

        return trash;
    }

    private void reBuildSort(short from, short to) {
        if (size == 0) {
            clear();
            return;
        }
        int slen = size; //Math.min(size, capacity); //TODO prealloc once and fill remainder with empties
        if (sorted.length != slen) {
            this.sorted = new short[slen];
        } else {
        }

        short[] s = this.sorted;

        int i = 0;
        for (short index = 0, keysLength = (short) keys.length; index < keysLength; index++) {
            Object o = keys[index];
            if (isNonSentinel(o))// o != null && o!=REM)
                s[i++] = index;
        }
        assert (i == size);

        reSort(from, to);
    }

    /**
     * TODO partial sort the affected range
     */
    private void reSort(short from, short to) {
        short[] s = this.sorted;
        sort(s, 0, s.length - 1, (x) -> values[x]); //descending
        this.max = values[s[0]];
        this.min = values[s[s.length-1]];
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
        removeIt((X) key);
    }

    public boolean removeIt(@NotNull X key) {
        synchronized (this) {
            boolean x = removeKey(key);
            if (x) {
                reBuildSort((short)-1, (short)-1);
            }
            return x;
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

    public boolean forEach(ObjectFloatPredicate<X> each) {
        synchronized (this) {
            short[] sorted = this.sorted;
            for (int i = 0; i < size; i++) {
                short ii = sorted[i];
                if (!each.accept((X) keys[ii], priShort(values[ii])))
                    return false;
            }
        }
        return true;
    }

    public List<ObjectFloatPair<X>> toList() {
        synchronized (this) {
            //TODO use non-Stream impl
            return streamDirect().collect(Collectors.toList());
        }
    }

    /** use with caution, only for non-concurrent situations */
    public Stream<LightObjectFloatPair<X>> streamDirect() {
        return IntStream.range(0, size).mapToObj((int i) -> {
            short ii = sorted[i];
            return new LightObjectFloatPair<>((X) keys[ii], priShort(values[ii]));
        });
    }

    /** creates a copy for concurrency purposes */
    public Stream<ObjectFloatPair<X>> stream() {
        return toList().stream();
    }


    /** iterate all elements while each returns true, applying changed values afterward and batch sorting at the end */
    public void commit(Random rng, Predicate<LightObjectFloatPair<X>> each) {
        throw new TODO();
    }

    public void sample(Random rng, Predicate<LightObjectFloatPair<X>> each) {
        ChangeAwareLightObjectFloatPair<X> l = new ChangeAwareLightObjectFloatPair<>();

        while (size > 0) {
            X x;
            synchronized (this) {

                assert(sorted.length == size): "sorted=" + sorted.length + " but size=" + size;

                int i = sorted[sample(rng)];
                x = (X) keys[i];
                assert(x!=null);
                short v = values[i];
                assert(v >= 0);
                l.set(x, priShort(v));
            }

            boolean done = !each.test(l);

            if (l.nextPri != l.pri) {
                boolean removed = false;
                synchronized (this) {
                    if (l.nextPri < 0) {
                        //delete
                        if (removeIt(x)) {
                            removed = true;
                        }
                    } else {
                        if (containsKey(x)) {
                            //TODO reuse the probed index from containsKey lookup
                            set(x, l.nextPri);
                            reSort(l.pri, l.nextPri);
                        }
                        //else it has been removed during the sampling
                    }
                }

                if (removed) { //outside of sync
                    onRemoved(x);
                }

            }

            if (done)
                break;
        }
    }

    protected int sample(Random random) {
        int size = this.size;
        if (size == 1 || random == null)
            return 0;
        else {
            float min = priShort(this.min);
            float max = priShort(this.max);
            float diff = max - min;
            if (diff > Prioritized.EPSILON * size) {
                float i = random.nextFloat(); //uniform
                //normalize to the lack of dynamic range
                i = Util.lerp(diff, i /* flat */, (i * i) /* curved */);
                int j = (int) Math.floor(i * (size - 0.5f));
                if (j >= size) j = size - 1;
                if (j < 0) j = 0;
                return j;
            } else {
                return random.nextInt(size);
            }
        }
    }

    public float priMax() {
        return priShort(max);
    }
    public float priMin() {
        return priShort(min);
    }

    private static class ChangeAwareLightObjectFloatPair<X> extends LightObjectFloatPair<X> {

        short nextPri, pri;

        /**
         * called by this before iteration
         */
        public void set(X x, short v) {
            set(x, priShort(this.nextPri = this.pri = v));
        }

        /**
         * called by callee during iteration
         */
        @Override
        public void set(float v) {
            this.nextPri = shortPri(v);
        }
    }
}
