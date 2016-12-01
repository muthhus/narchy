package nars.budget;


import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.Nullable;

public interface PriorityAccumulator<X> {
    void add(X x, float v);

    @Nullable Iterable<ObjectFloatPair<X>> commit();
}
