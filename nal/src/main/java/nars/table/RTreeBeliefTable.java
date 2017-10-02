package nars.table;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import jcog.pri.Prioritized;
import jcog.sort.TopN;
import jcog.tree.rtree.*;
import jcog.util.Top;
import jcog.util.Top2;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.control.Activate;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.SignalTask;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.task.util.TaskRegionLink;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.signal.Signal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;
import static nars.time.Tense.ETERNAL;

public class RTreeBeliefTable implements TemporalBeliefTable {

    public static final int MIN_TASKS_PER_LEAF = 2;
    public static final int MAX_TASKS_PER_LEAF = 3;
    public static final Spatialization.DefaultSplits SPLIT =
            Spatialization.DefaultSplits.AXIAL;
    //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here

    private int capacity;

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    static final float SCAN_MAX_FRACTION = 0.1f;

    public static final float PRESENT_AND_FUTURE_BOOST = 1.0f;


    private transient NAR nar;


    final Space<TaskRegion> tree;


    public RTreeBeliefTable() {
        Spatialization<TaskRegion> model = new RTreeBeliefModel();

        this.tree = new ConcurrentRTree<>(
                new RTree<TaskRegion>(model) {

//                    @Override
//                    public boolean add(TaskRegion tr) {
//                        if (super.add(tr)) {
//                            //new insertion
//                            Task task = tr.task;
//                            Activate.activate(task, task.priElseZero(), nar);
//                            return true;
//                        }
//                        return false;
//                    }
                });
    }

    private static class TopDeleteVictims extends TopN<TaskRegion> {

        private final float inputStrength;

        public TopDeleteVictims(int count, FloatFunction<TaskRegion> weakestTask, float inputStrength) {
            super(new TaskRegion[count], weakestTask);
            this.inputStrength = inputStrength;
        }

        @Override
        public int add(TaskRegion element, float elementRank, FloatFunction<TaskRegion> cmp) {
            if (elementRank > inputStrength)
                return -1;
            return super.add(element, elementRank, cmp);
        }
    }


    final class MyTaskStretcher extends TaskRegionLink implements BooleanProcedure {


        MyTaskStretcher(SignalTask task) {
            super(task, task.freq(), task.conf());
        }

        @Override
        public void value(boolean updateOrFinish) {
            ((ConcurrentRTree<TaskRegion>) (tree)).write((treeRW) -> {

                boolean removed = treeRW.remove(MyTaskStretcher.this);

                if (updateOrFinish) {
                    end = task.end();

                    boolean added = treeRW.add(MyTaskStretcher.this);
                } else {
                    boolean added = treeRW.add(task /* re-add the frozen task directly, now that it has finished stretching */);
                }

            });
        }
    }


//    public void updateSignalTasks(long now) {
//
//        if (this.lastUpdate == now)
//            return;
//
//        this.lastUpdate = now;
//    }


    @Override
    public Truth truth(long start, long end, EternalTable eternal, NAR nar) {


        final Task ete = eternal != null ? eternal.strongest() : null;

        if (start == ETERNAL) start = end = nar.time();

        int s = size();
        if (s > 0) {

            int dur = nar.dur();

            FloatFunction<Task> ts = taskStrength(start, end, dur);
            FloatFunction<TaskRegion> strongestTask = (t -> +ts.floatValueOf(t.task()));


            int maxTruths = (int) Math.min(s, Math.max(2, Math.ceil(capacity * SCAN_MAX_FRACTION)));
            TopN<TaskRegion> tt = scan(
                    new TopN<>(new TaskRegion[maxTruths], strongestTask),
                    start, end, maxTruths);


            if (!tt.isEmpty()) {

                //                Iterable<? extends Tasked> ii;
                //                if (anyMatchTime) {
                //                    //tt.removeIf((x) -> !x.task().during(when));
                //                    ii = Iterables.filter(tt, (x) -> x.task().during(when));
                //                } else {
                //                    ii = tt;
                //                }

                //applying eternal should not influence the scan for temporal so it is left null here
                return Param.truth(ete, start, end, dur, tt);

                //        if (t != null /*&& t.conf() >= confMin*/) {
                //            return t.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f);
                //        } else {
                //            return null;
                //        }

            }
        }

        return ete != null ? ete.truth() : null;

    }

//    /**
//     * timerange spanned by entries in this table
//     */
//    public float timeRange() {
//        if (tree.isEmpty())
//            return 0f;
//        return (float) tree.root().region().range(0);
//    }

