package nars.op.stm;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.MutableInteger;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Node;
import jcog.pri.WeakPLinkUntilDeleted;
import nars.NAR;
import nars.Task;
import nars.control.TaskService;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * clusterjunctioning
 * TODO abstract into general purpose "Cluster of Bags" class
 */
public abstract class STMClustered extends TaskService {


    //final @Nullable Bag<Task, PriReference<Task>> input;
    public final MutableInteger capacity;

    final short clusters;
    public final int dims;

    long now;


    @NotNull
    public final NeuralGasNet<TasksNode> net;

    //final Map<TLink,TasksNode> transfer = new ConcurrentHashMap();


    public final byte punc;

//    final Deque<TasksNode> removed =
//            new ArrayDeque<>();
    //new ConcurrentLinkedDeque<>();

    final static double[] noCoherence = {0, 0};

    public final class TasksNode extends Node {

        private final Bag<TLink, TLink> tasks;

        /**
         * current members
         */


        public TasksNode(int id) {

            super(id, dims);
            tasks = new PriorityHijackBag<>(capacity.intValue(), 3) {

                @Override
                protected TLink merge(@NotNull STMClustered.TLink existing, @NotNull STMClustered.TLink incoming, @Nullable MutableFloat overflowing) {
                    existing.priMax(incoming.priElseZero());
                    return existing;
//                    float overflow =
//                    if (overflow > 0) {
//                        //pressurize(-overflow);
//                        if (overflowing != null) overflowing.add(overflow);
//                    }
//                    return existing; //default to the original instance
                }

                @Override
                protected Consumer<TLink> forget(float rate) {
                    return null;
                }

                @Override
                public void onAdded(@NotNull TLink x) {
                    x.node = TasksNode.this;
                }

                @Override
                public TLink key(TLink value) {
                    return value;
                }
            };
        }

        @Override
        public void update(double rate, double[] x) {
            super.update(rate, x);
            filter();
        }

        @Override
        public void add(double[] x) {
            super.add(x);
            filter();
        }

        protected void filter() {
//            final double[] d = getDataRef();
//            double t = d[TIME];
//            d[TIME] = Math.round(t);
//            double p = d[PUNC];
//            d[PUNC] = p < 0 ? -1 : 1; //force to polarize -1 (goal) or +1 (belief)
        }

        @NotNull
        @Override
        public String toString() {
            return super.toString() + ':' + tasks;
        }

        public void transfer(@NotNull TLink x) {
            TasksNode previous = x.node;
            if (previous == this)
                return; //nothing to do

            if (previous != null) {
                previous.remove(x);
            }
            insert(x);
        }

        protected void remove(@NotNull TLink x) {
            x.node = null;
            tasks.remove(x);
        }

        public int size() {
            return tasks.size();
        }

        public void insert(@NotNull TLink x) {

            if (x.node != this) {
                tasks.putAsync(x);
            }


        }

        /**
         * inverse of variance measured from the items for a given vector dimension
         */
        @Nullable
        public double[] coherence(int dim) {

            double[] v = Util.variance(tasks.stream().mapToDouble(t -> t.coord[dim])); //HACK slow

            if (v == null)
                return noCoherence;

            v[1] = 1f / (1f + Math.sqrt(v[1])); //convert variance to coherence
            return v;
        }

//        //TODO cache this value
//        public float priSum() {
//            return (float) tasks.stream().mapToDouble(TLink::pri).sum();
//        }

        /**
         * produces a parallel conjunction term consisting of all the task's terms
         */
        public Stream<List<TLink>> chunk(int maxComponentsPerTerm, int maxVolume) {
            final int[] group = {0};
            final int[] subterms = {0};
            final int[] currentVolume = {0};
            return tasks.stream().
                    //filter(x -> x.get() != null).
                            collect(Collectors.groupingBy(tx -> {

                        Task x = tx.get();
                        if (x == null)
                            return -1;

                        int v = x.volume();

                        if ((subterms[0] >= maxComponentsPerTerm) || (currentVolume[0] + v > maxVolume)) {
                            //next group
                            group[0]++;
                            subterms[0] = 1;
                            currentVolume[0] = v;
                        } else {

                            subterms[0]++;
                            currentVolume[0] += v;
                        }

                        return group[0];
                    }))
                    .entrySet().stream()
                    .filter(c -> c.getKey() >= 0)
                    .map(Map.Entry::getValue)//ignore the -1 discard group
                    .filter(c -> c.size() > 1); //only batches of >1

        }

