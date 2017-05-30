package nars.table;

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.LockingRTree;
import jcog.tree.rtree.Node;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.point.Long1D;
import jcog.tree.rtree.rect.RectLong1D;
import nars.$;
import nars.Task;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
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
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {
        Task t = super.match(when, now, dur, against, rng);

        Task h = historicTask(when, dur);
        if (h != null) {
            if ((t == null) || (h != t && h.evi(when, dur) > t.evi(when, dur))) {
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
        Truth b = historicTruth(when, dur, 4 /* HEURISTIC */);
        if (a == null)
            return b;
        if (b == null)
            return a;

        return Revision.revise(a, b);
    }

    @Nullable Truth historicTruth(long when, int dur, int limit) {

        List<Task> t = $.newArrayList(limit);
        history.intersecting(new RectLong1D(when - dur, when + dur), (x) -> {
            t.add(x);
            return t.size() < limit;
        });
        switch (t.size()) {
            case 0:
                return null;
            default:
                return TruthPolation.truth(null, when, dur, t);
        }

    }

    //TODO use a better method:
    @Nullable Task historicTask(long when, int dur) {

        //TODO either write Top1 or use Top2, or a Top2 with a separate ranking bi-float-function for comparing #2's to #1's
        Task[] found = new Task[1];
        final float[] bestEvi = {0};
        history.intersecting(new RectLong1D(when - dur, when + dur), (x) -> {
            float evi = x.evi(when, dur);
            if (bestEvi[0] < evi) {
                bestEvi[0] = evi;
                found[0] = x;
            }
            return true;
        });
        return found[0];
    }

    @Override
    public void clear() {
        //super.clear();
        throw new UnsupportedOperationException();
    }

//    @Override
//    public void add(@NotNull Task x, TaskConcept c, NAR n) {
//        if (history.)
//        super.add(x, c, n);
//    }

    @Override
    public void onRemoved(Task x) {
        if (include(x)) {

            ressurect(x);

            List<Task> removing = $.newArrayList();

            history.read(h -> {

                int toRemove = history.size() + 1 - historicCapacity;
                for (int i = 0; i < toRemove; i++) {

                    //after each one is removed, call super.remove(x)
                    Node<Task> n = h.getRoot();

                    long min = n.bounds().min().coord(0);

                    n.intersecting(new RectLong1D(min), (t) -> {
                        removing.add(t);
                        return false;
                    });


                }
            });

            if (!removing.isEmpty()) {
                history.removeAll(removing);
                for (Task x1 : removing) {
                    if (x1 == null)
                        break;

                    super.onRemoved(x1);
                }
            }

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


    /**
     * whether a given task should be stored in this
     */
    abstract protected boolean include(Task t);

    @Override
    public HyperRect<Long1D> apply(Task task) {
        return new RectLong1D(task.start(), task.end());
    }
}
