package nars.table;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import jcog.Util;
import jcog.tree.rtree.*;
import jcog.util.Top;
import jcog.util.Top2;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Activate;
import nars.task.*;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;

public class RTreeBeliefTable implements TemporalBeliefTable {

    static final int[] sampleRadii = { 0, 1, 2, 4, 8, 16, 32 };
    final static int maxSamplesTruthpolated = 5;

    /** proportional to capacity (not size) */
    final static float enoughSamplesRate = 0.1f;


    public static class TaskRegion implements HyperRegion, Tasked {

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
            return obj != null && (this == obj || (task != null && Objects.equals(task, ((TaskRegion) obj).task)));
        }

        @Override
        public int hashCode() {
            return task.hashCode();
        }

        @Override
        public double cost() {
            return (1f + 0.5f * Util.sigmoid(range(0))) *
                    Util.sqr(1f + (float) range(1)) *
                    (1f + 0.5f * range(2));
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

        @Override
        public final @Nullable Task task() {
            return task;
        }
    }

    final Space<TaskRegion> tree;
    final Set<TaskRegion> ongoing = Sets.newConcurrentHashSet();

    private long lastUpdate = Long.MIN_VALUE;
    private transient NAR nar = null;
    //private final AtomicBoolean compressing = new AtomicBoolean(false);