        /**
         * returns the range of values seen for the given dimension
         */
        public double range(int dim) {
            final double[] min = {Double.POSITIVE_INFINITY};
            final double[] max = {Double.NEGATIVE_INFINITY};
            tasks.forEach(x -> {
                double v = x.coord[dim];
                if (v < min[0]) min[0] = v;
                if (v > max[0]) max[0] = v;
            });
            return Double.isFinite(min[0]) ? max[0] - min[0] : 0;
        }

//        public float confMin() {
//            return (float)tasks.stream().mapToDouble(t->t.get().conf()).min().getAsDouble();
//        }


    }

    /**
     * temporal link, centroid
     */
    public final class TLink extends WeakPLinkUntilDeleted<Task> implements Truthed {

        /**
         * feature vector representing the item as learned by clusterer
         */
        @NotNull
        public final double[] coord;

        /**
         * current centroid
         */
        @Nullable TasksNode node;

        public TLink(@NotNull Task t) {
            super(t, t.priElseZero());
            this.coord = coord(t);
        }

        @Override
        public @Nullable Truth truth() {
            return get().truth();
        }

        @NotNull
        @Override
        public String toString() {
            return get() + "<<" +
                    (coord != null ? Arrays.toString(coord) : "0") +
                    '|' + (node != null ? node.id : "null") +
                    ">>";
        }


        private TasksNode nearest() {
            return net.put(filter(coord));
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                if (node != null) {
                    node.remove(this);
                }
                return true;
            }
            return false;
        }


//        @Override
//        public int compareTo(TLink o) {
//            @Nullable Task id = this.id;
//            @Nullable Task oid = o.id;
//            if (id == oid)
//                return 0;
////            if (id == null)
////                return 1;
////            if (oid == null)
////                return -1;
//            return id.compareTo(oid);
//        }
    }


    /**
     * allows transparent filtering of a task's coord vector just prior to gasnet learning.
     * ex: for normalization or shifting of time etc
     */
    protected double[] filter(@NotNull double[] coord) {
        return coord;
    }

