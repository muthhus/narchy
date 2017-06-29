package jcog.tree.rtree;

import jcog.list.FasterList;
import jcog.util.UniqueRanker;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class RTreeCursor<T> {

    private final Space<T> space;
    List<Leaf<T>> starts = new FasterList();

    public RTreeCursor(Space<T> space, HyperRegion start) {
        this.space = space;
        go(start);
    }

    protected void go(HyperRegion region) {
        starts.clear();
        space.intersectingNodes(region, (n) -> {
            if (n instanceof Leaf)
                starts.add((Leaf) n);
            return true;
        });
        if (starts.isEmpty() && space.root().isLeaf())
            starts.add((Leaf)space.root());
    }

    public void forEach(Consumer<? super T> each) {
        starts.forEach(n -> {
            n.forEach(each);
        });
    }

    public FasterList<T> list() {
        FasterList<T> l = new FasterList();
        forEach(l::add);
        return l;
    }


    public MutableList<T> listSorted(FloatFunction<T> c) {
        return listSorted(new UniqueRanker<>(c));
    }


    public FasterList<T> listSorted(Comparator<T> ranker) {
        FasterList<T> l = list();
        Collections.sort(l, ranker);
        return l;
    }

    //listSorted(ranking, topN)...
}
