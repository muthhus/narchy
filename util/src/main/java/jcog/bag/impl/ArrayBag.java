package jcog.bag.impl;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.bag.Bag;
import jcog.data.sorted.SortedArray;
import jcog.list.FasterList;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriForget;
import jcog.pri.op.PriMerge;
import jcog.table.SortedListTable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
abstract public class ArrayBag<X, Y extends Prioritized> extends SortedListTable<X, Y> implements Bag<X, Y> {

    final PriMerge mergeFunction;

    /**
     * inbound pressure sum since last commit
     */
    public final AtomicDouble pressure = new AtomicDouble();

    public float mass;

    protected float min, max;
    protected boolean mustSort;

    protected ArrayBag(PriMerge mergeFunction, @NotNull Map<X, Y> map) {
        this(0, mergeFunction, map);
    }

    static final class SortedPLinks extends SortedArray {
        @Override
        protected Object[] newArray(int oldSize) {
            return new Object[oldSize == 0 ? 2 : oldSize + (Math.max(1, oldSize / 2))];
        }
    }

    protected ArrayBag(@Deprecated int cap, PriMerge mergeFunction, @NotNull Map<X, Y> map) {
        super(new SortedPLinks(), map);
        this.mergeFunction = mergeFunction;
        setCapacity(cap);

        //this.capacity = cap;
        //this.toPut = map instanceof ConcurrentMap ? new QueueLock<>(this::put) : this::put;
    }

    @Override
    public final float floatValueOf(Y y) {
        return -pCmp(y);
    }


    /**
     * returns whether the capacity has changed
     */
    @Override
    public final void setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            this.capacity = newCapacity;
            //synchronized (items) {
            if (this.size() > newCapacity)
                commit(null, true);
            //}
            //return true;
        }
        //return false;
    }

    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public float depressurize() {
        float pv = (float) pressure.getAndSet(0);
        if (pv >= 0) {
            return pv;
        } else {
            pressure.set(0);
            return 0;
        }
    }

    @Override
    public void pressurize(float f) {
        pressure.addAndGet(f);
    }


    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     * call within synchronized
     *
     * @return List of trash items
     * trash must be removed from the map, outside of critical section
     * may include the item being added
     */
    @Nullable
    private FasterList<Y> update(@Nullable Y toAdd, @Nullable Consumer<Y> update) {

        int s = size();
        int c = capacity();
        if (s == 0 && toAdd == null) {
            this.min = this.max = this.mass = 0;
            return null;
        }


        FasterList<Y> trash = new FasterList(0);
        if (s > 0) {
            s = update(toAdd != null, s, trash, update);
        } else {
            this.min = this.max = this.mass = 0;
        }


        boolean rejection = false;
        if (toAdd != null) {
            if (s < c) {
                //room to add an item
                items.add(toAdd, this);
                this.mass += toAdd.priElseZero();
                s++;

            } else {
                //at capacity, size will remain the same
                Y removed;
                if (toAdd.priElseZero() > min) {
                    //remove lowest
                    assert (size() == s);
                    assert (s > 0) : "size is " + s + " and capacity is " + c + " so why are we removing an item";

                    removed = items.removeLast();
                    this.mass -= removed.priElseZero();
                    //add this
                    items.add(toAdd, this);
                    this.mass += toAdd.priElseZero();
                } else {
                    removed = toAdd;
                    rejection = true;
                }

                trash.add(removed);
            }
        }

        boolean trashEmpty = trash.isEmpty();

        if (!mustSort && s > 1 && (toAdd != null || (!trashEmpty && !rejection))) {
            //bag has changed, update:
            mustSort = true;
        }

        return trashEmpty ? null : trash;
    }


    protected void ensureSorted() {
        if (!mustSort)
            return;

        mustSort = false;

        sort();
    }

    protected void sort() {
        int s = size();
        if (s == 0)
            return;
        Object[] il = items.list;
        if (s > 1) { //test again
            int[] stack = new int[sortSize(s) /* estimate */];
            qsort(stack, il, 0 /*dirtyStart - 1*/, (s - 1));


            //Arrays.sort(il, sortComparator); //wont work because if the priorities are being changed from another thread it complains about sort order getting violated
        }
    }