    @Override
    public Task match(long start, long end, @Nullable Term template, NAR nar) {

        if (start == ETERNAL) start = end = nar.time();

        assert (end >= start);


        int s = size();
        if (s == 0)
            return null;

        int dur = nar.dur();

        FloatFunction<Task> ts = taskStrength(template, start, end, dur);
        FloatFunction<TaskRegion> strongestTask = t -> +ts.floatValueOf(t.task());


        int maxTruths = (int) Math.min(s, Math.max(2, Math.ceil(capacity * SCAN_MAX_FRACTION)));
        Top2<TaskRegion> tt = scan(new Top2(strongestTask), start, end, maxTruths);

        switch (tt.size()) {

            case 0:
                return null;

            case 1:
                return tt.a.task();

            default:
                Task a = tt.a.task();
                Task b = tt.b.task();
                if (a.during(start, end) && !b.during(start, end))
                    return a; //only 'a' is for that time

                if (template != null) {
                    //choose if either one (but not both or neither) matches template's time
                    boolean at = (a.term().equals(template));
                    boolean bt = (b.term().equals(template));
                    if (at && !bt)
                        return a;
                    else if (bt && !at)
                        return b;
                }


                //otherwise interpolate
                Task c = Revision.merge(a, b, start, nar);
                if (c != null) {
//                    if (c.equals(a))
//                        return a;
//                    if (c.equals(b))
//                        return b;

                    return c;
                } else {
                    return a;
                }

        }
    }

    private <X extends Collection<TaskRegion>> X scan(X u, long start, long end, int maxAttempts) {
        //return ((ConcurrentRTree)tree).withReadLock() ?

        int s = size();

        if (s < MAX_TASKS_PER_LEAF * 2) {
            //all
            tree.forEach(u::add);
        } else {

            maxAttempts = Math.min(s, maxAttempts);

            //scan
            final int[] attempts = {0};
            Predicate<TaskRegion> update = x -> {
                attempts[0]++;
                u.add(x);
                return true;
            };

            TimeRange r = new TimeRange(); //recycled

            //check the precise range first
            tree.intersecting(r.set(start, end), update);

            if (u.isEmpty()) {

                //scan outwards

                TaskRegion bounds = (TaskRegion) (tree.root().region());
                if (bounds == null) //?? why
                    return null;

                long expand = Math.max(1, (bounds.end() - bounds.start()) / 8);

                long scanStart = start - 1, scanEnd = end + 1;
                long nextStart = start, nextEnd = end;
                int done;
                do {
                    nextStart -= expand;
                    nextEnd += expand;
                    done = 0;

                    if (nextStart >= bounds.start())
                        tree.intersecting(r.set(nextStart, scanStart), update);
                    else
                        done++;

                    if (nextEnd <= bounds.end())
                        tree.intersecting(r.set(scanEnd, nextEnd), update);
                    else
                        done++;

                    if (attempts[0] >= maxAttempts)
                        break;

                    scanStart = nextStart - 1;
                    scanEnd = nextEnd + 1;
                } while (done < 2);
            }
        }
        return u;
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compress on shrink
    }

    @Override
    public void add(@NotNull Task x, BaseConcept c, NAR n) {


//        if (x instanceof SignalTask && ((SignalTask) x).stretchKey != null)
//            return; //already added and being managed

        this.nar = n;

        boolean isSignal = x instanceof SignalTask;

        if (isSignal) {
            SignalTask sx = (SignalTask) x;
            if (sx.stretchKey instanceof MyTaskStretcher) {
                //ignore, it is already present; this actually should never be reached if the Signal works right
                return;
            }
        }

        TaskRegion tr;

        if (isSignal) {
            SignalTask sx = (SignalTask) x;
            if (sx.stretchKey == Signal.Pending) {
                //set the stretcher
                TaskRegionLink taskRegion = TaskRegionLink.link(x);
                sx.stretchKey = new MyTaskStretcher(sx);
                tr = taskRegion;
            } else {
                //throw new UnsupportedOperationException(sx + " either stretching or it isn't");
                tr = x; //new TaskLinkRegion(x);
            }
        } else {
            tr = x;//new TaskLinkRegion(x);
        }

        final boolean[] added = new boolean[1];
        ((ConcurrentRTree<TaskRegion>) tree).write(treeRW -> {
            //ensureCapacity(treeRW, null);

            added[0] = treeRW.add(tr);

            if (added[0])
                ensureCapacity(treeRW, tr);
        });


        if (added[0]) {
            float pri = x.pri();
            if (pri != pri) {
                //somehow it was added then immediately removed during compression, ie. rejected
            } else {
                Activate.activate(x, pri, nar);
            }
        } else {

            Object ar = x.lastLogged();
            if (ar instanceof Runnable) {
                x.log().remove(ar);
                ((Runnable) ar).run();
            }

        }

    }

