package nars.table;

import jcog.list.LimitedFasterList;
import jcog.tree.rtree.*;
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

import java.io.PrintStream;
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
        this.tree = new LockingRTree<Task>(
                new RTree(this, 2,8, RTreeModel.DefaultSplits.LINEAR),
                new ReentrantReadWriteLock(false));
    }
    public RTreeBeliefTable(int cap) {
        this();
        setCapacity(cap);
    }

    private int capacity;

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {

        //HACK
        int window = 8;
        long radius = dur * window;
        LimitedFasterList<Task> m = new LimitedFasterList(8);
        tree.intersecting(new RectLong1D(when- radius, when+radius), (t) -> m.add(t) );
        if (!m.isEmpty())
            return m.get(0);
        else
            return null;
    }
      @Override
    public Truth truth(long when, int dur, EternalTable eternal) {
        //HACK
        List<Task> tt = $.newArrayList(2);
        tree.intersecting(new RectLong1D(when - dur * 4, when + dur * 4), tt::add);
        @Nullable Task e = eternal != null ? eternal.strongest() : null;
        if (!tt.isEmpty())
            return TruthPolation.truth(e, when, dur, tt);
        else
            return e!=null ? e.truth() : null;
    }

    @Override
    public HyperRect<Long1D> apply(Task task) {
        return !task.isInput() ? new RectLong1D(task.start(), task.end()) : new RectLong1D(task.start(), task.start());
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compress on shrink
    }

    @Override
    public void add(@NotNull Task t, TaskConcept c, NAR n) {
        float activation = t.priElseZero();
        if (activation==0)
            return;

        final Task found = find(t);
        if (found==null) {
            tree.add(t);
        } else {
            if (t!=found) {
                float overflow = found.priAddOverflow(activation);
                activation -= overflow;
            }
        }

        if (activation > 0)
            TaskTable.activate(t, activation, c, n);

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


    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int size() {
        return tree.size();
    }

    @Override
    public Iterator<Task> taskIterator() {
        //return id.keySet().iterator();

        //TODO tree.stream().iterator()
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        tree.forEach(x);
    }

    @Override
    public boolean removeTask(Task x) {
        return tree.remove(x);
    }

    @Override
    public void clear() {
        tree.clear();
    }

    public void print(PrintStream out) {
        forEachTask(out::println);
        tree.stats().print(out);
    }
}
