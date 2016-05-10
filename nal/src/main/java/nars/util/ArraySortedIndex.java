//package nars.util;
//
//import nars.Global;
//import nars.util.data.list.FasterList;
//import nars.util.data.sorted.SortedIndex;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.function.Consumer;
//
////import org.apache.commons.collections.iterators.ReverseListIterator;
//
//
//abstract public class ArraySortedIndex<E> extends SortedIndex<E> {
//
//    protected int capacity;
//
//    final List<E> list;
//
//
//    @Override
//    public final void forEach(@NotNull Consumer<? super E> consumer) {
//        list.forEach(consumer);
//    }
//
//    @Override
//    public final boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (!(obj instanceof ArraySortedIndex)) return false;
//        ArraySortedIndex o = (ArraySortedIndex) obj;
//        return list.equals(o.list);
//    }
//
//    @Override
//    public int hashCode() {
//        return list.hashCode();
//    }
//
//    @Override public final List<E> list() {
//        return list;
//    }
//
//    public ArraySortedIndex(int capacity) {
//        this(1, capacity);
//    }
//
//    public ArraySortedIndex(int initialCapacity, int maxCapacity) {
//        this(Global.newArrayList(initialCapacity), maxCapacity);
//    }
//
//    public ArraySortedIndex(List<E> list, int capacity) {
//        this.list = list;
//        setCapacity(capacity);
//    }
//
//    /**
//     * any scalar decomposition function of a budget value
//     * can be used
//     *  //MODE 0: priority only
//     return b.getPriority();
//
//     //MODE 1:
//     //return b.getBudget().summary();
//
//     //MODE 2:
//     //this ensures that priority is the most significant ordering factor, even if zero
//     /*return (1+b.getPriority())*
//     (b.getDurability()*b.getQuality());
//     */
////    @Override public float score(E b) {
////
////        return b.getPriority();
////
////    }
//
//    @Override
//    public boolean isSorted() {
//        if (size() < 2) return true;
//
//        Iterator<E> ii = iterator();
//        float pp = Float.MAX_VALUE;
//
//        while (ii.hasNext()) {
//            E c = ii.next();
//            float sc = score(c);
//            if (sc > pp)
//                return false;
//            pp = sc;
//        }
//
//        return true;
//    }
//
//    @Override
//    public final void setCapacity(int capacity) {
//
//        if (this.capacity==capacity) {
//            return;
//        }
//
//        this.capacity = capacity;
//
//        List<E> l = list;
//
//            int n = l.size();
//            //remove elements from end
//            for (; n - capacity > 0; n--) {
//                l.remove(n-1);
//            }
//
//
//    }
//
//
//    @Override public final int pos(E o) {
//        return pos(score(o));
//    }
//
//    public final int pos(float score) {
//        int upperBound = 0;
//        int lowerBound = size()-1;
//
//        E[] ll = (E[]) ((FasterList) list).array();
//
//        while (upperBound <= lowerBound) {
//            int mid = (upperBound + lowerBound) /2; // >>> 1;
//
//            //float mp = score(get(mid));
//            float mp = score(ll[mid]);
//
//
//            if (mp <= score) { //midpoint is new upperBound so go to lowerBound half
//
//                lowerBound = mid;
//
//                if (mp < score) //midpoint is new lowerBound, so we need to go to the upper half
//                    lowerBound--;  //mid - 1
//                else
//                    break; // key found
//            } else {
//                upperBound = mid + 1;
//            }
//        }
//        return lowerBound;
//    }
//
//    @Override
//    public final E get(int i) {
//        return list.get(i);
//    }
//
//    @Nullable
//    @Override
//    public E insert(E incoming) {
//
//        E removed = null;
//
//        int s = size();
//
//        int insertPos;
//        if (s == 0) {
//            //first element in empty list, insert at beginning
//            insertPos = 0;
//        } else {
//
//            float incomingScore = score(incoming);
//
//            if (s >= capacity) {
//
//                int lastIndex = size() - 1;
//                float lowestScore = score(get(lastIndex));
//
//                if (incomingScore < lowestScore) {
//                    //priority too low to join this list, bounce
//                    return incoming;
//                }
//
//                removed = remove(lastIndex);
//            }
//            else {
//                removed = null;
//            }
//
//            insertPos = pos(incomingScore);
//
//            if (insertPos < 0)
//                insertPos = 0;
//            else
//                insertPos++;
//
//        }
//
//        list.add(insertPos, incoming);
//
//        return removed;
//    }
//
//    @Override public boolean remove(Object o) {
//        int l = locate(o);
//        if (l!=-1) return remove(l)==o;
//        return true;
//    }
//
//    @Override public int locate(Object o) {
//
//        int s = size();
//        if (s == 0) return -1;
//
//        //estimated position according to current priority,
//        //which if it hasnt changed much has a chance of being
//        //close to the index
//        int p = pos((E)o);
//        if (p >= s) p = s-1;
//        if (p < 0)  p = 0;
//
//        return attemptEqual(this.list, o, p) ?
//                p :
//                locateAt(o, s, p);
//    }
//
//    /**
//     *
//     * @param o object being sought
//     * @param s size
//     * @param p scan start index
//     * @return
//     */
//    private int locateAt(Object o, int s, int p) {
//        int r = 0;
//        int maxDist = Math.max(s - p, p);
//
//        boolean phase = false;
//
//        //scan in an expanding radius around the point
//        List<E> list = this.list;
//        do {
//
//            phase = !phase;
//
//            int u;
//            if (phase) {
//                u = p + (++r);
//                if (u >= s) continue;
//            }
//            else {
//                u = p - r;
//                if (u < 0) continue;
//            }
//
//            if (attemptEqual(list, o, u))
//                return u;
//
//        } while ( r <= maxDist );
//
//        return -1;
//    }
//
//    private static boolean attemptEqual(@NotNull List l, Object o, /*final Object oName, */ int i) {
//        return o == l.get(i);
//    }
//
//
//    @Override
//    public final int capacity() {
//        return capacity;
//    }
//
//    public final int available() {
//        return capacity - size();
//    }
//
//    @Override
//    public String toString() {
//        return list.toString();
//    }
//
////    @NotNull
////    @Override
////    public final Iterator<E> descendingIterator() {
////        //return new ReverseListIterator(list);
////        throw new RuntimeException("unimpl yet");
////    }
//
//    @Override public final E remove(int i) {
//        return list.remove(i);
//    }
//
//    @Override
//    public final int size() {
//        return list.size();
//    }
//
//    @Override public final boolean isEmpty() {
//        return list.isEmpty();
//    }
//
//    /** this is a potentially very slow O(N) iteration,
//      * shouldnt be any reason to use this */
//    @Override public final boolean contains(Object o) {
//        return list.contains(o);
//    }
//
//    /** if possible, use the forEach visitor which wont
//     * incur the cost of allocating an iterator */
//    @Override public final Iterator<E> iterator() {
//        return list.iterator();
//    }
//
//    @Override public final void clear() {
//        list.clear();
//    }
//
//    @NotNull
//    @Override public Object[] toArray() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @NotNull
//    @Override
//    public <T> T[] toArray(T[] a) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//
//    @Override
//    public boolean containsAll(Collection<?> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean addAll(Collection<? extends E> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean removeAll(Collection<?> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean retainAll(Collection<?> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//}