//    @Deprecated final float baseForgetRate = 0.01f;
//    @Deprecated final float forgetRate = 0.01f;
//
//    /**
//     * amount of priority subtracted from the priority each iteration
//     */
//    @Deprecated private float cycleCost(@NotNull Task id) {
//        //float dt = Math.abs(id.occurrence() - now);
//        return baseForgetRate + forgetRate * (1f - id.conf() * id.originality());
//    }

    abstract double[] coord(@NotNull Task t);


    protected STMClustered(int dims, @NotNull NAR nar, @NotNull MutableInteger capacity, byte punc, int centroids) {
        super(nar);

        this.capacity = capacity;

        this.dims = dims;

        this.clusters = (short) centroids;

        this.punc = punc;


//        this.input =
//                //new ArrayBag<>(capacity.intValue(), PriMerge.max, new ConcurrentHashMap<>(capacity.intValue())) {
//                new DefaultHijackBag<>(PriMerge.max, capacity.intValue(), 3) {

//            @NotNull
//            @Override
//            public BLink<Task> newLink(@NotNull Task i, BLink<Task> exists) {
//                if (!(exists instanceof TLink) || exists.isDeleted())
//                    return new TLink(i);
//                return exists;
//            }


//                    @Override
//                    public void onRemoved(@NotNull PriReference<Task> value) {
////                        TasksNode owner = ((TLink) value).node;
////                        if (owner != null)
////                            owner.remove((TLink) value);
////                        value.delete();
//                    }
//
//                };

        this.net = new NeuralGasNet<>(dims, clusters) {
            @NotNull
            @Override
            public STMClustered.TasksNode newNode(int i, int _dims) {
                TasksNode c = newCentroid(i);
                c.filter();
                return c;
            }

//                    public void onRemoved(@NotNull TLink value) {
//                        TasksNode owner = ((TLink) value).node;
//                        if (owner != null)
//                            owner.remove((TLink) value);
//                        value.delete();
//                    }

            @Override
            protected void removed(TasksNode furthest) {
                //System.err.println("node removed: " + furthest);
                //removed.add(furthest);

                furthest.tasks.sample(t -> {
                    //TODO either attempt re-insert or delete
                    t.delete();
                    return Bag.BagSample.Remove;
                });


            }
        };

        now = nar.time();

        nar.onCycle(this::iterate);
    }

    abstract protected TasksNode newCentroid(int id);

    final AtomicBoolean busy = new AtomicBoolean(false);

    protected boolean iterate(NAR nar) {

        if (busy.compareAndSet(false, true)) {

            now = nar.time();

//            int rr = removed.size();
//            for (int i = 0; i < rr; i++) {
//                TasksNode t = removed.pollFirst();
//                t.tasks.forEach(TLink::migrate);
//                t.delete();
//            }

//            input.setCapacity(capacity.intValue());
//            input.commit();

            net.compact();

            busy.set(false);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void clear() {
        net.clear();
    }

    @Override
    public void accept(NAR nar, @NotNull Task t) {


        TLink tt = new TLink(t);

        TasksNode nearest;
        synchronized (net) {
            nearest = tt.nearest();
        }

        synchronized (net) {
            nearest.transfer(tt);
        }


    }


    public int size() {
        int sum[] = new int[1];
        net.forEachNode(x -> sum[0] += x.tasks.size());
        return sum[0];
    }

    public void print(@NotNull PrintStream out) {
        out.println(this + " @" + now + ", x " + size() + " tasks");
//        out.println("\tNode Sizes: " + nodeStatistics() + "\t+" + removed.size() + " nodes pending migration ("
//                + removed.stream().mapToInt(TasksNode::size).sum() + " tasks)");
        net.forEachNode(v -> {
            out.println(v);
            out.println("\t[Avg,Coherence]: Temporal=" + Arrays.toString(v.coherence(0)) +
                    "\tFrequency=" + Arrays.toString(v.coherence(1)));
        });

        /*bag.forEach(b -> {
            out.println(b);
        });*/
        //out.println(Joiner.on(' ').join(net.edgeSet()));
        out.println();
    }


    public IntSummaryStatistics nodeStatistics() {
        return net.nodeStream().mapToInt(TasksNode::size).summaryStatistics();
    }


//    abstract static class EventGenerator implements Consumer<NAR> {
//
//        @NotNull
//        private final NAR n;
//        private final float averageTasksPerFrame;
//        //private final float variation;
//        private final int uniques;
//        protected long now;
//
//        public EventGenerator(@NotNull NAR n, float averageTasksPerFrame, /*float variation,*/ int uniques) {
//            this.n = n;
//            this.averageTasksPerFrame = averageTasksPerFrame;
//            //this.variation = variation;
//            this.uniques = uniques;
//
//            n.onFrame(this);
//        }
//
//        @Override
//        public void accept(@NotNull NAR nar) {
//            now = n.time();
//
//            int numInputs = (int) Math.round(Math.random() * averageTasksPerFrame);
//            for (int i = 0; i < numInputs; i++) {
//                int u = (int) Math.floor(Math.random() * uniques);
//                nar.input(task(u));
//            }
//        }
//
//        @NotNull
//        abstract Task task(int u);
//    }

//    public static void main(String[] args) {
//        Default n = new Default();
//        STMClustered stm = new STMClustered(n, new MutableInteger(16), '.');
//
//        new EventGenerator(n, 2f, 8) {
//
//            Compound term(int u) {
//                return $.sete($.the(u));
//            }
//
//            @NotNull
//            @Override
//            Task task(int u) {
//                return new TaskBuilder(term(u), /*(Math.random() < 0.5f) ?*/ '.' /*: '!'*/, new DefaultTruth((float) Math.random(), 0.5f)).time(now, now);
//            }
//        };
//
//        n.run(24);
//    }
}
