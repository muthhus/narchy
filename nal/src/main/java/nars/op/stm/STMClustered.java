package nars.op.stm;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.data.MutableInteger;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.Node;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.budget.DependentBLink;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * clusterjunctioning
 */
public abstract class STMClustered extends STM {


    public final ThreadLocal<@Nullable Bag<Task, PLink<Task>>> input;

    final short clusters;
    public final int dims;

    long now;


    @NotNull
    public final NeuralGasNet<TasksNode> net;

    //final Map<TLink,TasksNode> transfer = new ConcurrentHashMap();


    public final byte punc;

    final Deque<TasksNode> removed =
            new ArrayDeque<>();
            //new ConcurrentLinkedDeque<>();

    final static double[] noCoherence = { 0, 0 };

    public final class TasksNode extends Node {

        /**
         * current members
         */
        public final Set<TLink> tasks = nar.exe.concurrent() ?
                Collections.newSetFromMap(new ConcurrentHashMap<>()) : new LinkedHashSet<>();


        public TasksNode(int id) {
            super(id, dims);
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

        protected boolean remove(@NotNull TLink x) {
            x.node = null;
            return tasks.remove(x);
        }

        public int size() {
            return tasks.size();
        }

        public void insert(@NotNull TLink x) {
            Task xx = x.get();
            //priSub(cycleCost(id));

            if (xx != null) {

                if (x.node != this) {
                    tasks.add(x);
                    x.node = this;
                }

            } else {
                //task is deleted
                if (x.node == this) {
                    tasks.remove(x);
                    x.node = null;
                }
            }
        }

        public void delete() {
            tasks.clear();
        }



        /**
         * inverse of variance measured from the items for a given vector dimension
         */
        @Nullable
        public double[] coherence(int dim) {

            double[] v = Util.variance(tasks.stream().mapToDouble(t -> t.coord[dim])); //HACK slow

            if (v==null)
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

//        public float confMin() {
//            return (float)tasks.stream().mapToDouble(t->t.get().conf()).min().getAsDouble();
//        }


    }

    /**
     * temporal link, centroid
     */
    public final class TLink extends DependentBLink<Task> implements Truthed {

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
            super(t);
            this.coord = getCoord(t);
        }

        public TLink(@NotNull Task t, float p, float q) {
            super(t, p, q);
            this.coord = getCoord(t);
        }

        @Override
        public @Nullable Truth truth() {
            return get().truth();
        }

        @NotNull
        @Override
        public String toString() {
            return id + "<<" +
                    (coord != null ? Arrays.toString(coord) : "0") +
                    '|' + (node != null ? node.id : "null") +
                    ">>";
        }


        private TasksNode nearest() {
            synchronized (net) {
                return net.learn(coord);
            }
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                if (node != null)
                    node.remove(this);
                return true;
            }
            return false;
        }

        public void migrate() {
            nearest().insert(this);
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

    abstract double[] getCoord(@NotNull Task t);


    public STMClustered(int dims, @NotNull NAR nar, @NotNull MutableInteger capacity, byte punc, int expectedTasksPerNode) {
        super(nar, capacity);

        this.dims = dims;

        //TODO make this adaptive
        clusters = (short) Math.max(2f, 1f + capacity.floatValue() / expectedTasksPerNode);

        this.punc = punc;

        //this.input = new ArrayBag<Task>(capacity.intValue(), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity.intValue())) {
        this.input = ThreadLocal.withInitial(() ->
                new DefaultHijackBag<Task>(capacity.intValue(), 2, PriMerge.max, nar.random) {

//            @NotNull
//            @Override
//            public BLink<Task> newLink(@NotNull Task i, BLink<Task> exists) {
//                if (!(exists instanceof TLink) || exists.isDeleted())
//                    return new TLink(i);
//                return exists;
//            }



            @Override
            public void onRemoved(@NotNull PLink<Task> value) {
                super.onRemoved(value);
                drop((TLink) value);
            }


//            @Override
//            public void onRemoved(@Nullable BLink<Task> value) {
//                drop((TLink) value);
//            }

        });

        this.net = new NeuralGasNet<TasksNode>(dims, clusters) {
            @NotNull
            @Override
            public STMClustered.TasksNode newNode(int i, int _dims) {
                TasksNode c = newCentroid(i);
                c.filter();
                return c;
            }

            @Override
            protected void removed(TasksNode furthest) {
                //System.err.println("node removed: " + furthest);
                removed.add(furthest);
            }
        };

        now = nar.time();

        nar.onCycle((nn) -> iterate());
    }

    abstract protected TasksNode newCentroid(int id);

    final AtomicBoolean busy = new AtomicBoolean(false);

    protected boolean iterate() {

        if (busy.compareAndSet(false, true)) {

            int rr = removed.size();
            for (int i = 0; i < rr; i++) {
                TasksNode t = removed.pollFirst();
                t.tasks.forEach(TLink::migrate);
                t.delete();
            }

            input.get().setCapacity(capacity.intValue());
            input.get().commit();

            net.compact();

            now = nar.time();

            input.get().forEach(t -> {
                if (t != null) {
                    TLink tt = (TLink) t;
                    tt.nearest().transfer(tt);
                }
            });

            busy.set(false);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void clear() {
        input.get().clear();
    }

    @Override
    public void accept(@NotNull Task t) {

        if (t.punc() == punc) {
            input.get().put(new TLink(t));
        }

    }

    protected void drop(@NotNull TLink displaced) {
        TasksNode owner = displaced.node;
        if (owner != null)
            owner.remove(displaced);
    }

    public int size() {
        return input.get().size();
    }

    public void print(@NotNull PrintStream out) {
        out.println(this + " @" + now + ", x " + size() + " tasks");
        out.println("\tNode Sizes: " + nodeStatistics() + "\t+" + removed.size() + " nodes pending migration ("
                + removed.stream().mapToInt(TasksNode::size).sum() + " tasks)");
        out.println("\tBag Priority: " + bagStatistics());
        net.forEachVertex(v -> {
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

    public DoubleSummaryStatistics bagStatistics() {
        return StreamSupport.stream(input.get().spliterator(), false).mapToDouble(Prioritized::pri).summaryStatistics();
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
