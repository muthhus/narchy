package nars.table;

import jcog.tree.rtree.*;
import jcog.tree.rtree.point.LongND;
import jcog.tree.rtree.rect.RectLong1D;
import jcog.tree.rtree.rect.RectLongND;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.SignalTask;
import nars.task.TruthPolation;
import nars.truth.Truth;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.*;

public class RTreeBeliefTable implements TemporalBeliefTable, Function<Task, HyperRect<LongND>> {

    static final int radius = 16;

    final Space<Task> tree;

    final Map<SignalTask, HyperRect> activeSignals = new ConcurrentHashMap<>();

    private long lastUpdate = Long.MIN_VALUE;
    private final AtomicBoolean compressing = new AtomicBoolean(false);

    public RTreeBeliefTable() {
        this.tree = new ConcurrentRTree<Task>(
                new RTree<Task>((Function)this, 2, 4, Spatialization.DefaultSplits.AXIAL) {

            @Override
            public boolean add(Task task) {
                if (task instanceof SignalTask)
                    activeSignals.put((SignalTask) task, tree.bounds(task));
                return super.add(task);
            }

            @Override
            public boolean remove(Task x) {
                if (super.remove(x)) {
                    if (x instanceof SignalTask)
                        activeSignals.remove(x);
                    return true;
                } else return false;
            }
        });
    }

    public void updateSignalTasks(long now) {

        if (this.lastUpdate == now)
            return;

        this.lastUpdate = now;


        List<Task> inactivated = $.newArrayList();
        activeSignals.replaceAll((x, prev) -> {

            if (x.growing()) {
                RectLongND next = (RectLongND) tree.bounds(x);

                if (!next.equals(prev)) {
                    //bounds changed
                    boolean removed = tree.remove(x, prev);
                    if (!removed) { //has been deleted otherwise
                        inactivated.add(x); return prev;
                    }
                    boolean added = tree.add(x /*, next*/);
                    assert(added);
                    return next;
                }
                return prev;
            }
            inactivated.add(x); return prev;
        });
        inactivated.forEach(activeSignals::remove);

    }

    public RTreeBeliefTable(int cap) {
        this();
        setCapacity(cap);
    }

    private int capacity;

    @Override
    public Truth truth(long when, long now, int dur, EternalTable eternal) {

        updateSignalTasks(now);

        List<Task> tt = cursor(when - radius * dur, when + radius * dur).list();
        @Nullable Task e = eternal != null ? eternal.strongest() : null;
        if (!tt.isEmpty())
            return TruthPolation.truth(e, when, dur, tt);
        else
            return e != null ? e.truth() : null;
    }

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {

        updateSignalTasks(now);

        MutableList<ObjectFloatPair<Task>> tt = timeDistanceSortedList(when - radius * dur, when + radius * dur, now);
        switch (tt.size()) {
            case 0:
                return null;
            case 1:
            default: //TODO for default case, use Top2 or clustering
                return tt.get(0).getOne();
        }
    }

    protected MutableList<ObjectFloatPair<Task>> timeDistanceSortedList(long start, long end, long now) {
        RTreeCursor<Task> c = cursor(start, end);
        return c.listSorted((t) -> t.timeDistance(now));
    }

    private RTreeCursor<Task> cursor(long start, long end) {
        return tree.cursor(timeRange(start, end));
    }

    private HyperRect timeRange(long a, long b) {
        return new RectLongND(new long[]{a, Long.MIN_VALUE}, new long[]{b, Long.MAX_VALUE});
    }


    @NotNull @Override
    public HyperRect<LongND> apply(@NotNull Task task) {
        long start = task.start();
        long end = task.end();

        int freq = dither32(task.freq());
        int conf = dither32(task.conf());
        long truth = (((long)freq) << 32) /* most significant 32 bits */ | conf;

        return new RectLongND(
                new long[]{start, truth},
                new long[]{end, truth}
        );
    }

    /**
     * map a float value (in range of 0..1 to an integer value (unsigned 31 bits)
     */
    private static int dither32(float x) {
        return (int) (x * (1 << 31));
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compress on shrink
    }

    @Override
    public void add(@NotNull Task t, TaskConcept c, NAR n) {
        float activation = t.priElseZero();
        if (activation == 0)
            return;

        final Task found = find(t);
        if (found == null) {
            tree.addAsync(t);

            int over = size() - capacity;
            if (over > 0)
                compress(n.time());

        } else {
            if (t != found) {
                float overflow = found.priAddOverflow(activation);
                activation -= overflow;
            }
        }

        if (activation > 0)
            TaskTable.activate(t, activation, c, n);

    }

    private void compress(long now) {
        if (compressing.compareAndSet(false, true)) {
            try {
                while (size() > capacity) {
                    compressNext(now);
                }
            } finally {
                compressing.set(false);
            }
        }
    }

    protected void compressNext(long now) {

        //tree.forEachLeaf...
        List<Task> victims = $.newArrayList();
        compressNext(tree.root(), victims, now);

    }

    private void compressNext(Node<Task> next, List<Task> victims, long now) {
        if (next instanceof Leaf) {
            Leaf<Task> l = (Leaf)next;


            //options
            //a. remove by heuristic
            //  1. the weakest task
            //  2. the oldest task
            //b. merge 2 or more into a revision
            //c. do nothing, and/or try another leafnode
            //d. pick a random task and try removing it (HACK)
            Object[] ld = l.data;
            Task r = (Task) ld[ThreadLocalRandom.current().nextInt(l.size)];
            victims.add(r);
            if (r instanceof SignalTask) {
                HyperRect curBounds = activeSignals.remove(r);
                if (curBounds!=null) {
                    tree.remove(r, curBounds);
                    return;
                }
            }

            tree.remove(r);

        } else if (next instanceof Branch) {
            Branch<Task> b = (Branch)next;

            //options:
            //a. smallest
            //b. oldest
            //c. other metrics

            Node<Task> oldest = b.childMin(c -> {
                RectLongND cb = (RectLongND) c.bounds();
                return //(float)cb.perimeter() +
                    -Math.max(abs(cb.min().coord(0) - now ),abs(cb.max().coord(0) - now ));
            });

            compressNext(oldest, victims, now);
        } else {
            throw new UnsupportedOperationException();
        }
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
