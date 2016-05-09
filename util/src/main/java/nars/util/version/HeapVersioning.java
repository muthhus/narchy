package nars.util.version;

import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;

/** allocates supporting instances on the heap */
public class HeapVersioning extends Versioning {


    private final int stackSize;

    public HeapVersioning(int capacity, int stackSize) {
        super(capacity);
        this.stackSize = stackSize;
    }

    @Override
    public <X> FasterList<X> newValueStack() {
        return new FasterList(1);
    }

    @Override
    public int[] newIntStack() {
        return new int[stackSize];
    }


}
