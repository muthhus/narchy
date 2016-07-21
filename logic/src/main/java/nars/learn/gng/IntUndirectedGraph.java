package nars.learn.gng;

import com.gs.collections.api.block.predicate.primitive.IntPredicate;
import com.gs.collections.api.block.procedure.primitive.ShortIntProcedure;
import com.gs.collections.api.block.procedure.primitive.ShortProcedure;

public interface IntUndirectedGraph {

    void clear();

    void setEdge(short first, short second, int value);

    void edgesOf(short vertex, ShortIntProcedure eachKeyValue);

    void edgesOf(short vertex, ShortProcedure eachKey);

    void removeEdgeIf(IntPredicate filter);

    void addToEdges(short i, int d);

    default void compact() { /* by default nothing needs to be done, this is implementation specific */ }

    void removeVertex(short v);

    void removeEdge(short first, short second);

}
