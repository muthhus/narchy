package nars.table;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import jcog.Util;
import jcog.tree.rtree.*;
import jcog.util.Top;
import jcog.util.Top2;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.Activate;
import nars.concept.TaskConcept;
import nars.task.Revision;
import nars.task.SignalTask;
import nars.task.TruthPolation;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;

public class RTreeBeliefTable implements TemporalBeliefTable {

    static final int sampleRadius = 32;


    public static class TaskRegion implements HyperRegion {

        public final long start;
        long end; //allow end to stretch for ongoing tasks

        public final float freqMin, freqMax, confMin, confMax;

        @Nullable
        public final Task task;

        public TaskRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
            this(start, end, freqMin, freqMax, confMin, confMax, null);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (task != null ? Objects.equals(task, ((TaskRegion) obj).task) : false);
        }

        @Override
        public int hashCode() {
            return task.hashCode();
        }

        @Override
        public String toString() {
            return task != null ? task.toString() : Arrays.toString(new double[]{start, end, freqMin, freqMax, confMin, confMax});
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

        /**
         * all inclusive time region
         */
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
            return task != null && task.isDeleted();
        }
    }

    final Space<TaskRegion> tree;
    final Set<TaskRegion> ongoing = Sets.newConcurrentHashSet();

    private long lastUpdate = Long.MIN_VALUE;
    //private final AtomicBoolean compressing = new AtomicBoolean(false);

    public RTreeBeliefTable() {
        this.tree = new ConcurrentRTree<TaskRegion>(
                new RTree<TaskRegion>((t -> t), 2, 4, Spatialization.DefaultSplits.AXIAL) {

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

        List<TaskRegion> tt = cursor(when - sampleRadius * dur, when + sampleRadius * dur).list();
        @Nullable Task e = eternal != null ? eternal.strongest() : null;
        if (!tt.isEmpty())
            return TruthPolation.truth(e, when, dur, Iterables.transform(tt, t -> t.task));
        else
            return e != null ? e.truth() : null;
    }

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {

        updateSignalTasks(now);


        FloatFunction<TaskRegion> wr = weaknessRegion(now, dur);
        MutableList<TaskRegion> tt = cursor(when - sampleRadius * dur, when + sampleRadius * dur).
                listSorted(t -> -wr.floatValueOf(t));

        switch (tt.size()) {
            case 0:
                return null;
            case 1:
                return tt.get(0).task;

            default:
                Task a = tt.get(0).task;
                Task b = tt.get(1).task;

                Task c = Revision.merge(this, a, b, now, dur, Param.TRUTH_EPSILON, rng);
                return c != null ? c : a;
        }


    }

//    public static <X> FloatFunction<X> neg(FloatFunction<X> f) {
//        return (x) -> -f.floatValueOf(x);
//    }

//    protected MutableList<TaskRegion> timeDistanceSortedList(long start, long end, long now) {
//        RTreeCursor<TaskRegion> c = cursor(start, end);
//        return c.listSorted((t) -> t.task.timeDistance(now));
//    }

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
                addAfterCompressing(tr, n, tree.model());
            } else {
                tree.addAsync(tr);
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

    /**
     * assumes called with writeLock
     */
    private void addAfterCompressing(TaskRegion tr, NAR nar, Spatialization<TaskRegion> model) {
        //if (compressing.compareAndSet(false, true)) {
        try {

            long now = nar.time();
            int dur = nar.dur();
            FloatFunction<Task> wt = weaknessTask(now, dur);
            FloatFunction<TaskRegion> taskRanker = (t -> wt.floatValueOf(t.task));
            FloatFunction<TaskRegion> regionRanker = weaknessRegion(now, dur);
            FloatFunction<Leaf<TaskRegion>> mergeRanker = (l) -> regionRanker.floatValueOf((TaskRegion) l.bounds);

            ConcurrentRTree<TaskRegion> lt = ((ConcurrentRTree) tree);

            Set<Activate> activations = new UnifiedSet(1);
            lt.withWriteLock((r) -> {
                while (size() > capacity) {

                    Top<TaskRegion> removeVictim = new Top<>(taskRanker);
                    Top<Leaf<TaskRegion>> mergeVictim = new Top<>(mergeRanker);

                    compressNext(tree.root(), removeVictim, mergeVictim, nar);

                    //decide to remove or merge:
                    @Nullable TaskRegion toRemove = removeVictim.the;

                    @Nullable final Leaf<TaskRegion> toMerge = mergeVictim.the;
                    float confMin = toRemove!=null ? toRemove.task.conf() : nar.confMin.floatValue();
                    Activate activation = null;
                    if (toMerge != null && (activation = compressMerge(toMerge, now, dur, confMin, nar.random())) != null) {
                        activations.add(activation);
                    } else if (toRemove != null) {
                        compressEvict(toRemove);
                    }
                }

                tree.add(tr);

            });

            nar.input(activations);


        } finally {
            //compressing.set(false);
        }
        //}
    }

    private Activate compressMerge(Leaf<TaskRegion> l, long now, int dur, float confMin, Random rng) {
        short s = l.size;
        assert (s > 0);

        TaskRegion a, b;
        if (s > 2) {
            Top2<TaskRegion> t2 = new Top2<>(weaknessRegion(now, dur));
            l.forEach(t2);
            a = t2.a;
            b = t2.b;
        } else {
            a = l.get(0);
            b = l.get(1);
        }

        if (a != null && b != null) {
            Task c = Revision.merge(this, a.task, b.task, now, dur, confMin, rng);
            if (c != null) {
                //already has write lock so just use non-async methods
                remove(a);
                remove(b);
                add(c);

                //run but don't broadcast it
                return new Activate(c, c.pri());  //TODO defer until out of this critical section?


                //TaskTable.activate(c, c.pri(), nar);
            }
        }

        return null;

    }


    private void compressEvict(TaskRegion t) {
        remove(t);
    }

    private void compressNext(Node<TaskRegion, ?> next, Consumer<TaskRegion> deleteVictim, Consumer<Leaf<TaskRegion>> mergeVictim, NAR nar) {
        if (next instanceof Leaf) {

            compressLeaf((Leaf) next, deleteVictim);

            if (next.size() > 1)
                mergeVictim.accept((Leaf) next);

        } else if (next instanceof Branch) {
            compressBranch((Branch) next, deleteVictim, mergeVictim, nar);
        } else {
            throw new RuntimeException();
        }
    }

    private void compressBranch(Branch<TaskRegion> b, Consumer<TaskRegion> deleteVictims, Consumer<Leaf<TaskRegion>> mergeVictims, NAR nar) {
        //options:
        //a. smallest
        //b. oldest
        //c. other metrics

        long now = nar.time();

//        HyperRegion branchBounds = b.bounds();
//        double branchTimeRange = branchBounds.range(0);
//        double branchFreqRange = branchBounds.range(1);

        int dur = nar.dur();


        //List<Node<TaskRegion, ?>> weakest = b.childMinList( weaknessTask(now, dur ), sizeBefore / 2);
        int w = b.size();
        if (w > 0) {

            //recurse through a subest of the weakest regions, while also collecting victims
            FloatFunction<TaskRegion> wr = weaknessRegion(now, dur);
            Top<Node<TaskRegion, ?>> weakest = new Top<>(
                    (n) -> wr.floatValueOf((TaskRegion) n.region())
            );

            for (int i = 0; i < w; i++) {
                Node bb = b.get(i);
                if (bb != null) {
                    if (bb instanceof Leaf && bb.size() > 1)
                        mergeVictims.accept((Leaf) bb);
                    weakest.accept(bb);
                }
            }

            Node<TaskRegion, ?> the = weakest.the;
            if (the != null)
                compressNext(the, deleteVictims, mergeVictims, nar);
        }
    }

    private void compressLeaf(Leaf<TaskRegion> l, Consumer<TaskRegion> deleteVictims) {

        int size = l.size;
        Object[] ld = l.data;

        // remove any deleted tasks while scanning for victims
        for (int i = 0; i < size; i++) {
            Object x = ld[i];
            if (x == null)
                continue;
            TaskRegion t = (TaskRegion) x;
            if (t.isDeleted()) {
                remove(t); //already has write lock so just use non-async methods
            } else {
                deleteVictims.accept(t);
            }
        }
    }

    private FloatFunction<TaskRegion> weaknessRegion(long when, int dur) {

        return (TaskRegion cb) -> {

            float awayFromNow = (float) (Math.max(cb.start - when, cb.end - when)) / dur; //0..1.0

            long timeSpan = cb.end - cb.start;
            float timeSpanFactor = awayFromNow == 0 ? 1f : (timeSpan / (timeSpan + awayFromNow));

            return (
                    (1 + 1.0f * (cb.freqMax - cb.freqMin)) *  //minimize
                            (1 + 1.0f * (cb.confMax - cb.confMin)) *  //minimize
                            (1 + 0.1f * (timeSpanFactor)) *  //minimize: prefer smaller time spans
                            (1 + 0.5f * 1f / Util.sqr(1f + (awayFromNow)))) *  //maximize
                    (1 + 0.5f * cb.confMax) //minimize
                    ;
        };
    }

    private static FloatFunction<Task> weaknessTask(long when, float dur) {
        return (Task x) -> -temporalTaskPriority(x, when, dur);
    }


    protected Task find(@NotNull TaskRegion t) {
        final Task[] found = {null};
        tree.intersecting(t, (x) -> {
            if (x.equals(t)) {
                @Nullable Task xt = x.task;
                if (xt != null) {
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
            if (rt != null)
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