//    static final Comparator<Object> sortComparator = (x, y) -> {
//        if (x == y) return 0;
//        if (x == null) return +1;
//        if (y == null) return -1;
//        return Float.compare(
//                ((Prioritized) y).priElseNeg1(), ((Prioritized) x).priElseNeg1()
//        );
//    };

    private int update(@Deprecated boolean toAdd, int s, List<Y> trash, @Nullable Consumer<Y> update) {

        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;


        //first step: remove any nulls and deleted values

        SortedArray items2 = this.items;
        final Object[] l = items2.array();
        int removedFromMap = 0;

        float above = Float.POSITIVE_INFINITY;
        boolean mustSort = false;
        for (int i = 0; i < s; i++) {
            Y x = (Y) l[i];
            float p;
            if (x == null || ((p = x.pri()) != p /* deleted */) && trash.add(x)) {
                items2.removeFast(i);
                removedFromMap++;
            } else {
                if (update != null) {
                    update.accept(x);
                    p = x.priElseZero(); //update pri because it may have changed during update
                }
                min = Math.min(min, p);
                max = Math.max(max, p);
                mass += p;
                if (p - above >= Pri.EPSILON)
                    mustSort = true;

                above = p;
            }
        }

        s -= removedFromMap;

        final int c = capacity;
        if (s > c) {

            //second step: if still not enough, do emergency removal of the lowest ranked items until quota is met
            SortedArray<Y> items1 = this.items;
            while (s > 0 && ((s - c) + (toAdd ? 1 : 0)) > 0) {
                Y w1 = items1.removeLast();
                if (w1 != null) //skip over nulls ... does this even happen?
                    trash.add(w1);
                s--;
            }
        }

//        if (!mustSort && !toAdd)
//            System.out.println("elides sort");

        this.min = min;
        this.max = max;
        this.mass = mass;
        this.mustSort |= mustSort;
        return s;
    }


    //    @Override
