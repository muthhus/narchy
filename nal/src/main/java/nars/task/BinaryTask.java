package nars.task;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/19/17.
 */
abstract public class BinaryTask<X, Y> extends UnaryTask<Pair<X, Y>> {

    protected BinaryTask(X x, Y y, float pri) {
        this(Tuples.pair(x, y), pri);
    }

    protected BinaryTask(@NotNull Pair<X, Y> value, float pri) {
        super(value, pri);
    }

    public final X getOne() {
        return id.getOne();
    }

    public final Y getTwo() {
        return id.getTwo();
    }
}
