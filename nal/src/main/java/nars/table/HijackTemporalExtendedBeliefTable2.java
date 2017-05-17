package nars.table;

import jcog.list.Top2;
import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.LockingRTree;
import jcog.tree.rtree.Node;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.point.Long1D;
import jcog.tree.rtree.rect.RectLong1D;
import nars.$;
import nars.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * extends ListTemporalBeliefTable with additional read-only history which tasks, once removed from the
 * main (parent) belief table, may be allowed to populate.
 */
abstract public class HijackTemporalExtendedBeliefTable2 extends HijackTemporalBeliefTable
        implements Function<Task, HyperRect<Long1D>> {

    final LockingRTree<Task> history;
    private final int historicCapacity;

    public HijackTemporalExtendedBeliefTable2(int initialCapacity, int historicCapacity) {
        super(initialCapacity);
        this.historicCapacity = historicCapacity;
        this.history = new LockingRTree<>(new RTree(this), new ReentrantReadWriteLock(false));
    }

    @Override
    public @Nullable Task match(long when, long now, int dur, @Nullable Task against) {
        Task t = super.match(when, now, dur, against);

        Task h = matchHistory(when);
        if (h != null) {
            if ((t == null) || (h!=t && h.evi(when, dur) > t.evi(when, dur))) {
                return h;
            }
        }

        return t;
    }

    protected Task ressurect(Task t) {
        t.priMax(0.5f * t.conf() /* HEURISTIC */);
        return t;
    }

    @Override
    public Truth truth(long when, int dur, @Nullable EternalTable eternal) {
        Truth a = super.truth(when, dur, eternal);
        Task h = matchHistory(when);
        if (h != null) {
            Truth b = h.truth(when, dur);
            if (b!=null && (a == null || b.evi() > a.evi()))
                return b;
        }
        return a;
    }

    //TODO use a better method:
    Task matchHistory(long when) {

        //TODO either write Top1 or use Top2, or a Top2 with a separate ranking bi-float-function for comparing #2's to #1's
        Top2<Task> found = new Top2<>(t -> t.pri());
        history.intersecting(new RectLong1D(when), (x) -> {
            found.accept(x);
            return true;
        });
        return found.a;
    }

    @Override
    public void clear() {
        //super.clear();
        throw new UnsupportedOperationException();
    }

    @Override
    public void onRemoved(Task x) {
        if (include(x)) {

            ressurect(x);

            int toRemove = history.size() + 1 - historicCapacity;
            for (int i = 0; i < toRemove; i++)
                removeOldest(/*toRemove*/);

//            synchronized (history) {
//                int toRemove = ;
//                for (int i = 0; i < toRemove; i++) {
//                    Task t = history.pollFirstEntry().getValue();
//                    super.onRemoved(t);
//                }


            history.add(x);
//            }

        } else {
            super.onRemoved(x);
        }
    }


    private void removeOldest(/*int num*/) {

        Task[] found = new Task[1];

        //after each one is removed, call super.remove(x)
        history.read(h -> {
            Node<Task> n = h.getRoot();
            long min = n.bounds().min().coord(0);

            n.intersecting(new RectLong1D(min), (t) -> {
                found[0] = t;
                return false;
            });


        });

        for (Task x : found) {
            if (x == null)
                break;

            boolean removed = history.remove(x);
            assert(removed);

            super.onRemoved(x);
        }
    }

    /**
     * whether a given task should be stored in this
     */
    abstract protected boolean include(Task t);

    @Override
    public HyperRect<Long1D> apply(Task task) {
        return new RectLong1D(task.start(), task.end());
    }
}
