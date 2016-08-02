package nars.op.time;

import nars.NAR;
import nars.bag.impl.ArrayBag;

import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.learn.gng.NeuralGasNet;
import nars.learn.gng.Node;
import nars.link.BLink;
import nars.link.StrongBLink;
import nars.task.Task;
import nars.util.Util;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class STMClustered extends STM {

    static final int DIMENSIONS = 2;
    final int TIME = 0;
    final int FREQ = 1;


    final short clusters;


    @NotNull
    public final ArrayBag<Task> input;

    @NotNull
    public final NeuralGasNet<TasksNode> net;

    protected long now;


    public final char punc;

    //final float timeResolution = 0.5f;


    private static final int compactPeriod = 8;
    private long lastCompact;

    public final class TasksNode extends Node {

        /** current members */
        public final Map<Task,TLink> tasks = new ConcurrentHashMap();


        public TasksNode(int id, int dimensions) {
            super(id, dimensions);
            randomizeUniform(0, now-1, now+1);
            randomizeUniform(1, 0f, 1f);
            filter();
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
            return super.toString() + ":" + tasks;
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

        protected boolean remove(TLink x) {
            x.node = null;
            if (tasks.remove(x)!=null) {
                return true;
            }
            //if (requireRemoval)
                //throw new RuntimeException("task not in set");
            return false;
        }

        public int size() {
            return tasks.size();
        }

        public void insert(@NotNull TLink x) {
            @Nullable Task xx = x.get();
            if (xx != null && !xx.isDeleted()) {
                if (x.node == this)
                    return;

                tasks.put(xx, x);
//                if (tasks.putIfAbsent(xx, x) != null) {
//                    throw new RuntimeException(xx + " already in this node's task set");
//                }

                x.node = this;
            } else {
                //task is deleted
                x.node = null;
            }
        }

        public void delete() {
            tasks.clear();
        }

        /** 1f - variance measured from the items for a given vector dimension */
        @Nullable
        public double[] coherence(int dim) {
            if (size() == 0) return null;
            double[] v = Util.avgvar(tasks.values().stream().mapToDouble(t -> t.coord[dim]).toArray()); //HACK slow
            v[1] = 1f - v[1]; //convert variance to coherence
            return v;
        }

        //TODO cache this value
        public float priSum() {
            return (float)tasks.values().stream().mapToDouble(TLink::pri).sum();
        }

        /** produces a parallel conjunction term consisting of all the task's terms */
        public Stream<Task[]> termSet(int maxComponentsPerTerm, int maxVolume) {
            AtomicInteger group = new AtomicInteger();
            AtomicInteger subterms = new AtomicInteger();
            AtomicInteger currentVolume = new AtomicInteger();
            return tasks.values().stream().map(TLink::get).
                    filter(x -> x!=null ? true : false).
                    collect(Collectors.groupingBy(x -> {

                        int v = x.volume();

                        if ((subterms.intValue() == maxComponentsPerTerm) || (currentVolume.intValue() + v > maxVolume)) {
                            //next group
                            subterms.set(0);
                            currentVolume.set(0);
                            group.incrementAndGet();
                        }

                        subterms.incrementAndGet();
                        currentVolume.addAndGet(v);

                        return group.intValue();
                    }))
                    .values().stream()
                    .filter(c -> c.size()> 1)
                    .map(c -> c.toArray(new Task[c.size()]));
        }

//        public float confMin() {
//            return (float)tasks.stream().mapToDouble(t->t.get().conf()).min().getAsDouble();
//        }



        protected void remove(Task[] uu) {
            for (Task x : uu) {
                input.remove(x);
                tasks.remove(x);
            }
        }

    }

    /**
     * temporal link, centroid
     */
    public final class TLink extends StrongBLink<Task> implements Comparable<TLink> {

        /** feature vector representing the item as learned by clusterer */
        @NotNull
        public final double[] coord;

        /** current centroid */
        TasksNode node;

        public TLink(@NotNull Task t, float p, float d, float q) {
            super(t, p, d, q);
            this.coord = getCoord(t);
        }

        @NotNull
        @Override
        public String toString() {
            return id + "<<" +
                    Arrays.toString(coord) +
                    "|" + node.id +
                    ">>";
        }

        @Override
        public void commit() {
            @Nullable Task id = this.id;

            if (id == null || id.isDeleted()) {
                delete();
            } else {
                //priSub(cycleCost(id));
                nearest().transfer(this);
                super.commit();
            }
        }

        private TasksNode nearest() {
            return net.learn(coord);
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                if (node!=null)
                    node.remove(this);
                return true;
            }
            return false;
        }

        public void migrate() {
            nearest().insert(this);
        }

        @Override
        public int compareTo(TLink o) {
            if (id == o.id)
                return 0;
            if (id == null)
                return 1;
            if (o.id == null)
                return -1;
            return id.compareTo(o.id);
        }
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

    @NotNull
    public static double[] getCoord(@NotNull Task t) {
        double[] c = new double[DIMENSIONS];
        c[0] = t.occurrence(); //time
        c[1] = t.freq(); //0..+1
        return c;
    }

    final Deque<TasksNode> removed = new ArrayDeque<>();



    public STMClustered(@NotNull NAR nar, @NotNull MutableInteger capacity, char punc, int expectedTasksPerNode) {
        super(nar, capacity);

        //TODO make this adaptive
        clusters = (short)Math.max(2f, 1f + capacity.floatValue() / expectedTasksPerNode);

        this.punc = punc;
        this.input = new ArrayBag<>(1, BudgetMerge.avgBlend, new HashMap<>(capacity.intValue())) {


            @NotNull
            @Override
            protected BLink<Task> newLink(@NotNull Task i, float p, float d, float q) {
                return new TLink(i, p, d, q);
            }

            @Override
            protected BLink<Task> putNew(Task i, BLink<Task> newBudget) {
                BLink<Task> displaced = super.putNew(i, newBudget);
                if (displaced != null)
                    drop((TLink)displaced);
                return displaced;
            }
        };

        this.net = new NeuralGasNet<>(DIMENSIONS, clusters) {
            @NotNull
            @Override
            public STMClustered.TasksNode newNode(int i, int dims) {
                return new TasksNode(i, dims);
            }

            @Override
            protected void removed(TasksNode furthest) {
                //System.err.println("node removed: " + furthest);
                removed.add(furthest);
            }
        };

        now = nar.time();

        start();
    }

    final AtomicBoolean ready = new AtomicBoolean(true);

    @Override
    protected void start() {
        super.start();
        nar.onFrame((nn) -> {
            if (ready.compareAndSet(true, false)) {
                nn.runLater(this::iterate);
            }
        });
    }

    protected void iterate() {
        input.setCapacity(capacity.intValue());

        int rr = removed.size();
        for (int i = 0; i < rr; i++) {
            TasksNode t = removed.pollFirst();
            t.tasks.values().forEach(TLink::migrate);
            t.delete();
        }

        long t = nar.time();
        if (t - lastCompact > compactPeriod) {
            net.compact();
            lastCompact = t;
        }

        now = t;


        input.commit();

        ready.set(true);

    }

    @Override
    public void clear() {
        input.clear();
    }

    @Override
    public void accept(@NotNull Task t) {

        if (t.punc() == punc) {

            input.put(t, t.budget());
        }

    }

    protected void drop(@NotNull TLink displaced) {
        TasksNode owner = displaced.node;
        if (owner != null)
            owner.remove(displaced);
    }

    public int size() {
        return input.size();
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
        return StreamSupport.stream(input.spliterator(), false).mapToDouble(Budgeted::pri).summaryStatistics();
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
//                return new MutableTask(term(u), /*(Math.random() < 0.5f) ?*/ '.' /*: '!'*/, new DefaultTruth((float) Math.random(), 0.5f)).time(now, now);
//            }
//        };
//
//        n.run(24);
//    }
}
