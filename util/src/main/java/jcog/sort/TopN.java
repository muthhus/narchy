package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TopN<E> extends SortedArray<E> implements Consumer<E>  {

    private final FloatFunction<E> rank;
    float minSeen = Float.POSITIVE_INFINITY;

    public TopN(E[] target, FloatFunction<E> rank) {
        this.list = target;
        this.rank = (x) -> -rank.floatValueOf(x); //descending
    }

//    /**
//     * resets the best values, effectively setting a the minimum entry requirement
//     * untested
//     */
//    public TopN min(float min) {
//        this.minSeen = min;
//        return this;
//    }

    @Override
    public int add(E element, float elementRank, FloatFunction<E> cmp) {
//        if (size == list.length && elementRank < minSeen)
//            return -1; //insufficient

        int r = super.add(element, elementRank, cmp);
        if (r>=0) {
//            //added
//            if (elementRank < minSeen)
//                minSeen = elementRank;
        }
        return r;
    }

    @Override
    public boolean add(E e) {
        int r = add(e, rank);
        return r >= 0;
    }

    @Override
    protected boolean grows() {
        return false;
    }

    @Override
    protected E[] newArray(int oldSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void accept(E e) {
        add(e);
    }

    public E pop() {
        int s = size();
        if (s == 0) return null;
        return removeFirst();
    }

    @Override
    public E remove(int index) {
        E e = super.remove(index);
        minSeen = Float.POSITIVE_INFINITY;
        return e;
    }

    @Override
    public void removeFast(int index) {
        throw new UnsupportedOperationException();
    }
}
