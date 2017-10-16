package nars.table;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import jcog.pri.Prioritized;
import jcog.sort.TopN;
import jcog.tree.rtree.*;
import jcog.util.CachedFloatFunction;
import jcog.util.Top;
import jcog.util.Top2;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.TermLinks;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.SignalTask;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;

public class RTreeBeliefTable implements TemporalBeliefTable {

    /** max fraction of the fully capacity table to compute in a single truthpolation */
    static final float SCAN_QUALITY = 0.5f;

    /** max allowed truths to be truthpolated in one test */
    static final int TRUTHPOLATION_LIMIT = 4;

    public static final float PRESENT_AND_FUTURE_BOOST = 1f;


    public static final int MIN_TASKS_PER_LEAF = 2;
    public static final int MAX_TASKS_PER_LEAF = 4;
    public static final Spatialization.DefaultSplits SPLIT =
            Spatialization.DefaultSplits.AXIAL; //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here

    public static final BiPredicate<Collection, TimeRange> ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS = (u, r) -> {
        return u.isEmpty(); //quit after even only one, now that an entire range has been scanned
    };



    private int capacity;

    final Space<TaskRegion> tree;
    final LongObjectProcedure<SignalTask> stretch;

    public RTreeBeliefTable() {

        tree = new ConcurrentRTree<>(  new RTree<TaskRegion>(RTreeBeliefModel.the) );

        stretch = (newEnd, task) -> {
            ((ConcurrentRTree<TaskRegion>) (tree)).write((treeRW) -> {

                boolean removed = treeRW.remove(task);

                task.setEnd(newEnd);

                boolean added = treeRW.add(task);

            });
        };
    }

    private static final class TopDeleteVictims extends TopN<TaskRegion> {

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




    @Override
    public Truth truth(long start, long end, EternalTable eternal, NAR nar) {


        final Task ete = eternal != null ? eternal.strongest() : null;

        if (start == ETERNAL) start = end = nar.time();
        assert (end >= start);

        int s = size();
        if (s > 0) {


            FloatFunction<Task> ts = taskStrength(start, end);
            FloatFunction<TaskRegion> strongestTask =
                    new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));


            int maxTruths = TRUTHPOLATION_LIMIT;
            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
            TopN<TaskRegion> tt = new TopN<>(new TaskRegion[maxTruths], strongestTask);
            scan(tt, start, end, maxTries, RTreeBeliefTable.ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS );


            if (!tt.isEmpty()) {

                //                Iterable<? extends Tasked> ii;
                //                if (anyMatchTime) {
                //                    //tt.removeIf((x) -> !x.task().during(when));
                //                    ii = Iterables.filter(tt, (x) -> x.task().during(when));
                //                } else {
                //                    ii = tt;
                //                }

                //applying eternal should not influence the scan for temporal so it is left null here
                return Param.truth(ete, start, end, nar.dur(), tt);

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

        int s = size();
        if (s == 0) //quick exit
            return null;

        if (start == ETERNAL) start = end = nar.time();
        assert (end >= start);

        FloatFunction<Task> ts = (template != null && template.isTemporal()) ?
                taskStrength(template, start, end) :
                taskStrength(start, end);

        FloatFunction<TaskRegion> strongestTask =
                new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));

        int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
        Top2<TaskRegion> tt = new Top2<>(strongestTask);
        scan(tt, start, end, maxTries, ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS);

        switch (tt.size()) {

            case 0:
                return null;

            case 1:
                return tt.a.task();

            default:
                Task a = tt.a.task();
                Task b = tt.b.task();

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

                    int dur = nar.dur();
                    if (c.evi(start, end, dur) > a.evi(start, end, dur))
                        return c;
                }

