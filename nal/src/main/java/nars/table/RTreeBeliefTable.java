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
import org.eclipse.collections.impl.map.mutable.primitive.ObjectBooleanHashMap;
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

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    static final float SCAN_QUALITY = 0.5f;

    /**
     * max allowed truths to be truthpolated in one test
     */
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

        tree = new ConcurrentRTree<>(new RTree<TaskRegion>(RTreeBeliefModel.the));

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
            scan(tt, start, end, maxTries, RTreeBeliefTable.ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS);


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

        FloatFunction<Task> ts =
                //(template != null && template.isTemporal()) ?
                    //taskStrength(template, start, end) :
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

    /**
     * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
     * order matters because the quality limit may terminate it.
     * however maybe the quality can be specified in terms that are compared
     * only after the pair has been scanned making the order irrelevant.
     */
    private <X extends Collection> X scan(X u, long _start, long _end, int maxAttempts, BiPredicate<X, TimeRange> continueScanning) {

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

            float timeRange = boundsEnd - boundsStart;
            int divisions = 4;
            long expand = Math.max(1, Math.round(timeRange / (1 << divisions)));

            //TODO use a polynomial or exponential scan expansion, to start narrow and grow wider faster


            long mid = (start + end) / 2;
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

                if (rightEnd <= boundsEnd && !(leftStart == rightMid && leftMid == rightEnd))
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
    }

    @Override
    public void add(Task x, BaseConcept c, NAR n) {

        if (x instanceof SignalTask) {
            SignalTask sx = (SignalTask) x;

            if (sx.stretch == null) {
                sx.stretch = this.stretch;
            } else {
                assert (sx.stretch == null); //should only be input once, when it has no stretch to update otherwise
            }
        }


        ObjectBooleanHashMap<Task> changes = new ObjectBooleanHashMap<>();
        ((ConcurrentRTree<TaskRegion>) tree).write(treeRW -> {

            ensureCapacity(treeRW, null, changes, n);

            if (!changes.getIfAbsent(x, true))
                return; //this can theoretically happen if a duplicate is inserted that the pre-compress just extracted. this catches it


            if (treeRW.add(x)) {
                changes.put(x, true);
                ensureCapacity(treeRW, x, changes, n);
            }
        });

        //check for an override about partial activation of the input which would have been appended to its log during rtree add/merge procedure
        Object ar = x.lastLogged();
        if (ar instanceof BiConsumer) {
            x.log().remove(ar);
            ((BiConsumer) ar).accept(n, c);
            boolean addOrRemInput = changes.removeKeyIfAbsent(x, true /*ignored*/);
            if (!addOrRemInput) {
                x.delete(); //delete which would have occurred in the block below anyway
            }
        }

        changes.forEachKeyValue((task, addOrRemove) -> {
            if (addOrRemove) {
                if (task == x) {
                    float pri = x.pri();

                    if (pri == pri) {
                        TermLinks.linkTask(x, pri, n, c);
                    }
                    //otherwise somehow it was added then immediately removed during compression, ie. rejected

                } else {
                    //it's a new merge
                    n.input(task);
                }
            } else {
                task.delete();
            }
        });




    }

    boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task inputRegion, ObjectBooleanHashMap<Task> changes, NAR nar) {
        int cap = this.capacity;
        int size = treeRW.size();
        if (size <= cap)
            return true;

        int dur = 1 + (int) ( tableDur());

        long now = nar.time();
        FloatFunction<Task> taskStrength =
                new CachedFloatFunction(
                        //taskStrength(now-dur/2, now+dur/2, dur);
                        taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, dur)
                );

        for (int e = 0; treeRW.size() > cap /*&& e < excess*/; e++) {
            if (!compress(treeRW, e == 0 ? inputRegion : null /** only limit by inputRegion first */, taskStrength, changes, cap, nar))
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
    private boolean compress(Space<TaskRegion> tree, @Nullable Task inputRegion, FloatFunction<Task> taskStrength, ObjectBooleanHashMap<Task> changes, int cap, NAR nar) {

        FloatFunction<TaskRegion> weakestTask = (t -> -taskStrength.floatValueOf((Task) t));

        float inputStrength = inputRegion != null ? taskStrength.floatValueOf(inputRegion) : Float.POSITIVE_INFINITY;


        FloatFunction<TaskRegion> rs = regionStrength(nar.time(), 1 + Math.round(tableDur()));
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
            if (compressMerge(tree, toMerge, taskStrength, inputStrength, lowestTaskConf, changes, nar)) {
                if (tree.size() <= cap) return true;
            }
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

    private static boolean compressMerge(Space<TaskRegion> treeRW, Leaf<TaskRegion> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> weakestTasks, ObjectBooleanHashMap<Task> changes, NAR nar) {
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
            Task c = Revision.merge(at, bt, nar.time(), nar);
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

                    treeRW.remove(at);
                    treeRW.remove(bt);
                    changes.put(at, false);
                    changes.put(bt, false);
                    changes.put(c, true);

                    return true;
                } else {
                    //merge result is not strong enough
                }

            }
        }

        return false;
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
                            (r.start() + r.end()) / 2L)
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
        HyperRegion root = tree.root().region();
        if (root == null)
            return 0;
        else
            return root.rangeIfFinite(0, 1);
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

            if (existing == incoming)
                return; //same instance

            Task i = incoming.task();
            float activation = i.priElseZero();
            if (activation < Prioritized.EPSILON)
                return;

            Task e = existing.task();
            float before = e.priElseZero();
            ((NALTask) e).causeMerge(i);
            float after = e.priMax(activation);
            float activationApplied = (after - before);


            BiConsumer<NAR, Concept> partialActivationSignal;
            if (activationApplied >= Prioritized.EPSILON) {
                partialActivationSignal = ((BiConsumer<NAR, Concept>) (nar, c) -> {
                    TermLinks.linkTask(e, activationApplied, nar, c);
                }); //store here so callee can activate outside of the lock
            } else {
                partialActivationSignal = (x,y)-> { /* nop */};
            }

            i.log(partialActivationSignal);
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
