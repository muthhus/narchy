package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public class TopN<E> extends SortedArray<E> {

    private final FloatFunction<E> rank;
    float minSeen = Float.POSITIVE_INFINITY;

    public TopN(E[] target, FloatFunction<E> rank) {
        this.list = target;
        this.rank = (x) -> -rank.floatValueOf(x); //descending
    }

    @Override
    protected float add(E element, float elementRank, FloatFunction<E> cmp, int size) {
        if (size() == list.length && elementRank < minSeen)
            return Float.NaN; //insufficient

        return super.add(element, elementRank, cmp, size);
    }

    @Override
    public boolean add(E e) {
        float r = add(e, rank);
        if (r == r) {
            if (r < minSeen)
                minSeen = r;
            return true;
        }
        return false;
    }

    @Override
    protected boolean grows() {
        return false;
    }

    @Override
    protected E[] newArray(int oldSize) {
        throw new UnsupportedOperationException();
    }
}
