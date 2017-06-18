package jcog.tree.rtree;

import jcog.list.FasterList;
import jcog.util.UniqueRanker;
import org.eclipse.collections.api.block.SerializableComparator;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RTreeCursor<T> {

    private final Spatialized<T> space;
    List<Leaf<T>> starts = new FasterList();

    public RTreeCursor(Spatialized<T> space, HyperRect start) {
        this.space = space;
        go(start);
    }

    protected void go(HyperRect region) {
        starts.clear();
        space.intersectingNodes(region, (n) -> {
            if (n instanceof Leaf)
                starts.add((Leaf) n);
            return true;
        });
    }

    public void forEach(Consumer<? super T> each) {
        starts.forEach(n -> {
            n.forEach(each);
        });
    }

    public List<T> list() {
        List<T> l = new FasterList();
        forEach(l::add);
        return l;
    }


    public MutableList listSorted(FloatFunction<T> c) {
        ObjectFloatHashMap<T> ranks = new ObjectFloatHashMap<>();
        forEach(x -> ranks.put(x, c.floatValueOf(x)));
        return ranks.keyValuesView().toSortedListBy(ObjectFloatPair::getTwo);
    }


    public List<T> listSorted(Comparator<T> ranker) {
        List<T> l = list();
        Collections.sort(l, ranker);
        return l;
    }

    //listSorted(ranking, topN)...
}