    boolean ensureCapacity(Space<TaskRegion> treeRW, TaskRegion inputRegion) {
        int cap = this.capacity;
        if (treeRW.size() <= cap)
            return true;

        for (int e = 0; treeRW.size() > cap /*&& e < excess*/; e++) {
            if (!compress(treeRW, e == 0 ? inputRegion : null /** only limit by inputRegion first */, nar, cap))
                return false;
        }

        assert (treeRW.size() <= cap);
        return true;
    }

    /**
     * results in at least 1 less task being present in the table
     * assumes called with writeLock
     * returns false if the input was rejected as too weak
     */
    @NotNull
    private boolean compress(Space<TaskRegion> treeRW, @Nullable TaskRegion inputRegion, NAR nar, int cap) {

        long now = nar.time();
        int dur = nar.dur();


        FloatFunction<Task> taskStrength =
                //taskStrength(now-dur/2, now+dur/2, dur);
                taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, dur);
        FloatFunction<TaskRegion> weakestTask = (t -> -taskStrength.floatValueOf(t.task()));

        float inputStrength = inputRegion != null ? taskStrength.floatValueOf(inputRegion.task()) : Float.POSITIVE_INFINITY;


        FloatFunction<TaskRegion> rs = regionStrength(now, dur);
        FloatFunction<Node<?, TaskRegion>> leafWeakness = (l) -> -rs.floatValueOf((TaskRegion) (l.region()));

        final int DELETE_VICTIMS = 3; //should be greater than 2 in case the merge victims are included

        Top<Leaf<TaskRegion>> mergeVictim = new Top(leafWeakness);

        //0.
        int startSize = treeRW.size();
        if (startSize <= cap) return true; //compressed thanks to another thread

        TopN<TaskRegion> deleteVictim = new TopDeleteVictims(DELETE_VICTIMS, weakestTask, inputStrength);

        //1.
        findEvictable(treeRW, treeRW.root(), deleteVictim, mergeVictim);
        if (treeRW.size() <= cap) return true; //done, due to a removal of deleted items while finding eviction candiates

        //2.
        @Nullable Leaf<TaskRegion> toMerge = mergeVictim.the;
        if (toMerge != null) {
            FloatFunction<TaskRegion> regionWeakness = (r) -> -rs.floatValueOf(r);
            compressMerge(treeRW, toMerge, taskStrength, inputStrength, regionWeakness, now, nar);
            if (treeRW.size() <= cap) return true;
        }


        if (deleteVictim.isEmpty())
            return false; //input too weak

        //3.
        for (TaskRegion d : deleteVictim.list) {
            if (d != null) {
                if (tryDelete(treeRW, d))
                    if (treeRW.size() <= cap) return true;
            } else {
                break;
            }
        }

