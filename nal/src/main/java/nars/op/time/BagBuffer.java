package nars.op.time;

import nars.NAR;
import nars.bag.Bag;
import nars.link.BLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Manages a limited size Bag (priority queue) of items in a continous flow
 */
abstract public class BagBuffer<X>  {


    public final Bag<X> bag;


    public BagBuffer(Bag<X> bag) {
        this.bag = bag;
    }

    public void setCapacity(int c) {
        bag.setCapacity(c);
    }

    @Nullable
    protected final X take() {
        return bag.pop();
    }

    protected final void put(@NotNull X x) {
        bag.put(x);
    }

}