                return a;


        }
    }

    /** TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
     * order matters because the quality limit may terminate it.
     * however maybe the quality can be specified in terms that are compared
     * only after the pair has been scanned making the order irrelevant.
     */
    private <X extends Collection> X scan(X u, long _start, long _end, int maxAttempts, BiPredicate<X,TimeRange> continueScanning) {

        ((ConcurrentRTree<TaskRegion>) tree).read((RTree<TaskRegion> tree) -> {

            int s = tree.size();
            if (s == 0)
                return;
            if (s == 1) {
                tree.forEach(u::add);
                return;
            }


            int maxTries = Math.min(s, maxAttempts);

            //scan
            final int[] attempts = {0};
            Predicate<TaskRegion> update = x -> {
                u.add(x);
                return attempts[0]++ < maxTries;
            };

            TaskRegion bounds = (TaskRegion) (tree.root().region());

            long boundsStart = bounds.start();
            long boundsEnd = bounds.end();

            long start = Math.max(boundsStart, _start);
            long end = Math.min(boundsEnd, _end);

            float timeRange = boundsEnd-boundsStart;
            int divisions = 4;
            long expand = Math.max(1, Math.round(timeRange / (1 << divisions)));

            //TODO use a polynomial or exponential scan expansion, to start narrow and grow wider faster


            long mid = (start+end)/2;
            long leftStart = start, leftMid = mid, rightMid = mid, rightEnd = end;
            int complete;
            //TODO float complete and use this as the metric for limiting with scan quality parameter
            TimeRange r = new TimeRange(); //recycled
            do {
                complete = 0;

                if (leftStart >= boundsStart)
                    tree.intersecting(r.set(leftStart, leftMid), update);
                else
                    complete++;

                if (rightEnd <= boundsEnd && !(leftStart==rightMid && leftMid==rightEnd))
                    tree.intersecting(r.set(rightMid, rightEnd), update);
                else
                    complete++;

                if (complete == 2 || attempts[0] >= maxTries)
                    break;

                if (!continueScanning.test(u, r.set(leftStart, rightEnd)))
                    break;

                leftStart = leftStart - expand - 1;
                 leftMid = leftMid - 1;
                rightMid = rightMid + 1;
                rightEnd = rightEnd + expand + 1;


                expand *= 2; //accelerate on the next cycle

            } while (true);

        });

        return u;
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        //TODO compress on shrink
    }

    @Override
    public void add(Task x, BaseConcept c, NAR n) {

        if (x instanceof SignalTask) {
            SignalTask sx = (SignalTask) x;

            if (sx.stretch == null) {
                sx.stretch = this.stretch;
            } else {
                assert(sx.stretch == null); //should only be input once, when it has no stretch to update otherwise
            }
        }

        final boolean[] added = new boolean[1];
        ((ConcurrentRTree<TaskRegion>) tree).write(treeRW -> {
            //ensureCapacity(treeRW, null);

            added[0] = treeRW.add(x);

            if (added[0])
                ensureCapacity(treeRW, x, n);
        });


        if (added[0]) {
            float pri = x.pri();
            if (pri != pri) {
                //somehow it was added then immediately removed during compression, ie. rejected
            } else {
                TermLinks.linkTask(x, pri, n, c);
            }
        } else {

            Object ar = x.lastLogged();
            if (ar instanceof BiConsumer) {
                x.log().remove(ar);
                ((BiConsumer) ar).accept(n, c);
            }

        }

    }

    boolean ensureCapacity(Space<TaskRegion> treeRW, Task inputRegion, NAR nar) {
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
    /*@NotNull*/
    private boolean compress(Space<TaskRegion> tree, @Nullable Task inputRegion, NAR nar, int cap) {

        long now = nar.time();
        int perceptDur = nar.dur();


        FloatFunction<Task> taskStrength =
                new CachedFloatFunction(
                        //taskStrength(now-dur/2, now+dur/2, dur);
                        taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, perceptDur)
                );

        FloatFunction<TaskRegion> weakestTask = (t -> -taskStrength.floatValueOf((Task) t));

        float inputStrength = inputRegion != null ? taskStrength.floatValueOf(inputRegion) : Float.POSITIVE_INFINITY;


        FloatFunction<TaskRegion> rs = regionStrength(now, 1 + Math.round(tableDur()));
        FloatFunction<Node<?, TaskRegion>> leafWeakness = (l) -> -rs.floatValueOf((TaskRegion) (l.region()));

        final int DELETE_VICTIMS = 3; //should be greater than 2 in case the merge victims are included

        Top<Leaf<TaskRegion>> mergeVictim = new Top(leafWeakness);

        //0.
        int startSize = tree.size();
        if (startSize <= cap) return true; //compressed thanks to another thread

        TopN<TaskRegion> deleteVictim = new TopDeleteVictims(DELETE_VICTIMS, weakestTask, inputStrength);

        //1.
        findEvictable(tree, tree.root(), deleteVictim, mergeVictim);
        if (tree.size() <= cap)
            return true; //done, due to a removal of deleted items while finding eviction candiates

        //2.
        @Nullable Leaf<TaskRegion> toMerge = mergeVictim.the;
        if (toMerge != null) {
            compressMerge(tree, toMerge, taskStrength, inputStrength, lowestTaskConf, now, nar);
            if (tree.size() <= cap) return true;
        }


        if (deleteVictim.isEmpty())
            return false; //input too weak

        //3.
        for (TaskRegion d : deleteVictim.list) {
            if (d != null) {
                if (tryDelete(tree, d))
                    if (tree.size() <= cap) return true;
            } else {
                break;
            }
        }

        return false; //?? could be a problem if it reaches here
    }

    static final FloatFunction<TaskRegion> lowestTaskConf = (r) -> -((Task) r).conf();

    private static boolean tryDelete(Space<TaskRegion> treeRW, @Nullable TaskRegion x) {
        if (x != null && treeRW.remove(x)) {
            x.task().delete();
            return true;
        }
        return false;
    }

    private static Task compressMerge(Space<TaskRegion> treeRW, Leaf<TaskRegion> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> weakestTasks, long now, NAR nar) {
        short s = l.size;
        assert (s > 0);

        TaskRegion a, b;
        if (s > 2) {
            Top2<TaskRegion> w = new Top2<>(weakestTasks);
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
    private static FloatFunction<TaskRegion> regionStrength(long when, long dur) {

        return (TaskRegion r) -> {

            float maxConf = (float) r.coord(true, 2); //optimistic
            float evi = w2c(Param.evi(w2c(maxConf), dur,
                    Math.abs(when -
                            (r.start() + r.end())/2L )
                    //Task.nearestBetween(r.start(), r.end(), when)))
            ));

            float span = (float) ((1 + r.range(0)) / dur);

            return evi * span;
        };
    }

    FloatFunction<Task> taskStrength(long start, long end) {
        int tableDur = 1 + (int) (tableDur()); //TODO HACK should be 'long' the belief table could span a long time
        return (Task x) -> temporalTaskPriority(x, start, end, tableDur);
    }

    public double tableDur() {
        return tree.root().region().rangeIfFinite(0, 1);
    }

    FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur) {
        int tableDur = 1 + (int) (tableDur());
        return (Task x) -> {
            if (x.isDeleted())
                return Float.NEGATIVE_INFINITY;

            //boost for present and future
            return (!x.isBefore(now - perceptDur) ? presentAndFutureBoost : 1f) * temporalTaskPriority(x, when, when, tableDur);
        };
    }

    FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end) {
        if (template == null || !template.isTemporal()) { //TODO this result can be cached for the entire table once knowing what term it stores
            return taskStrength(start, end);
        } else {
            int tableDur = 1 + (int) (tableDur());
            return (Task x) -> {
                return temporalTaskPriority(x, start, end, tableDur) / (1f + Revision.dtDiff(template, x.term()));
            };
        }
    }