        return false; //?? could be a problem if it reaches here
    }


    private static boolean tryDelete(Space<TaskRegion> treeRW, @Nullable TaskRegion x) {
        if (x != null && treeRW.remove(x)) {
            x.task().delete();
            return true;
        }
        return false;
    }

    private static Task compressMerge(Space<TaskRegion> treeRW, Leaf<TaskRegion> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> regionWeakness, long now, NAR nar) {
        short s = l.size;
        assert (s > 0);

        TaskRegion a, b;
        if (s > 2) {
            Top2<TaskRegion> w = new Top2<>(regionWeakness);
            l.forEach(w::add);
            a = w.a;
            b = w.b;
        } else {
            a = l.get(0);
            b = l.get(1);
        }

        if (a != null && b != null) {
            Task at = a.task();
            Task bt = b.task();
            Task c = Revision.merge(at, bt, now, nar);
            if (c != null) {

                boolean allowMerge;

                if (inputStrength != inputStrength) {
                    allowMerge = true;
                } else {
                    float strengthRemoved = taskStrength.floatValueOf(at) + taskStrength.floatValueOf(bt);
                    float strengthAdded = taskStrength.floatValueOf(c) + inputStrength;
                    allowMerge = strengthAdded >= strengthRemoved;
                }


                if (allowMerge) {

                    //already has write lock so just use non-async methods
                    treeRW.remove(a); //only remove dont delete, it could involve the task being input in which case deleting will avoid activation
                    treeRW.remove(b);
                    treeRW.add(c);

                    return c;
                } else {
                    return null; //merge result is still isnt strong enough
                }

            }
        }

        return null;

    }


    static void findEvictable(Space<TaskRegion> tree, Node<TaskRegion, ?> next, Consumer<TaskRegion> deleteVictims, Top<Leaf<TaskRegion>> mergeVictims) {
        if (next instanceof Leaf) {

            Leaf<TaskRegion> l = (Leaf) next;

            int size = l.size;

            if (size > 1)
                mergeVictims.accept(l);

            Object[] ld = l.data;

            // remove any deleted tasks while scanning for victims
            for (int i = 0; i < size; i++) {
                TaskRegion t = (TaskRegion) ld[i];
//                if (t.task().isDeleted()) {
//                    //TODO this may disrupt the iteration being conducted, it may need to be deferred until after
//                    //boolean deleted = tree.remove(t); //already has write lock so just use non-async methods
//
//                } else {
                deleteVictims.accept(t);
//                }
            }


        } else { //if (next instanceof Branch)

            Branch b = (Branch) next;
            int size = b.size();
            Node<TaskRegion, ?>[] ww = b.child;
            for (int i = 0; i < size; i++) {
                findEvictable(tree, ww[i], deleteVictims, mergeVictims);
            }

//        int w = b.size();
//        if (w > 0) {
//
//            //recurse through a subest of the weakest regions, while also collecting victims
//            //select the 2 weakest regions and recurse
//            Top2<Node<TaskRegion, ?>> weakest = new Top2(
//                    mergeVictims.rank
//            );
//
//            for (int i = 0; i < w; i++) {
//                Node bb = b.get(i);
//                if (bb != null) {
//                    weakest.add(bb);
//                }
//            }
//
//            if (weakest.a != null)
//                compressNode(tree, weakest.a, deleteVictims, mergeVictims);
//            if (weakest.b != null)
//                compressNode(tree, weakest.b, deleteVictims, mergeVictims);
//        }

        }
//      else {
//            throw new RuntimeException();
//        }
    }


    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatFunction<TaskRegion> regionStrength(long when, int dur) {

        return (TaskRegion r) -> {

            float awayFromNow = //Math.abs(when - ((cb.start + cb.end)/2)); //now to its midpoint
                    //(Math.min(Math.abs(cb.start - when), Math.abs(cb.end - when)));
                    Math.min(Math.abs(r.start() - when), Math.abs(r.end() - when)) / ((float) dur); //optimistic distance


            return (float) ((1.0 / ((1 + awayFromNow)))
                    //* (1 + (r.range(0)) / awayFromNow) /* range, divided by the distance to emulate vanishing perspective proportion to distance */
                    //* (/*1 +*/ (r.range(2)) / 2)); /* conf */
                    * (1f + r.coord(true, 2)));


            //maximize confidence, minimize frequency variability, minimize distance to now
            //return (1 + awayFromNow/((float)dur)) * (1f + (cb.confMin+cb.confMax)/2 / (cb.freqMax - cb.freqMin) );

            //float timeSpan = (cb.end - cb.start) / fdur;
            //float timeSpanFactor = awayFromNow == 0 ? 0f : (timeSpan / ((timeSpan + awayFromNow)));

//            return
//                    (
//                    -1.0f * awayFromNow  //minimize distance to now
//                    //-0.5f * cb.temporalVariabilityAvg() //minimize temporalVariability
//                    )
//                        *

//
//                            (1f / (1f + (
//                        //2.0f * (cb.freqMax - cb.freqMin) +  //minimize frequency variation
//                        //0.25f * (cb.confMax - cb.confMin) +  //minimize confidence variation
//                    )));
        };
    }

    static FloatFunction<Task> taskStrength(long start, long end, int dur) {
        return (Task x) -> temporalTaskPriority(x, start, end, dur);
    }

    static FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int dur) {
        return (Task x) -> {
            if (x.isDeleted())
                return Float.NEGATIVE_INFINITY;

            //boost for present and future
            return (!x.isBefore(now - dur) ? presentAndFutureBoost : 1f) * temporalTaskPriority(x, when, when, dur);
        };
    }

    static FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end, int dur) {
        if (template == null || !template.isTemporal()) { //TODO this result can be cached for the entire table once knowing what term it stores
            return taskStrength(start, end, dur);
        } else {
            return (Task x) -> {
                return temporalTaskPriority(x, start, end, dur) / (1f + Revision.dtDiff(template, x.term()));
            };
        }
    }


    protected Task find(@NotNull TaskRegion t) {
        final Task[] found = {null};
        tree.intersecting(t, (x) -> {
            if (x.equals(t)) {
                @Nullable Task xt = x.task();
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
        return Iterators.transform(tree.iterator(), Tasked::task);
    }

    @Override
    public Stream<Task> stream() {
        return Streams.stream(taskIterator());
    }

    @Override
    public void forEach(long minT, long maxT, Consumer<? super Task> each) {
        tree.intersecting(new TimeRange(minT, maxT), (t) -> {
            each.accept(t.task());
            return true;
        });
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        tree.forEach(r -> {
            Task rt = r.task();
            if (rt != null)
                x.accept(rt);
        });
    }

    @Override
    public boolean removeTask(Task x) {
        return tree.remove(
                x
                //new TaskLinkRegion(x)
        );
    }


    @Override
    public void clear() {
        tree.clear();
    }

    public void print(PrintStream out) {
        forEachTask(t -> out.println(t.toString(true)));
        tree.stats().print(out);
    }

    private class RTreeBeliefModel extends Spatialization<TaskRegion> {


        public RTreeBeliefModel() {
            super((t -> t), RTreeBeliefTable.SPLIT, RTreeBeliefTable.MIN_TASKS_PER_LEAF, RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public Node<TaskRegion, TaskRegion> newLeaf() {
            return new BeliefLeaf(max);
        }

        @Override
        public void merge(TaskRegion existing, TaskRegion incoming) {

            Task i = incoming.task();
//                if (e == i)
//                    return; //same instance

            float activation = i.priElseZero();
            if (activation < Prioritized.EPSILON)
                return;

            Task e = existing.task();
            float before = e.priElseZero();
            ((NALTask) e).causeMerge(i);
            float after = e.priMax(activation);
            float activationApplied = (after - before);


            if (activationApplied >= Prioritized.EPSILON) {
                i.log((Runnable) () -> {
                    Activate.activate(e, activationApplied, nar);
                }); //store here so callee can activate outside of the lock
            }
        }

    }

    private static class BeliefLeaf extends Leaf<TaskRegion> {
        public BeliefLeaf(int max) {
            super(new TaskRegion[max]);
        }

        @Override
        public boolean contains(TaskRegion t, Spatialization<TaskRegion> model) {
            Task incomingTask = t.task();
            for (int i = 0; i < size; i++) {
                TaskRegion d = data[i];
                if (d == t) {
                    return true;
                }
                if (d.contains(t)) {
                    if (d.equals(t)) {
                        model.merge(d, t);
                        return true;
                    } else {
                        NALTask existingTask = (NALTask) d.task();
                        if (existingTask.term().equals(incomingTask.term())) {
                            if (Stamp.equalsIgnoreCyclic(existingTask.stamp(), incomingTask.stamp())) {
                                existingTask.causeMerge(incomingTask);
                                existingTask.priMax(incomingTask.priElseZero());
                                return true;
                            }
                        }
                    }
                }
            }
            return false;

        }
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
//                float actualActivation = x.priElseZero(); //will have been set; get this set pri which is actually the effective activation; then delete the task
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