//    public final int compare(@Nullable BLink o1, @Nullable BLink o2) {
//        float f1 = cmp(o1);
//        float f2 = cmp(o2);
//
//        if (f1 < f2)
//            return 1;           // Neither val is NaN, thisVal is smaller
//        if (f1 > f2)
//            return -1;            // Neither val is NaN, thisVal is larger
//        return 0;
//    }


    static boolean cmpGT(@Nullable Object o1, @Nullable Object o2) {
        return cmpGT((Prioritized) o1, (Prioritized) o2);
    }

    static boolean cmpGT(/*@Nullable */Object o1, float o2) {
        return cmpGT((Prioritized) o1, o2);
    }

    /**
     * true iff o1 > o2
     */
    static boolean cmpGT(@Nullable Prioritized o1, @Nullable Prioritized o2) {
        return cmpGT(o1, pCmp(o2));
    }

    static boolean cmpGT(@Nullable Prioritized o1, float o2) {
        return (pCmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static boolean cmpGT(float o1, @Nullable Prioritized o2) {
        return (o1 < pCmp(o2));
    }

    static boolean cmpGT(float o1, float o2) {
        return (o1 < o2);
    }


    /**
     * true iff o1 < o2
     */
    static boolean cmpLT(@Nullable Prioritized o1, @Nullable Prioritized o2) {
        return cmpLT(o1, pCmp(o2));
    }

    static boolean cmpLT(@Nullable Prioritized o1, float o2) {
        return (pCmp(o1) > o2);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    static float pCmp(@Nullable Prioritized b) {
        return (b == null) ? -2f : b.priElseNeg1(); //sort nulls beneath

//        float p = b.pri();
//        return p == p ? p : -1f;
        //return (b!=null) ? b.priIfFiniteElseNeg1() : -1f;
        //return b.priIfFiniteElseNeg1();
    }


//    @Override
//    public Bag<X, Y> sample(int max, Consumer<? super Y> each) {
//        return sample(max, ((x) -> {
//            each.accept(x);
//            return true;
//        }));
//    }
//
//    @Override
//    public Bag<X, Y> sample(int max, Predicate<? super Y> kontinue) {
//        synchronized (items) {
//            assert (max > 0);
//            int s = size();
//
//            Object[] oo = items.list;
//            if (oo.length == 0)
//                return this;
//
//            Object[] ll = oo;
//            if (s == 1) {
//                //get the only
//                kontinue.test((Y) ll[0]);
//            } else if (s == max) {
//                //get all
//                for (int i = 0; i < s; i++) {
//                    if (!kontinue.test((Y) ll[i]))
//                        break;
//                }
//            } else if (s > 1) {
//                //get some: choose random starting index, get the next consecutive values
//                max = Math.min(s, max);
//                for (int i =
//                     (this instanceof CurveBag ? random(s) : 0), m = 0; m < max; m++) {
//                    Y lll = (Y) ll[i++];
//                    if (lll != null)
//                        if (!kontinue.test(lll))
//                            break;
//                    if (i == s) i = 0; //modulo
//                }
//            }
//        }
//        return this;
//    }

    protected int random(int s) {
        return random().nextInt(s);
    }

    protected Random random() {
        return ThreadLocalRandom.current();
    }


    /**
     * size > 0
     */
    protected int sampleStart(int size) {
        if (size == 1) return 0;
        else
            return random().nextInt(size);
    }

    /**
     * chooses a starting index randomly then iterates descending the list
     * of items. if the sampling is not finished it restarts
     * at the top of the list. so for large amounts of samples
     * it will be helpful to call this in batches << the size of the bag.
     */
    @NotNull
    @Override
    public Bag<X, Y> sample(@NotNull Bag.BagCursor<? super Y> each) {


        final Object[] ii = items.array();
        int s0 = ii.length;
        if (s0 == 0) return this; //to be safe

        int s = Math.min(s0, size());
        if (s == 0) return this;

        int i = sampleStart(s);

        BagSample next = BagSample.Next;

        /*
        sampled items will be limited to the current array.  if the array has resized by
        an insertion from another thread, it will not be available in this sampling
         */

        boolean direction = random().nextBoolean();
        int nulls = 0; //# of nulls encountered. when this reaches the array length we know it is empty
        while (!next.stop && nulls < s0) {
            Y x = (Y) ii[i];

            if (x != null) {
                next = each.next(x);
                if (next.remove) {

                    //if removed and the bag's array has been changed to a new array while processing:
                    if (remove(key(x)) != null && items.array() != ii) {
                        //set it in this array to not encounter it again
                        ii[i] = null;
                        nulls++;
                    }
                    //modified = true;
                } else {
                    nulls = 0; //reset null count
                }
            } else {
                nulls++;
            }

            if (direction) {
                i++;
                if (i == s) i = 0;
            } else {
                i--;
                if (i == -1) i = s - 1;
            }
        }

//        if (modified) {
//            commit(null);
//        }
        return this;
    }

    @Nullable
    @Override
    public Y remove(@NotNull X x) {
        synchronized (items) {
            return super.remove(x);
        }
    }

    //    @Override
//    public final void putAsync(@NotNull Y b) {
//        toPut.accept(b);
//    }

    @Override
    public final Y put(@NotNull final Y incoming, @Nullable final MutableFloat overflow) {

        if (capacity == 0)
            return null;

        final float p = incoming.pri();
        if (p != p)
            return null; //already deleted

//        if (p < Pri.EPSILON) {
//            if (atCap)
//                return null; //automatically refuse sub-ther
//        }

        X key = key(incoming);

        final boolean[] added = {false};
        final @Nullable List<Y>[] trash = new List[1];
        Y inserted;

        synchronized (items) {

            if (capacity == 0) //check again inside the synch
                return null;

            inserted = map.compute(key, (kk, existing) -> {
                if (existing != null) {
                    if (existing == incoming) {
                        //no change
                        if (overflow != null)
                            overflow.setValue(p);
                    } else {


                        int s = size();
                        boolean atCap = s == capacity;
                        float priBefore = existing.priElseZero();
                        float oo = mergeFunction.merge((Priority) existing /* HACK */, incoming);

                        if (overflow != null)
                            overflow.add(oo);

                        float delta = existing.priElseZero() - priBefore;
                        if (delta >= Pri.EPSILON) {
                            if (atCap) {
                                pressurize(delta);
                            }
                            mustSort = true;
                        }
                    }
                    return existing;

                } else {


                    if (size() == capacity)
                        pressurize(p);

                    @Nullable FasterList<Y> trsh = update(incoming, null);
                    if (trsh != null) {
                        trash[0] = trsh;
                        if (trsh.getLast() == incoming)
                            return null;
                    }

                    added[0] = true;
                    return incoming; //success
                }
            });


            if (trash[0] != null) {
                //clear the entries from the map right away
                //this should be done in a synchronized block along with what happens above
                trash[0].forEach(x -> {
                    if (x != incoming)
                        map.remove(key(x));
                });
            }

            ensureSorted();
        }

        //this can be done outside critical section
        if (trash[0] != null) {
            trash[0].forEach(x -> {
                if (x != incoming)
                    onRemoved(x);
            });
        }
        if (added[0]) {
            onAdded(inserted);
        }


        return inserted;

//        Y y = map.merge(key, incoming, (existing, incoming) -> {
//
//            incomingPri[0] *= -1; //upright
//
//            float oo = existing != incoming ?
//                    mergeFunction.merge((Priority) existing /* HACK */, incoming)
//                    :
//                    incomingPri[0] /* all of it if identical */;
//
//            if (oo >= Pri.EPSILON) {
//                incomingPri[0] -= oo; //release any unabsorbed pressure
//            }
//
//            return existing;
//        });
//        if (incomingPri[0] < 0) {
//
//            if (atCap)
//                pressurize(p); //absorb pressure even if it's about to get removed
//
//
//            synchronized (items) {
//                //check if it can actually exist here
//                if (((size() >= capacity) && (p < min) || !updateItems(y))) {
//                    map.remove(key);
//                    return null;
//                }
//            }
//
//
//            onAdded(y);
//            return y;
//
//        } else {
//
//            float activated = incomingPri[0];
//
//            if (activated >= Pri.EPSILON) {
//
//                unsorted.set(true); //merging may have shifted ordering, so sort later
//
//                if (atCap)
//                    pressurize(activated);
//
//                if (overflow != null) {
//                    float oo = p - activated;
//                    if (oo >= Pri.EPSILON)
//                        overflow.add(oo);
//                }
//            }
//        }

    }


//    @Nullable
//    @Override
//    protected Y addItem(@NotNull Y i) {
//        throw new UnsupportedOperationException();
//    }


    @Override
    @Deprecated
    public Bag<X, Y> commit() {
        double p = this.pressure.getAndSet(0);
        if (p >= Pri.EPSILON) {
            return commit(PriForget.forget(size(), capacity(),
                    (float) p, mass, PriForget.DEFAULT_TEMP, Priority.EPSILON, PriForget::new));
        }
        return this;
    }

    @Override
    @NotNull
    public Bag<X, Y> commit(Consumer<Y> update) {
        commit(update, false);
        return this;
    }

    private void commit(@Nullable Consumer<Y> update, boolean checkCapacity) {

        if (update == null && !checkCapacity)
            return;

        @Nullable FasterList<Y> trash = null;
        synchronized (items) {
            if (size() == 0)
                return;

            trash = update(null, update);

            if (trash != null) {
                trash.forEach(t -> {
                    map.remove(key(t));
                });
            }
            ensureSorted();
        }

        //then outside the synch:
        if (trash != null) {
            trash.forEach(this::onRemoved);
        }

    }


    static int sortSize(int s) {
        //TODO get a better calculation; this is an estimate, probably some ~log2(size) relationship
        if (s < 16)
            return 4;
        if (s < 64)
            return 6;
        if (s < 128)
            return 8;
        if (s < 2048)
            return 16;
        else
            return 32;
    }

//    /**
//     * returns whether the items list was detected to be sorted
//     */
//    private boolean updateBudget(@NotNull Consumer<Y> each) {
////        int dirtyStart = -1;
//        boolean sorted = true;
//
//
//        int s = size();
//        Object[] l = items.array();
//        //@NotNull PLink<V> beneath = l[i]; //compares with self below to avoid a null check in subsequent iterations
//        float pAbove = Float.POSITIVE_INFINITY;
//        for (int i = s - 1; i >= 0; ) {
//            Y b = (Y) l[i];
//
//            float p;
//            if (b == null) {
//                p = -2; //sort nulls to the end of the end
//            } else {
//                p = b.priSafe(-2);
//                if (p >= 0) {
//                    each.accept(b);
//                    p = b.priSafe(-2);
//                }
//            }
//
//            if (pAbove - p < -Priority.EPSILON) {
//                sorted = false;
//            }
//
//            pAbove = p;
//            i--;
//        }
//
//
//        return sorted;
//    }


    public @Nullable Y remove(boolean topOrBottom) {
        @Nullable Y x = topOrBottom ? top() : bottom();
        if (x != null) {
            remove(key(x));
            return x;
        }
        return null;
    }

    @Override
    public void clear() {
        //TODO do onRemoved outside synch
        synchronized (items) {
            //map is possibly shared with another bag. only remove the items from it which are present in items
            items.forEach(x -> {
                map.remove(key(x));
                onRemoved(x);
            });
            items.clear();
        }
    }


//    @Nullable
//    @Override
//    public RawBudget apply(@Nullable RawBudget bExisting, RawBudget bNext) {
//        if (bExisting != null) {
//            mergeFunction.merge(bExisting, bNext, 1f);
//            return bExisting;
//        } else {
//            return bNext;
//        }
//    }


    @Override
    public float pri(@NotNull Y key) {
        return key.pri();
        //throw new UnsupportedOperationException("TODO currently this bag works with PLink.pri() directly");
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super Y> action) {
        Object[] x = items.array();
        for (Object a : (x)) {
            if (a != null) {
                Y b = (Y) a;
                float p = b.pri();
                if (p == p) {
                    action.accept(b);
                    if (--max <= 0)
                        break;
                }
            }


        }

    }

    @Override
    public void forEachKey(@NotNull Consumer<? super X> each) {

        forEach(x -> each.accept(key(x)));
    }

    @Override
    public void forEach(Consumer<? super Y> action) {

        forEach(Integer.MAX_VALUE, action);
    }


    //    public void sortPartial(float sortPercentage) {
//        int s = size();
//        int sortRange = (int) Math.ceil(s * sortPercentage);
//        int start = sampleIndex();
//        int end = Math.min(start + sortRange, s - 1);
//
//        qsort(new int[sortSize(sortRange)], items.array(), start, end);
//    }

    /**
     * http://kosbie.net/cmu/summer-08/15-100/handouts/IterativeQuickSort.java
     */

    public static void qsort(int[] stack, Object[] c, int left, int right) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        while (true) {
            int i, j;
            if (right - left <= 7) {
                //bubble sort on a region of right less than 8?
                for (j = left + 1; j <= right; j++) {
                    Prioritized swap = (Prioritized) c[j];
                    i = j - 1;
                    float swapV = pCmp(swap);
                    while (i >= left && cmpGT((Prioritized) c[i], swapV)) {
                        swap(c, i + 1, i);
                        i--;
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {

                int median = (left + right) / 2;
                i = left + 1;
                j = right;

                swap(c, i, median);

                float cl = pCmp((Prioritized) c[left]);
                float cr = pCmp((Prioritized) c[right]);
                if (cmpGT(cl, cr)) {
                    swap(c, right, left);
                    float x = cr;
                    cr = cl;
                    cl = x;
                }
                float ci = pCmp((Prioritized) c[i]);
                if (cmpGT(ci, cr)) {
                    swap(c, right, i);
                    float x = cr; /*cr = ci;*/
                    ci = x;
                }
                if (cmpGT(cl, ci)) {
                    swap(c, i, left);
                    //float x = cl; cl = ci; ci = x;
                }

                Prioritized temp = (Prioritized) c[i];
                float tempV = pCmp(temp);

                while (true) {
                    while (i < cLenMin1 && cmpLT((Prioritized) c[++i], tempV)) ;
                    while (j > 0 && /* <- that added */ cmpGT(c[--j], tempV)) ;
                    if (j < i) {
                        break;
                    }
                    swap(c, j, i);
                }

                c[left + 1] = c[j];
                c[j] = temp;

                int a, b;
                if ((right - i + 1) >= (j - left)) {
                    a = i;
                    b = right;
                    right = j - 1;
                } else {
                    a = left;
                    b = j - 1;
                    left = i;
                }

                stack[++stack_pointer] = a;
                stack[++stack_pointer] = b;
            }
        }
    }

    public static void swap(Object[] c, int x, int y) {
        Object swap = c[y];
        c[y] = c[x];
        c[x] = swap;
    }

    //    final Comparator<? super PLink<V>> comparator = (a, b) -> {
//        return Float.compare(items.score(b), items.score(a));
//    };


//        if (!v.hasDelta()) {
//            return;
//        }
//
////
////        int size = ii.size();
////        if (size == 1) {
////            //its the only item
////            v.commit();
////            return;
////        }
//
//        SortedIndex ii = this.items;
//
//        int currentIndex = ii.locate(v);
//
//        v.commit(); //after finding where it was, apply its updates to find where it will be next
//
//        if (currentIndex == -1) {
//            //an update for an item which has been removed already. must be re-inserted
//            put(v.get(), v);
//        } else if (ii.scoreBetween(currentIndex, ii.size(), v)) { //has position changed?
//            ii.reinsert(currentIndex, v);
//        }
//        /*} else {
//            //otherwise, it remains in the same position and a move is unnecessary
//        }*/
//    }


    @NotNull
    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public float priMax() {
        return max;
    }

    @Override
    public float priMin() {
        return min;
    }


//    public final void popAll(@NotNull Consumer<PLink<V>> receiver) {
//        forEach(receiver);
//        clear();
//    }

//    public void pop(@NotNull Consumer<PLink<V>> receiver, int n) {
//        if (n == size()) {
//            //special case where size <= inputPerCycle, the entire bag can be flushed in one operation
//            popAll(receiver);
//        } else {
//            for (int i = 0; i < n; i++) {
//                receiver.accept(pop());
//            }
//        }
//    }

//    public final float priAt(int cap) {
//        return size() <= cap ? 1f : item(cap).pri();
//    }
//

//    public final static class BudgetedArraySortedIndex<X extends Budgeted> extends ArraySortedIndex<X> {
//        public BudgetedArraySortedIndex(int capacity) {
//            super(1, capacity);
//        }
//
//
//        @Override
//        public float score(@NotNull X v) {
//            return v.pri();
//        }
//    }

}


//        if (dirtyStart != -1) {
//            //Needs sorted
//
//            int dirtyRange = 1 + dirtyEnd - dirtyStart;
//
//            if (dirtyRange == 1) {
//                //Special case: only one unordered item; remove and reinsert
//                PLink<V> x = items.remove(dirtyStart); //remove directly from the decorated list
//                items.add(x); //add using the sorted list
//
//            } else if ( dirtyRange < Math.max(1, reinsertionThreshold * s) ) {
//                //Special case: a limited number of unordered items
//                PLink<V>[] tmp = new BLink[dirtyRange];
//
//                for (int k = 0; k < dirtyRange; k++) {
//                    tmp[k] = items.remove( dirtyStart /* removal position remains at the same index as items get removed */);
//                }
//
//                //TODO items.get(i) and
//                //   ((FasterList) items.list).removeRange(dirtyStart+1, dirtyEnd);
//
//                for (BLink i : tmp) {
//                    if (i.isDeleted()) {
//                        removeKeyForValue(i);
//                    } else {
//                        items.add(i);
//                    }
//                }
//
//            } else {
//            }
//        }