    public RTreeBeliefTable() {
        Spatialization<TaskRegion> model = new Spatialization<TaskRegion>((t -> t), Spatialization.DefaultSplits.AXIAL, 3, 4) {

            @Override
            public void merge(TaskRegion existing, TaskRegion incoming) {

                Task i = incoming.task;
                Task e = existing.task;
                if (e == i)
                    return; //same instance

                float activation = i.priElseZero();
                float before = e.priElseZero();
                float after = e.priAdd(activation);
                activation = (after - before);

                ((NALTask) e).merge(((NALTask) i));

                Activate.activate(e, activation, nar);
            }
        };

        this.tree = new ConcurrentRTree<TaskRegion>(
                new RTree<TaskRegion>(model) {

                    @Override
                    public boolean add(TaskRegion tr) {
                        Task task = tr.task;
                        if (task instanceof SignalTask) {
                            if (!ongoing.add(tr))
                                return false; //already in
                        }

                        if (super.add(tr)) {
                            //new insertion
                            Activate.activate(tr.task, tr.task.priElseZero(), nar);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean remove(TaskRegion tr) {
                        if (super.remove(tr)) {
                            Task task = tr.task;
                            if (task instanceof SignalTask)
                                ongoing.remove(tr);
                            return true;
                        } else
                            return false;
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

                    boolean readded = tree.add(r);
                    //assert(readded);

                    return false; //keep tracking
                } else {
                    return true; //stop tracking, this task was not in the tree
                }
            }

            return false; //keep tracking
        });


    }

    public RTreeBeliefTable(int cap) {
        this();
        setCapacity(cap);
    }

    private int capacity;

    @Override
    public Truth truth(long when, EternalTable eternal, NAR nar) {



        final Task ete = eternal != null ? eternal.strongest() : null;

        int ss = size();
        if (!tree.isEmpty()) {

            long now = nar.time();
            updateSignalTasks(now);

            int dur = nar.dur();


            FloatFunction<Task> ts = taskStrength(when, dur);
            FloatFunction<TaskRegion> strongestTask = (t -> +ts.floatValueOf(t.task));

            float confMin = nar.confMin.floatValue();
            int enoughSampled = Math.min(ss, Math.round(Math.max(1, enoughSamplesRate * capacity)));

            RTreeCursor<TaskRegion> c = null;
            for (int r : sampleRadii) {

                long from = when - r * dur;
                long to = when + r * dur;
                if (c == null)
                    c = cursor(from, to);
                else
                    c.in(timeRange(from, to)); //recycle

                if (c.size() >= enoughSampled)
                    break;
            }

            if (c != null && c.size() > 0) {


                List<TaskRegion> tt = c.topSorted(strongestTask, maxSamplesTruthpolated);
                int tts = tt.size();
                if (tts > 0) {
                    //applying eternal should not influence the scan for temporal so it is left null here
                    @Nullable PreciseTruth t = TruthPolation.truth(
                            ete, when, dur, tt);
                    if (t != null /*&& t.conf() >= confMin*/) {
                        return t.ditherFreqConf(nar.truthResolution.floatValue(), confMin, 1f);
                    }
                }
            }

        }


        return ete != null ? ete.truth() : null;

    }

    @Override
    public Task match(long when, @Nullable Task against, NAR nar) {

        if (size()==0)
            return null;

        long now = nar.time();

        updateSignalTasks(now);

        int dur = nar.dur();


        FloatFunction<Task> ts = taskStrength(when, dur);
        FloatFunction<TaskRegion> strongestTask = t -> +ts.floatValueOf(t.task);

        for (int r : sampleRadii) {
            RTreeCursor<TaskRegion> ct = cursor(when - r * dur, when + r * dur);
            if (ct.size()==0)
                continue;

            List<TaskRegion> tt = ct.topSorted(strongestTask, 2);

            if (!tt.isEmpty()) {

                switch (tt.size()) {
                    case 0:
                        return null;
                    case 1:
                        return tt.get(0).task;

                    default:
                        Task a = tt.get(0).task;
                        Task b = tt.get(1).task;

                        Task c = Revision.merge(a, b, now, nar.confMin.floatValue(), nar.random());
                        return c != null ? c : a;
                }

            }
        }

        return null;


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

    private static HyperRegion timeRange(long a, long b) {
        return new TaskRegion(a, b);
    }


    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compress on shrink
    }

    @Override
    public void add(@NotNull Task x, TaskConcept c, NAR n) {

        updateSignalTasks(n.time());

        this.nar = n;

        TaskRegion tr = new TaskRegion(x);
        if (tree.add(tr)) {

            //check capacity overage
            int over = size() + 1 - capacity;
            if (over > 0) {
                List<Task> toActivate = addAfterCompressing(tr, n);

                for (int i = 0, toActivateSize = toActivate.size(); i < toActivateSize; i++) {
                    Task ii = toActivate.get(i);

                    Activate aa = Activate.activate(ii, ii.priElseZero(), c, n);
                    if (aa!=null)
                        n.input(aa);
                }
            }
        }
    }

    private void add(@NotNull Task t) {
        tree.addAsync(new TaskRegion(t));
    }

    /**
     * assumes called with writeLock
     */
    @NotNull
    private List<Task> addAfterCompressing(TaskRegion tr, NAR nar) {
        //if (compressing.compareAndSet(false, true)) {
        try {


            long now = nar.time();
            int dur = nar.dur();

            Task input = tr.task;
            float inputConf = input.conf(now, dur);

            FloatFunction<Task> ts = taskStrength(now, dur);
            FloatFunction<TaskRegion> weakestTask = (t -> -ts.floatValueOf(t.task));
            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
            FloatFunction<Leaf<TaskRegion>> weakestRegion = (l) -> l.size * -rs.floatValueOf((TaskRegion) l.region);

            ConcurrentRTree<TaskRegion> lt = ((ConcurrentRTree) tree);

            List<Task> toActivate = $.newArrayList(1);
            lt.withWriteLock((r) -> {


                Top<TaskRegion> deleteVictim = new Top<>(weakestTask);
                Top<Leaf<TaskRegion>> mergeVictim = new Top<>(weakestRegion);

                compressNext(tree.root(), deleteVictim, mergeVictim, inputConf, nar);

                //decide to remove or merge:
                @Nullable TaskRegion toRemove = deleteVictim.the;
                @Nullable Leaf<TaskRegion> toMerge = mergeVictim.the;
                if (toRemove == null && toMerge == null)
                    return; //failed to compress

                //float confMin = toRemove != null ? toRemove.task.conf() : nar.confMin.floatValue();
                Task activation = null;

//                    if (toRemove != null && toMerge!=null) {
//                        Truth toRemoveNow = toRemove.task.truth(now, dur);
//                        TaskRegion toMergeRegion = (TaskRegion) (toMerge.region());
//                        float toMergeNow = (float) toMergeRegion.center(2); /* confMean*/ //not a fair comparison, so i use confMax/n
//                        if (toRemoveNow == null) {
//                            toRemove = null;
//                        } else {
//                            if (toRemoveNow.conf() >= toMergeNow)
//                                toMerge = null;
//                            else
//                                toRemove = null;
//                        }
//
//                    }

                if (toMerge != null && (activation = compressMerge(toMerge, now, dur, nar.confMin.floatValue(), nar.random())) != null) {
                    toActivate.add(activation);
                } else if (toRemove != null) {
                    compressEvict(toRemove);
                }

                if (r.size() < capacity)
                    tree.add(tr);
                else
                    toActivate.clear();
            });

            return toActivate;
            //nar.input(toActivate);

        } finally {
            //compressing.set(false);
        }
        //}

    }

    private Task compressMerge(Leaf<TaskRegion> l, long now, int dur, float confMin, Random rng) {
        short s = l.size;
        assert (s > 0);

        TaskRegion a, b;
        if (s > 2) {
            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
            Top2<TaskRegion> t2 = new Top2<>((r) -> -rs.floatValueOf(r));
            l.forEach(t2);
            a = t2.a;
            b = t2.b;
        } else {
            a = l.get(0);
            b = l.get(1);
        }

        if (a != null && b != null) {
            Task c = Revision.merge(a.task, b.task, now, confMin, rng);
            if (c != null) {
                //already has write lock so just use non-async methods
                remove(a);
                remove(b);
                add(c);

                //run but don't broadcast it
                return c;

                //TaskTable.activate(c, c.pri(), nar);
            }
        }

        return null;

    }


    private void compressEvict(TaskRegion t) {
        remove(t);
    }

    private void compressNext(Node<TaskRegion, ?> next, Consumer<TaskRegion> deleteVictim, Consumer<Leaf<TaskRegion>> mergeVictim, float inputConf, NAR nar) {
        if (next instanceof Leaf) {

            compressLeaf((Leaf) next, deleteVictim, inputConf, nar.time(), nar.dur());

            if (next.size() > 1)
                mergeVictim.accept((Leaf) next);

        } else if (next instanceof Branch) {
            compressBranch((Branch) next, deleteVictim, mergeVictim, inputConf, nar);
        } else {
            throw new RuntimeException();
        }
    }

    private void compressBranch(Branch<TaskRegion> b, Consumer<TaskRegion> deleteVictims, Consumer<Leaf<TaskRegion>> mergeVictims, float inputConf, NAR nar) {
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
            //select the 2 weakest regions and recurse
            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
            Top2<Node<TaskRegion, ?>> weakest = new Top2<>(
                    (n) -> -rs.floatValueOf((TaskRegion) n.region())
            );

            for (int i = 0; i < w; i++) {
                Node bb = b.get(i);
                if (bb != null) {
                    if (bb instanceof Leaf && bb.size() > 1)
                        mergeVictims.accept((Leaf) bb);
                    weakest.accept(bb);
                }
            }

            if (weakest.a != null)
                compressNext(weakest.a, deleteVictims, mergeVictims, inputConf, nar);
            if (weakest.b != null)
                compressNext(weakest.b, deleteVictims, mergeVictims, inputConf, nar);
        }
    }