//    protected Task find(/*@NotNull*/ TaskRegion t) {
//        final Task[] found = {null};
//        tree.intersecting(t, (x) -> {
//            if (x.equals(t)) {
//                Task xt = x.task();
//                if (xt != null) {
//                    found[0] = xt;
//                    return false; //finished
//                }
//            }
//            return true;
//        });
//        return found[0];
//    }


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
            Task tt = t.task();
            if (tt != null)
                each.accept(tt);
            return true;
        });
    }

    @Override
    public void forEachTask(Consumer<? super Task> each) {
        tree.forEach(t -> {
            Task tt = t.task();
            if (tt != null)
                each.accept(tt);
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

    private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {


        public static Spatialization<TaskRegion> the = new RTreeBeliefModel();
        ;

        public RTreeBeliefModel() {
            super((t -> t), RTreeBeliefTable.SPLIT, RTreeBeliefTable.MIN_TASKS_PER_LEAF, RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public final HyperRegion region(TaskRegion taskRegion) {
            return taskRegion;
        }

        //        @Override
//        public Node<TaskRegion, TaskRegion> newLeaf() {
//            return new BeliefLeaf(max);
//        }

        @Override
        protected void merge(TaskRegion existing, TaskRegion incoming) {

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
                i.log((BiConsumer<NAR, Concept>) (nar, c) -> {
                    TermLinks.linkTask(e, activationApplied, nar, c);
                }); //store here so callee can activate outside of the lock
            }
        }

    }

//    private static class BeliefLeaf extends Leaf<TaskRegion> {
//        public BeliefLeaf(int max) {
//            super(new TaskRegion[max]);
//        }
//
//
////        @Override
////        public boolean contains(TaskRegion t, Spatialization<TaskRegion> model) {
////            if (region == null)
////                return false;
//////            if (!region.contains(t))
//////                return false;
////
////            Task incomingTask = t.task();
////            TaskRegion[] data = this.data;
////            final int s = size;
////            for (int i = 0; i < s; i++) {
////                TaskRegion d = data[i];
////                if (d == t) {
////                    return true;
////                }
////                if (d.contains(t)) {
////                    if (d.equals(t)) {
////                        model.merge(d, t);
////                        return true;
////                    } else {
////                        NALTask existingTask = (NALTask) d.task();
////                        if (existingTask.term().equals(incomingTask.term())) {
////                            if (Stamp.equalsIgnoreCyclic(existingTask.stamp(), incomingTask.stamp())) {
////                                existingTask.causeMerge(incomingTask);
////                                existingTask.priMax(incomingTask.priElseZero());
////                                return true;
////                            }
////                        }
////                    }
////                }
////            }
////            return false;
////
////        }
//    }

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
//        public TaskRegion(/*@NotNull*/ Task task) {
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
//    public void add(/*@NotNull*/ Task x, TaskConcept c, NAR n) {
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
//    private void add(/*@NotNull*/ Task t) {
//        tree.addAsync(new TaskRegion(t));
//    }
//
//    /**
//     * assumes called with writeLock
//     */
//    /*@NotNull*/
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
//    protected Task find(/*@NotNull*/ TaskRegion t) {
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
