package nars.table;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import jcog.tree.rtree.*;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.Revision;
import nars.task.SignalTask;
import nars.task.TruthPolation;
import nars.truth.Truth;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.Math.abs;

public class RTreeBeliefTable implements TemporalBeliefTable {

    static final int radius = 32;


    public static class TaskRegion implements HyperRegion {

        public final long start;
        long end; //allow end to stretch for ongoing tasks

        public final float freqMin, freqMax, confMin, confMax;

        @Nullable
        public final Task task;

        public TaskRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
            this(start, end, freqMin, freqMax, confMin, confMax, null);
        }

        public TaskRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax, Supplier<Task> task) {
            this.start = start;
            this.end = end;
            this.freqMin = freqMin;
            this.freqMax = freqMax;
            this.confMin = confMin;
            this.confMax = confMax;
            this.task = null;
        }

        public TaskRegion(@NotNull Task task) {
            this.task = task;
            this.start = task.start();
            this.end = task.end();
            this.freqMin = this.freqMax = task.freq();
            this.confMin = this.confMax = task.conf();
        }

        /** all inclusive time region */
        public TaskRegion(long a, long b) {
            this(a, b, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }


        /**
         * computes a mbr of the given regions
         */
        public static TaskRegion mbr(TaskRegion x, TaskRegion y) {
            if (x == y) return x;
            return new TaskRegion(
                    Math.min(x.start, y.start), Math.max(x.end, y.end),
                    Math.min(x.freqMin, y.freqMin), Math.max(x.freqMax, y.freqMax),
                    Math.min(x.confMin, y.confMin), Math.max(x.confMax, y.confMax)
            );
        }

        @Override
        public int dim() {
            return 3;
        }

        @Override
        public TaskRegion mbr(HyperRegion r) {
            return TaskRegion.mbr(this, (TaskRegion) r);
        }

        @Override
        public double coord(boolean maxOrMin, int dimension) {
            switch (dimension) {
                case 0:
                    return maxOrMin ? end : start;
                case 1:
                    return maxOrMin ? freqMax : freqMin;
                case 2:
                    return maxOrMin ? confMax : confMin;
            }
            throw new UnsupportedOperationException();
        }

        public void updateOngoingTask() {

            this.end = task.end();

        }

        public boolean hasStretched() {
            return this.end != task.end();
        }

        public boolean isDeleted() {
            return task!=null && task.isDeleted();
        }
    }

    final Space<TaskRegion> tree;
    final Set<TaskRegion> ongoing = Sets.newConcurrentHashSet();

    private long lastUpdate = Long.MIN_VALUE;
    private final AtomicBoolean compressing = new AtomicBoolean(false);

    public RTreeBeliefTable() {
        this.tree = new ConcurrentRTree<TaskRegion>(
                new RTree<TaskRegion>((t -> t), 2, 3, Spatialization.DefaultSplits.AXIAL) {

                    @Override
                    public boolean add(TaskRegion tr) {
                        Task task = tr.task;
                        if (task instanceof SignalTask)
                            ongoing.add(tr);
                        return super.add(tr);
                    }

                    @Override
                    public boolean remove(TaskRegion tr) {
                        if (super.remove(tr)) {
                            Task task = tr.task;
                            if (task instanceof SignalTask)
                                ongoing.remove(tr);
                            return true;
                        } else return false;
                    }
                });
    }

    public void updateSignalTasks(long now) {

        if (this.lastUpdate == now)
            return;

        this.lastUpdate = now;

        ongoing.removeIf((r) -> {

            if (r.task.isDeleted())
                return true; //stop tracking

            if (r.hasStretched()) {


                boolean removed = tree.remove(r);
                if (removed) {

                    r.updateOngoingTask();

                    tree.addAsync(r);

                    return false; //keep tracking
                } else {
                    return true; //stop tracking, this task was not in the tree
                }
            }

            return true; //stop tracking
        });


    }

    public RTreeBeliefTable(int cap) {
        this();
        setCapacity(cap);
    }

    private int capacity;

    @Override
    public Truth truth(long when, long now, int dur, EternalTable eternal) {

        updateSignalTasks(now);

        List<TaskRegion> tt = cursor(when - radius * dur, when + radius * dur).list();
        @Nullable Task e = eternal != null ? eternal.strongest() : null;
        if (!tt.isEmpty())
            return TruthPolation.truth(e, when, dur, Iterables.transform(tt, t -> t.task) );
        else
            return e != null ? e.truth() : null;
    }

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {

        updateSignalTasks(now);

        MutableList<ObjectFloatPair<TaskRegion>> tt = timeDistanceSortedList(when - radius * dur, when + radius * dur, now);
        switch (tt.size()) {
            case 0:
                return null;
            case 1:
            default: //TODO for default case, use Top2 or clustering
                return tt.get(0).getOne().task;
        }
    }

    protected MutableList<ObjectFloatPair<TaskRegion>> timeDistanceSortedList(long start, long end, long now) {
        RTreeCursor<TaskRegion> c = cursor(start, end);
        return c.listSorted((t) -> t.task.timeDistance(now));
    }

    private RTreeCursor<TaskRegion> cursor(long start, long end) {
        return tree.cursor(timeRange(start, end));
    }

    private HyperRegion timeRange(long a, long b) {
        return new TaskRegion(a, b);
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

        TaskRegion tr = new TaskRegion(t);
        final Task found = find(tr);
        if (found == null) {

            int over = size() + 1 - capacity;
            if (over > 0) {
                ConcurrentRTree<TaskRegion> lt = ((ConcurrentRTree) tree);
                lt.withWriteLock((r)->{
                    r.add(tr);
                    compress(n);
                });
            } else {
                add(t);
            }


        } else {
            if (t != found) {
                float overflow = found.priAddOverflow(activation);
                activation -= overflow;
            }
        }

        if (activation > 0)
            TaskTable.activate(t, activation, n);

    }

    private void add(@NotNull Task t) {
        tree.addAsync(new TaskRegion(t));
    }

    private void compress(NAR nar) {
        if (compressing.compareAndSet(false, true)) {
            try {
                while (size() > capacity) {
                    compressNext(nar);
                }
            } finally {
                compressing.set(false);
            }
        }
    }

    protected void compressNext(NAR nar) {

        //tree.forEachLeaf...
        compressNext(tree.root(), nar);

    }

    private void compressNext(Node<TaskRegion, ?> next, NAR nar) {
        if (next instanceof Leaf) {
            Leaf<TaskRegion> l = (Leaf) next;

            int size = l.size;
            Object[] ld = l.data;

            //1. remove any deleted tasks
            int deleted = 0;
            for (int i = 0; i < size; i++) {
                Object x = ld[i];
                if (x == null)
                    continue;
                TaskRegion t = (TaskRegion) x;
                if (t.isDeleted()) {
                    deleted++;
                    removeAsync(t);
                }
            }
            if (deleted > 0)
                return; //done already

            //options
            //a. remove by heuristic
            //  1. the weakest task
            //  2. the oldest task
            //b. merge 2 or more into a revision
            //c. do nothing, and/or try another leafnode


            {
                if (l.size > 1) {
                    //e. merge 0 and 1
                    TaskRegion a = (TaskRegion) ld[0];
                    if (a != null) {
                        TaskRegion b = (TaskRegion) ld[1];
                        if (b != null) {
                            Task c = Revision.merge(a.task, b.task, nar.time(), Param.TRUTH_EPSILON, nar.random());
                            if (c != null) {
                                removeAsync(a);
                                removeAsync(b);
                                add(c);
                                TaskTable.activate(c, c.pri(), nar);
                                return;
                            }
                        }
                    }
                }

                {
                    //d. pick a random task and try removing it (HACK)
                    TaskRegion r = (TaskRegion) ld[nar.random().nextInt(l.size)];
                    removeAsync(r);
                }

            }

        } else if (next instanceof Branch) {
            Branch<TaskRegion> b = (Branch) next;

            //options:
            //a. smallest
            //b. oldest
            //c. other metrics

            long now = nar.time();

            HyperRegion branchBounds = next.bounds();
            double branchTimeRange = branchBounds.range(0);
            double branchFreqRange = branchBounds.range(1);

            Node<TaskRegion, ?> oldest = b.childMin(c -> {
                TaskRegion cb = (TaskRegion)c.bounds();

                long start = cb.start;
                long end = cb.end;
                float minFreq = cb.freqMin;
                float maxFreq = cb.freqMax;
                float freqDiff = (float) ((maxFreq - minFreq) / (1f + branchFreqRange));

                float dur = nar.dur();
                float awayFromNow = (float) (abs(cb.center(0) - now) / dur); //0..1.0

                float timeSpan = (float) ((end - start) / (1f + branchTimeRange));

                return (freqDiff) *  //minimize
                        (timeSpan) *  //minimize
                        (1 + 1f / (1f + awayFromNow))  //maximize
                        ;
            });

            compressNext(oldest, nar);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected Task find(@NotNull TaskRegion t) {
        final Task[] found = {null};
        tree.intersecting(t, (x) -> {
            if (x.equals(t)) {
                @Nullable Task xt = x.task;
                if (xt!=null) {
                    found[0] = xt;
                    return false; //finished
                }
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
        tree.forEach(r -> {
            Task rt = r.task;
            if (rt!=null)
                x.accept(rt);
        });
    }

    @Override
    public boolean removeTask(Task x) {
        x.delete();
        return tree.remove(new TaskRegion(x));
    }

    public boolean remove(TaskRegion x) {
        x.task.delete();
        return tree.remove(x);
    }

    public void removeAsync(TaskRegion x) {
        x.task.delete();
        tree.removeAsync(x);
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
