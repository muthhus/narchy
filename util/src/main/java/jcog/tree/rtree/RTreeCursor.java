package jcog.tree.rtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;
import jcog.list.FasterList;
import jcog.util.UniqueRanker;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class RTreeCursor<T> {

    private final Space<T> space;
    List<Leaf<T>> active = new FasterList();
    int size = 0;

    public RTreeCursor(Space<T> space) {
        this.space = space;
    }

    public RTreeCursor<T> in(HyperRegion region) {

        this.size = 0;
        this.active = new FasterList();

        space.intersectingNodes(region, (n) -> {
            if (n instanceof Leaf)
                addLeaf((Leaf) n);
            return true;
        });
        Node<T, ?> root = space.root();
        if (active.isEmpty() && root.isLeaf())
            addLeaf((Leaf) root);

        return this;
    }

    private void addLeaf(Leaf n) {
        active.add(n);
        size += n.size;
    }

    public void forEach(Consumer<? super T> each) {
        active.forEach(n -> {
            n.forEach(each);
        });
    }

    public FasterList<T> list() {
        FasterList<T> l = new FasterList();
        forEach(l::add);
        return l;
    }

    @Nullable
    protected Iterator<T> iterator() {

        if (size == 0)
            return null;

        return new RCursorIterator<>(active);
    }


    public MutableList<T> listSorted(FloatFunction<T> c) {
        return listSorted(new UniqueRanker<>(c));
    }


    public FasterList<T> listSorted(Comparator<T> ranker) {
        FasterList<T> l = list();
        Collections.sort(l, ranker);
        return l;
    }

    public List<T> topSorted(FloatFunction<T> ranker, int max) {
        return topSorted(new UniqueRanker<>(ranker), max);
    }

    public List<T> topSorted(Comparator<T> cmp, int max) {

        Iterator<T> iterator = iterator();
        if (iterator == null) return Collections.emptyList();

        return ordering(cmp).greatestOf(iterator, max);
    }
   public List<T> topSorted(Ordering<T> cmp, int max) {
        return cmp.greatestOf(iterator(), max);
    }

    public static <T> Ordering<T> ordering(Comparator<T> cmp) {
        return Ordering.from(cmp);
    }
    public static <T> Ordering<T> ordering(FloatFunction<T> cmp) {
        return Ordering.from(new UniqueRanker<>(cmp));
    }

    public int size() {
        return size;
    }


    /** untested */
    private class RCursorIterator<T> extends AbstractIterator<T> {

        List<Leaf<T>> a;
        Leaf<T> l;
        int i = 0, j = 0;

        public RCursorIterator(List<Leaf<T>> active) {
            this.a = active;
            this.l = a.get(0);
        }

        @Override protected T computeNext() {
            @NotNull T next = l.get(i++);
            if (i >= l.size) {

                if (a!=null) {
                    ++j;
                    i = 0;
                    l = null;
                    if (j >= a.size()-1) {
                        a = null; //no more nodes after this one
                    } else {
                        l = a.get(j); //next
                    }

                } else {
                    l = null; //no more items in the last node
                }
            }

            if (a == null && l == null) {
                endOfData();
            }

            return next;
        }
    }
}
