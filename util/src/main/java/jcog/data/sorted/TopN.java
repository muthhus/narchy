package jcog.data.sorted;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public class TopN<E> extends SortedArray<E> {

    private final FloatFunction<E> rank;

    public TopN(E[] target, FloatFunction<E> rank) {
        this.list = target;
        this.rank = (x) -> -rank.floatValueOf(x); //descending
    }

    @Override
    public boolean add(E e) {
        add(e, rank);
        return true; //TODO return if it was actually inserted
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
