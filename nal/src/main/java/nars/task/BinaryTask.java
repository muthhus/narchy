package nars.task;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/19/17.
 */
abstract public class BinaryTask<X, Y> extends UnaryTask<Pair<X, Y>> {

    public BinaryTask(X x, Y y, float pri) {
        this(Tuples.pair(x, y), pri);
    }

    public BinaryTask(@NotNull Pair<X, Y> value, float pri) {
        super(value, pri);
    }

}
