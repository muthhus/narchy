package nars.table;

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.LockingRTree;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Spatialized;
import jcog.tree.rtree.point.Long1D;
import jcog.tree.rtree.rect.RectLong1D;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.TruthPolation;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class RTreeBeliefTable implements TemporalBeliefTable, Function<Task, HyperRect<Long1D>> {

//    final AtomicInteger serial = new AtomicInteger(0);
//    final ConcurrentHashMap<Task,Integer> id = new ConcurrentHashMap<>();
    //final com.metamx.collections.spatial.RTree tree = new com.metamx.collections.spatial.RTree(1);
    final Spatialized<Task> tree;

    public RTreeBeliefTable() {
        this.tree = new LockingRTree<Task>(new RTree(this), new ReentrantReadWriteLock(false));
    }

    private int capacity;

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {
        return null;
    }

    @Override
    public HyperRect<Long1D> apply(Task task) {
        return !task.isInput() ? new RectLong1D(task.start(), task.end()) : new RectLong1D(task.start(), task.start());
    }

    @Override
    public Truth truth(long when, int dur, EternalTable eternal) {
        List<Task> tt = $.newArrayList(2);
        tree.intersecting(new RectLong1D(when - dur * 4, when + dur * 4), tt::add);
        @Nullable Task e = eternal != null ? eternal.strongest() : null;
        if (!tt.isEmpty())
            return TruthPolation.truth(e, when, dur, tt);
        else
            return e.truth();
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compression
    }

    @Override
    public void add(@NotNull Task t, TaskConcept c, NAR n) {
        final Task found = find(t);
        if (found==null) {
            tree.add(t);
        } else {
            if (t!=found)
                merge(found, t);
        }
//        id.compute(t, (tt, pp) -> {
//            if (pp==null) {
//                int s = serial.getAndIncrement();
//                tree.insert(coord(t), s);
//                return s;
//            } else {
//                merge(tt, t);
//                return pp;
//            }
//        });
    }

    protected Task find(@NotNull Task t) {
        final Task[] found = {null};
        tree.intersecting(apply(t), (x) -> {
           if (x.equals(t)) {
               found[0] = x;
               return false;
           }
            return true;
        });
        return found[0];
    }


    private Task merge(@NotNull Task exist, @NotNull Task incoming) {
        exist.merge(incoming);
        return exist;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int size() {
        return tree.size();
        //return id.size();
    }

    @Override
    public Iterator<Task> taskIterator() {
        //return id.keySet().iterator();

        //TODO tree.stream().iterator()
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean removeTask(Task x) {
        return tree.remove(x);
//        final boolean[] removed = {false};
//        id.computeIfPresent(x, (xx, pi) -> {
//            tree.remove(coord(xx), pi);
//            removed[0] = true;
//            return null;
//        });
//        return removed[0];
    }

    public static float[] coord(Task x) {
        //if (x.isInput()) {
            //only store this by the start because the end is allowed to tretch
        //TODO handle this loss of precisoin
            return new float[]{ (float)(x.start()) };
        //}
    }
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
        //tree.clear();
    }
}
