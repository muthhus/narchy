package nars.util.version;

import nars.util.data.DequePool;
import nars.util.data.list.FasterList;

/**
 * Created by me on 5/9/16.
 */
public final class PooledVersioning extends Versioning {
    final DequePool<FasterList> valueStackPool;
    final DequePool<int[]> intStackPool;

    public PooledVersioning(int capacity, int stackLimit) {
        this(capacity, stackLimit, null);
    }

    public PooledVersioning(int capacity, int stackLimit, PooledVersioning toSharePool) {
        super(capacity);

        if (toSharePool != null) {
            this.valueStackPool = toSharePool.valueStackPool;
            this.intStackPool = toSharePool.intStackPool;
        } else {
            this.valueStackPool = new FasterListDequePool(capacity, stackLimit);
            this.intStackPool = new intDequePool(capacity, stackLimit);
        }
    }

    @Override
    public final <X> FasterList<X> newValueStack() {
        //from heap:
        //return new FasterList(16);

        //object pooling value stacks from context:
        return valueStackPool.get();
    }

    @Override
    public final int[] newIntStack() {
        return intStackPool.get();
    }

    @Override
    public <Y> void delete(Versioned<Y> v) {
        super.delete(v);
        valueStackPool.put(v.value);
        intStackPool.put(v.array());
    }
}