    private void compressLeaf(Leaf<TaskRegion> l, Consumer<TaskRegion> deleteVictims, float inputConf, long now, int dur) {

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
                if (t.task.conf(now, dur) <= inputConf)
                    deleteVictims.accept(t);
            }
        }
    }

    private static FloatFunction<TaskRegion> regionStrength(long when, int dur) {

        return (TaskRegion cb) -> {

            float awayFromNow = (float) (Math.max(Math.abs(cb.start - when), Math.abs(cb.end - when))) / dur; //0..1.0

            long timeSpan = cb.end - cb.start;
            float timeSpanFactor = awayFromNow == 0 ? 0f : (timeSpan / (timeSpan + awayFromNow));

            return (
                    1f / (1f + awayFromNow)) //min
                    * //AND
                    (1f + 0.5f * Util.clamp(
                            (-1.0f * (cb.freqMax - cb.freqMin)) +  //max
                                    (-0.5f * (cb.confMax - cb.confMin)) +  //max
                                    (-1.0f * cb.confMax) + //max
                                    (-1.0f * timeSpanFactor),  //max: prefer smaller time spans
                            -1f, +1f))
                    ;
        };
    }

    private static FloatFunction<Task> taskStrength(long when, int dur) {
        return (Task x) -> temporalTaskPriority(x, when, dur);
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
        return Iterators.transform(tree.iterator(), x -> x.task);
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
        return tree.remove(new TaskRegion(x));
    }

    public void remove(TaskRegion x) {
        tree.removeAsync(x);
    }

    public void removeAsync(TaskRegion x) {
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


//package nars.table;
//
//import com.google.common.collect.Iterators;
//import jcog.Util;
//import jcog.pri.Pri;
//import jcog.tree.rtree.*;
//import jcog.util.Top;
//import jcog.util.Top2;
//import nars.$;
//import nars.NAR;
//import nars.Task;
//import nars.concept.TaskConcept;
//import nars.control.ConceptFire;
//import nars.task.Revision;
//import nars.task.SignalTask;
//import nars.task.Tasked;
//import nars.task.TruthPolation;
//import nars.truth.PreciseTruth;
//import nars.truth.Truth;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.eclipse.collections.api.list.MutableList;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.PrintStream;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//import static nars.table.TemporalBeliefTable.temporalTaskPriority;
//
//public class RTreeBeliefTable implements TemporalBeliefTable {
//
//    static final int[] sampleRadii = { /*0,*/ 1, 2, 4, 8, 32};
//
//
//    public static class TaskRegion implements HyperRegion, Tasked {
//
//        public final long start;
//        long end; //allow end to stretch for ongoing tasks
//
//        public final float freqMin, freqMax, confMin, confMax;
//
//        @Nullable
//        public Task task;
//
//        public TaskRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
//            this(start, end, freqMin, freqMax, confMin, confMax, null);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            return this == obj || (task != null && Objects.equals(task, ((TaskRegion) obj).task));
//        }
//
//        @Override
//        public int hashCode() {
//            return task.hashCode();
//        }
//
//        @Override
//        public double cost() {
//            return (1f + 0.5f * Util.sigmoid(range(0))) *
//                    Util.sqr(1f + (float) range(1)) *
//                    (1f + 0.5f * range(2));
//        }
//
//        @Override
//        public String toString() {
//            return task != null ? task.toString() : Arrays.toString(new double[]{start, end, freqMin, freqMax, confMin, confMax});
//        }
//
//        public TaskRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax, Supplier<Task> task) {
//            this.start = start;
//            this.end = end;
//            this.freqMin = freqMin;
//            this.freqMax = freqMax;
//            this.confMin = confMin;
//            this.confMax = confMax;
//            this.task = null;
//        }
//
//        public TaskRegion(@NotNull Task task) {
//            this.task = task;
//            this.start = task.start();
//            this.end = task.end();
//            this.freqMin = this.freqMax = task.freq();
//            this.confMin = this.confMax = task.conf();
//        }
//
//        /**
//         * all inclusive time region
//         */
//        public TaskRegion(long a, long b) {
//            this(a, b, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
//        }
//
//
//        /**
//         * computes a mbr of the given regions
//         */
//        public static TaskRegion mbr(TaskRegion x, TaskRegion y) {
//            if (x == y) return x;
//            return new TaskRegion(
//                    Math.min(x.start, y.start), Math.max(x.end, y.end),
//                    Math.min(x.freqMin, y.freqMin), Math.max(x.freqMax, y.freqMax),
//                    Math.min(x.confMin, y.confMin), Math.max(x.confMax, y.confMax)
//            );
//        }
//
//        @Override
//        public int dim() {
//            return 3;
//        }
//
//        @Override
//        public TaskRegion mbr(HyperRegion r) {
//            return TaskRegion.mbr(this, (TaskRegion) r);
//        }
//
//        @Override
//        public double coord(boolean maxOrMin, int dimension) {
//            switch (dimension) {
//                case 0:
//                    return maxOrMin ? end : start;
//                case 1:
//                    return maxOrMin ? freqMax : freqMin;
//                case 2:
//                    return maxOrMin ? confMax : confMin;
//            }
//            throw new UnsupportedOperationException();
//        }
//
//        public void updateOngoingTask() {
//
//            this.end = task.end();
//
//        }
//
//        public boolean hasStretched() {
//            return this.end != task.end();
//        }
//
//        public boolean isDeleted() {
//            return task != null && task.isDeleted();
//        }
//
//        @Override
//        public final @Nullable Task task() {
//            return task;
//        }
//    }
//
//    final Space<TaskRegion> tree;
//
//    final AtomicReference<TaskRegion> ongoing = new AtomicReference(null);
//    final AtomicLong lastUpdate = new AtomicLong(Long.MIN_VALUE);
//
//
//    private transient NAR nar = null;
//    //private final AtomicBoolean compressing = new AtomicBoolean(false);
//
//    public RTreeBeliefTable() {
//        Spatialization<TaskRegion> model = new Spatialization<TaskRegion>((t -> t), Spatialization.DefaultSplits.AXIAL, 3, 4) {
//
//            @Override
//            public void merge(TaskRegion found, TaskRegion incoming) {
//
//                if (found.task == incoming.task)
//                    return; //same instance
//
//                float before = found.task.priElseZero();
//                float after = found.task.priAdd(incoming.task.priElseZero());
//                float activation = (after - before);
//                incoming.task.setPri(activation); //temporary
//                incoming.task = found.task; //set the incoming task region so that the callee can handle the merge and its activation outside of this critical section
//            }
//        };
//
//        this.tree = new ConcurrentRTree<TaskRegion>(
//                new RTree<TaskRegion>(model) {
//
////                    @Override
////                    public boolean add(TaskRegion tr) {
////                        Task task = tr.task;
////                        if (task instanceof SignalTask) {
////                            TaskRegion pr = ongoing.getAndSet(tr);
////                            if (pr!=null && pr.task == tr.task)
////                                return false;
////                            //if (!ongoing.set(tr))
////                                //return false; //already in
////                        }
////
////                        return super.add(tr);
////                    }
////
////                    @Override
////                    public boolean remove(TaskRegion tr) {
////                        if (super.remove(tr)) {
////                            Task task = tr.task;
////                            if (task instanceof SignalTask &&
////                                    ongoing.get()!=null && ongoing.get().task==task)
////                                ongoing.set(null);
////                            return true;
////                        } else
////                            return false;
////                    }
//                });
//    }
//
//    public void updateSignalTasks(long now) {
//
//
//        if (this.lastUpdate.getAndSet(now) == now) //same cycle
//            return;
//
//        TaskRegion r = ongoing.get();
//        if (r != null) {
//
//
//            if (r.task.isDeleted()) {
//                ongoing.set(null); //stop tracking
//            } else {
//                if (r.hasStretched()) {
//
//                    boolean removed = tree.remove(r);
//                    if (!removed)
//                        ongoing.set(null); //stop tracking, this task was not in the tree
//                    else {
//                        r.updateOngoingTask();
//                        tree.add(r); //reinsert
//                    }
//                }
//            }
//
//        }
//    }
//
//
//    public RTreeBeliefTable(int cap) {
//        this();
//        setCapacity(cap);
//    }
//
//    private int capacity;
//
//    @Override
//    public Truth truth(long when, EternalTable eternal, NAR nar) {
//
//        long now = nar.time();
//        updateSignalTasks(now);
//
//        @Nullable Task e = eternal != null ? eternal.strongest() : null;
//
//        if (!tree.isEmpty()) {
//
//            int dur = nar.dur();
//
////            FloatFunction<TaskRegion> wr = regionStrength(now, dur);
////            FloatFunction<TaskRegion> sort = t -> -wr.floatValueOf(t);
//
//            float confMin = nar.confMin.floatValue();
//            for (int r : sampleRadii) {
//
//                List<TaskRegion> tt = cursor(when - r * dur, when + r * dur)
//                        //.listSorted(sort)
//                        .list();
//                if (!tt.isEmpty()) {
//                    //applying eternal should not influence the scan for temporal so it is left null here
//                    @Nullable PreciseTruth t = TruthPolation.truth(null, when, dur, tt);
//                    if (t != null && t.conf() > confMin) {
//                        return t.ditherFreqConf(nar.truthResolution.floatValue(), confMin, 1f);
//                    }
//                }
//            }
//
//        }
//
//
//        return e != null ? e.truth() : null;
//
//    }
//
//    @Override
//    public Task match(long when, @Nullable Task against, NAR nar) {
//
//        long now = nar.time();
//
//        updateSignalTasks(now);
//
//        int dur = nar.dur();
//
//        FloatFunction<TaskRegion> wr = regionStrength(when, dur);
//        FloatFunction<TaskRegion> sort = t -> -wr.floatValueOf(t);
//
//        for (int r : sampleRadii) {
//            MutableList<TaskRegion> tt = cursor(when - r * dur, when + r * dur).
//                    listSorted(sort);
//            if (!tt.isEmpty()) {
//
//                switch (tt.size()) {
//                    case 0:
//                        return null;
//                    case 1:
//                        return tt.get(0).task;
//
//                    default:
//                        Task a = tt.get(0).task;
//                        Task b = tt.get(1).task;
//
//                        Task c = Revision.merge(a, b, now, nar.confMin.floatValue(), nar.random());
//                        return c != null ? c : a;
//                }
//
//            }
//        }
//
//        return null;
//
//
//    }
//
////    public static <X> FloatFunction<X> neg(FloatFunction<X> f) {
////        return (x) -> -f.floatValueOf(x);
////    }
//
////    protected MutableList<TaskRegion> timeDistanceSortedList(long start, long end, long now) {
////        RTreeCursor<TaskRegion> c = cursor(start, end);
////        return c.listSorted((t) -> t.task.timeDistance(now));
////    }
//
//    private RTreeCursor<TaskRegion> cursor(long start, long end) {
//        return tree.cursor(timeRange(start, end));
//    }
//
//    private static HyperRegion timeRange(long a, long b) {
//        return new TaskRegion(a, b);
//    }
//
//
//    @Override
//    public void setCapacity(int capacity) {
//        this.capacity = capacity;
//        //TODO compress on shrink
//    }
//
//    @Override
//    public void add(@NotNull Task x, TaskConcept c, NAR n) {
//
//        this.nar = n;
//
//        updateSignalTasks(n.time());
//
//        float inputActivation = x.priElseZero();
//
//        TaskRegion tr = new TaskRegion(x);
//
//        if (tree.add(tr)) {
//
//
//            if (x instanceof SignalTask && tr.hasStretched()) {
//                ongoing.set(tr);
//            }
//
//            //NEW
//            TaskTable.activate(x, inputActivation, nar);
//
//            //check capacity overage
//            int over = size() + 1 - capacity;
//            if (over > 0) {
//                List<Task> toActivate = addAfterCompressing(tr, n);
//
//                for (int i = 0, toActivateSize = toActivate.size(); i < toActivateSize; i++) {
//                    Task ii = toActivate.get(i);
//
//                    n.input(ConceptFire.activate(ii, ii.priElseZero(), c, n));
//                }
//            }
//
//        } else {
//            Task found = tr.task; //it will have been set
//            if (found == x) {
//                //IDENTICAL; no effect
//                return;
//            } else {
//                //MERGE //assert(found.equals(x));
//                float actualActivation = x.priSafe(0); //will have been set; get this set pri which is actually the effective activation; then delete the task
//                x.delete();
//                if (actualActivation >= Pri.EPSILON) {
//                    TaskTable.activate(found, actualActivation, nar);
//                }
//            }
//        }
//
//
//    }
//
//    private void add(@NotNull Task t) {
//        tree.addAsync(new TaskRegion(t));
//    }
//
//    /**
//     * assumes called with writeLock
//     */
//    @NotNull
//    private List<Task> addAfterCompressing(TaskRegion tr, NAR nar) {
//        //if (compressing.compareAndSet(false, true)) {
//        try {
//
//
//            long now = nar.time();
//            int dur = nar.dur();
//
//            Task input = tr.task;
//            float inputConf = input.conf(now, dur);
//
//            FloatFunction<Task> ts = taskStrength(now, dur);
//            FloatFunction<TaskRegion> weakestTask = (t -> -ts.floatValueOf(t.task));
//            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
//            FloatFunction<Leaf<TaskRegion>> weakestRegion = (l) -> l.size * -rs.floatValueOf((TaskRegion) l.bounds);
//
//            ConcurrentRTree<TaskRegion> lt = ((ConcurrentRTree) tree);
//
//            List<Task> toActivate = $.newArrayList(1);
//            lt.withWriteLock((r) -> {
//
//
//                Top<TaskRegion> deleteVictim = new Top<>(weakestTask);
//                Top<Leaf<TaskRegion>> mergeVictim = new Top<>(weakestRegion);
//
//                compressNext(tree.root(), deleteVictim, mergeVictim, inputConf, nar);
//
//                //decide to remove or merge:
//                @Nullable TaskRegion toRemove = deleteVictim.the;
//                @Nullable Leaf<TaskRegion> toMerge = mergeVictim.the;
//                if (toRemove == null && toMerge == null)
//                    return; //failed to compress
//
//                //float confMin = toRemove != null ? toRemove.task.conf() : nar.confMin.floatValue();
//                Task activation = null;
//
////                    if (toRemove != null && toMerge!=null) {
////                        Truth toRemoveNow = toRemove.task.truth(now, dur);
////                        TaskRegion toMergeRegion = (TaskRegion) (toMerge.region());
////                        float toMergeNow = (float) toMergeRegion.center(2); /* confMean*/ //not a fair comparison, so i use confMax/n
////                        if (toRemoveNow == null) {
////                            toRemove = null;
////                        } else {
////                            if (toRemoveNow.conf() >= toMergeNow)
////                                toMerge = null;
////                            else
////                                toRemove = null;
////                        }
////
////                    }
//
//                if (toMerge != null && (activation = compressMerge(toMerge, now, dur, nar.confMin.floatValue(), nar.random())) != null) {
//                    toActivate.add(activation);
//                } else if (toRemove != null) {
//                    compressEvict(toRemove);
//                }
//
//                if (r.size() < capacity)
//                    tree.add(tr);
//                else
//                    toActivate.clear();
//            });
//
//            return toActivate;
//            //nar.input(toActivate);
//
//        } finally {
//            //compressing.set(false);
//        }
//        //}
//
//    }
//
//    private Task compressMerge(Leaf<TaskRegion> l, long now, int dur, float confMin, Random rng) {
//        short s = l.size;
//        assert (s > 0);
//
//        TaskRegion a, b;
//        if (s > 2) {
//            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
//            Top2<TaskRegion> t2 = new Top2<>((r) -> -rs.floatValueOf(r));
//            l.forEach(t2);
//            a = t2.a;
//            b = t2.b;
//        } else {
//            a = l.get(0);
//            b = l.get(1);
//        }
//
//        if (a != null && b != null) {
//            Task c = Revision.merge(a.task, b.task, now, confMin, rng);
//            if (c != null) {
//                //already has write lock so just use non-async methods
//                removeAsync(a);
//                removeAsync(b);
//                add(c);
//
//                //run but don't broadcast it
//                return c;
//
//                //TaskTable.activate(c, c.pri(), nar);
//            }
//        }
//
//        return null;
//
//    }
//
//
//    private void compressEvict(TaskRegion t) {
//        removeAsync(t);
//    }
//
//    private void compressNext(Node<TaskRegion, ?> next, Consumer<TaskRegion> deleteVictim, Consumer<Leaf<TaskRegion>> mergeVictim, float inputConf, NAR nar) {
//        if (next instanceof Leaf) {
//
//            compressLeaf((Leaf) next, deleteVictim, inputConf, nar.time(), nar.dur());
//
//            if (next.size() > 1)
//                mergeVictim.accept((Leaf) next);
//
//        } else if (next instanceof Branch) {
//            compressBranch((Branch) next, deleteVictim, mergeVictim, inputConf, nar);
//        } else {
//            throw new RuntimeException();
//        }
//    }
//
//    private void compressBranch(Branch<TaskRegion> b, Consumer<TaskRegion> deleteVictims, Consumer<Leaf<TaskRegion>> mergeVictims, float inputConf, NAR nar) {
//        //options:
//        //a. smallest
//        //b. oldest
//        //c. other metrics
//
//        long now = nar.time();
//
////        HyperRegion branchBounds = b.bounds();
////        double branchTimeRange = branchBounds.range(0);
////        double branchFreqRange = branchBounds.range(1);
//
//        int dur = nar.dur();
//
//
//        //List<Node<TaskRegion, ?>> weakest = b.childMinList( weaknessTask(now, dur ), sizeBefore / 2);
//        int w = b.size();
//        if (w > 0) {
//
//            //recurse through a subest of the weakest regions, while also collecting victims
//            //select the 2 weakest regions and recurse
//            FloatFunction<TaskRegion> rs = regionStrength(now, dur);
//            Top2<Node<TaskRegion, ?>> weakest = new Top2<>(
//                    (n) -> -rs.floatValueOf((TaskRegion) n.region())
//            );
//
//            for (int i = 0; i < w; i++) {
//                Node bb = b.get(i);
//                if (bb != null) {
//                    if (bb instanceof Leaf && bb.size() > 1)
//                        mergeVictims.accept((Leaf) bb);
//                    weakest.accept(bb);
//                }
//            }
//
//            if (weakest.a != null)
//                compressNext(weakest.a, deleteVictims, mergeVictims, inputConf, nar);
//            if (weakest.b != null)
//                compressNext(weakest.b, deleteVictims, mergeVictims, inputConf, nar);
//        }
//    }
//
//    private void compressLeaf(Leaf<TaskRegion> l, Consumer<TaskRegion> deleteVictims, float inputConf, long now, int dur) {
//
//        int size = l.size;
//        Object[] ld = l.data;
//
//        // remove any deleted tasks while scanning for victims
//        for (int i = 0; i < size; i++) {
//            Object x = ld[i];
//            if (x == null)
//                continue;
//            TaskRegion t = (TaskRegion) x;
//            if (t.isDeleted()) {
//                removeAsync(t); //already has write lock so just use non-async methods
//            } else {
//                if (t.task.conf(now, dur) <= inputConf)
//                    deleteVictims.accept(t);
//            }
//        }
//    }
//
//    private static FloatFunction<TaskRegion> regionStrength(long when, int dur) {
//
//        return (TaskRegion cb) -> {
//
//            float awayFromNow = (float) (Math.max(Math.abs(cb.start - when), Math.abs(cb.end - when))) / dur; //0..1.0
//
//            long timeSpan = cb.end - cb.start;
//            float timeSpanFactor = awayFromNow == 0 ? 0f : (timeSpan / (timeSpan + awayFromNow));
//
//            return (
//                    1f / (1f + awayFromNow)) //min
//                    * //AND
//                    (1f + 0.5f * Util.clamp(
//                            (-1.0f * (cb.freqMax - cb.freqMin)) +  //max
//                                    (-0.5f * (cb.confMax - cb.confMin)) +  //max
//                                    (-1.0f * cb.confMax) + //max
//                                    (-1.0f * timeSpanFactor),  //max: prefer smaller time spans
//                            -1f, +1f))
//                    ;
//        };
//    }
//
//    private static FloatFunction<Task> taskStrength(long when, int dur) {
//        return (Task x) -> temporalTaskPriority(x, when, dur);
//    }
//
//
//    protected Task find(@NotNull TaskRegion t) {
//        final Task[] found = {null};
//        tree.intersecting(t, (x) -> {
//            if (x.equals(t)) {
//                @Nullable Task xt = x.task;
//                if (xt != null) {
//                    found[0] = xt;
//                    return false; //finished
//                }
//            }
//            return true;
//        });
//        return found[0];
//    }
//
//
//    @Override
//    public int capacity() {
//        return capacity;
//    }
//
//    @Override
//    public int size() {
//        return tree.size();
//    }
//
//    @Override
//    public Iterator<Task> taskIterator() {
//        return Iterators.transform(tree.iterator(), x -> x.task);
//    }
//
//    @Override
//    public void forEachTask(Consumer<? super Task> x) {
//        tree.forEach(r -> {
//            Task rt = r.task;
//            if (rt != null)
//                x.accept(rt);
//        });
//    }
//
//    @Override
//    public boolean removeTask(Task x) {
//        return remove(new TaskRegion(x));
//    }
//
//    public boolean remove(TaskRegion x) {
//        x.task.delete();
//        return tree.remove(x);
//    }
//
//    public void removeAsync(TaskRegion x) {
//        x.task.delete();
//        tree.removeAsync(x);
//    }
//
//    @Override
//    public void clear() {
//        tree.clear();
//    }
//
//    public void print(PrintStream out) {
//        forEachTask(out::println);
//        tree.stats().print(out);
//    }
//}
