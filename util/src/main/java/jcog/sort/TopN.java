package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

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
    protected float add(E element, float elementRank, FloatFunction<E> cmp, int size) {
        if (size() == list.length && elementRank < minSeen)
            return Float.NaN; //insufficient

        float r = super.add(element, elementRank, cmp, size);
        if (r==r) {
            //added
            if (elementRank < minSeen)
                minSeen = elementRank;
        }
        return r;
    }

    @Override
    public boolean add(E e) {
        float r = add(e, rank);
        return r == r;
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
}
