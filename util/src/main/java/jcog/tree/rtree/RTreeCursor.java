package jcog.tree.rtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Ordering;
import jcog.list.FasterList;
import jcog.util.Top;
import jcog.util.Top2;
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
    FasterList<Leaf<T>> active = new FasterList();
    int size = 0;

    public RTreeCursor(Space<T> space) {
        this.space = space;
    }

    public RTreeCursor<T> in(HyperRegion region) {

        if (size!=0) {
            this.size = 0;
            this.active = new FasterList();
        } else {
            this.active.clearFast(); //to be sure
        }

        space.intersectingNodes(region, (n) -> {
            if (n instanceof Leaf)
                addLeaf((Leaf) n);
            return true;
        });

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
        l.sort(ranker);
        return l;
    }


    public List<T> topSorted(FloatFunction<T> ranker, int max) {

        assert(max > 0);

        Iterator<T> iterator = iterator();
        if (iterator == null) return Collections.emptyList();

        if (max == 1) {
            return new Top<T>(ranker).of(iterator).toList();
        } else if (max == 2) {
            return new Top2<T>(ranker).of(iterator).toList();
        } else {
            return ordering(new UniqueRanker(ranker)).greatestOf(iterator, max);
        }
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



    /**
     * untested
     */
    private static final class RCursorIterator<T> extends AbstractIterator<T> {

        List<Leaf<T>> a;
        Leaf<T> l;
        int i = 0, j = 0;

        public RCursorIterator(List<Leaf<T>> active) {
            this.a = active;
            this.l = a.get(0);
        }

        @Override
        protected T computeNext() {
            if (a == null)
                return endOfData();

            @NotNull T next = l.get(i);
            if (++i >= l.size) {

                if (++j >= a.size()) {
                    a = null; //no more nodes after this one
                    l = null; //the next call will terminate it
                } else {
                    l = a.get(j); //next
                    i = 0;
                }

            }

            return next!=null ? next : endOfData(); //why null?
        }
    }
}
