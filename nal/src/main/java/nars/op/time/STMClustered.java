package nars.op.time;

import nars.NAR;
import nars.bag.impl.ArrayBag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.learn.gng.NeuralGasNet;
import nars.learn.gng.Node;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.link.StrongBLink;
import nars.task.Task;
import nars.util.Util;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class STMClustered extends STM {

    static final int DIMENSIONS = 2;
    final int TIME = 0;
    final int FREQ = 1;


    final short clusters;

    @NotNull
    public final ArrayBag<Task> bag;
    @NotNull
    public final NeuralGasNet<TasksNode> net;
    protected long now;

    final float forgetRate = 0.01f; //TODO tune based on capacity, window size, etc.

    public final char punc;

    //final float timeResolution = 0.5f;

    private static final double[] EmptyCoherence = new double[] { Double.NaN, Double.NaN };

    private static int compactPeriod = 8;

    public final class TasksNode extends Node {

        /** current members */
        public final Set<TLink> tasks = new HashSet();


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

        protected void remove(TLink x) {
            tasks.remove(x);
        }

        public int size() {
            return tasks.size();
        }

        public void insert(@NotNull TLink x) {
            tasks.add(x);
            x.node = this;
        }

        public void delete() {
            tasks.clear();
        }

        /** 1f - variance measured from the items for a given vector dimension */
        public double[] coherence(int dim) {
            if (size() == 0) return EmptyCoherence;
            double[] v = Util.avgvar(tasks.stream().mapToDouble(t -> t.coord[dim]).toArray()); //HACK slow
            v[1] = 1f - v[1]; //convert variance to coherence
            return v;
        }

        //TODO cache this value
        public float priSum() {
            return (float)tasks.stream().mapToDouble(DefaultBLink::pri).sum();
        }

        /** produces a parallel conjunction term consisting of all the task's terms */
        public Stream<Task[]> termSet(int maxComponentsPerTerm) {
            AtomicInteger as = new AtomicInteger();
            return tasks.stream().map(StrongBLink::get).distinct().
                    collect(Collectors.groupingBy(x -> as.incrementAndGet() / (1+maxComponentsPerTerm)))
                    .values().stream()
                    .filter(c -> c.size()> 1)
                    .map(c -> c.toArray(new Task[c.size()]));
        }

//        public float confMin() {
//            return (float)tasks.stream().mapToDouble(t->t.get().conf()).min().getAsDouble();
//        }




        /** removes all tasks that are part of this node, and removes them from the bag also, effectively flushing these tasks out of this STM unit */
        public void clear() {
            tasks.forEach(t -> bag.remove(t.get()));
            tasks.clear();
        }
    }

    /**
     * temporal link, centroid
     */
    public final class TLink extends StrongBLink<Task> {

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
            if (get().isDeleted()) {
                delete();
            }
            priSub(cycleCost(id));
            nearest().transfer(this);
            super.commit();
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
    }

    /**
     * amount of priority subtracted from the priority each iteration
     */
    private float cycleCost(@NotNull Task id) {
        float dt = Math.abs(id.occurrence() - now);
        return forgetRate * dt * (1f - id.conf());
    }

    @NotNull
    public static double[] getCoord(@NotNull Task t) {
        double[] c = new double[DIMENSIONS];
        c[0] = t.occurrence(); //time
        c[1] = t.freq(); //0..+1
        return c;
    }

    final Deque<TasksNode> removed = new ArrayDeque<>();

    final int expectedTasksPerNode = 4;

    public STMClustered(@NotNull NAR nar, @NotNull MutableInteger capacity, char punc) {
        super(nar, capacity);

        //TODO make this adaptive
        clusters = (short)Math.max(2, 1 + capacity.intValue() / expectedTasksPerNode);

        this.punc = punc;
        this.bag = new ArrayBag<>(1, BudgetMerge.avgDQBlend) {
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

    @Override
    protected void start() {
        super.start();
        nar.runAsync(this::iterate, 1);
    }

    protected void iterate() {
        bag.setCapacity(capacity.intValue());

        int rr = removed.size();
        for (int i = 0; i < rr; i++) {
            TasksNode t = removed.pollFirst();
            t.tasks.forEach(TLink::migrate);
            t.delete();
        }

        long t = nar.time();
        if (t - now > compactPeriod) {
            net.compact();
        }
        now = nar.time();
        bag.commit();


        //net.compact();

        //print(System.out);
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Override
    public void accept(@NotNull Task t) {

        if (t.punc() == punc) {

            bag.put(t, t.budget());
        }

    }

    protected void drop(@NotNull TLink displaced) {
        TasksNode owner = displaced.node;
        if (owner != null)
            owner.remove(displaced);
    }

    public int size() {
        return bag.size();
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
        return StreamSupport.stream(bag.spliterator(), false).mapToDouble(Budgeted::pri).summaryStatistics();
    }


    abstract static class EventGenerator implements Consumer<NAR> {

        @NotNull
        private final NAR n;
        private final float averageTasksPerFrame;
        //private final float variation;
        private final int uniques;
        protected long now;

        public EventGenerator(@NotNull NAR n, float averageTasksPerFrame, /*float variation,*/ int uniques) {
            this.n = n;
            this.averageTasksPerFrame = averageTasksPerFrame;
            //this.variation = variation;
            this.uniques = uniques;

            n.onFrame(this);
        }

        @Override
        public void accept(@NotNull NAR nar) {
            now = n.time();

            int numInputs = (int) Math.round(Math.random() * averageTasksPerFrame);
            for (int i = 0; i < numInputs; i++) {
                int u = (int) Math.floor(Math.random() * uniques);
                nar.input(task(u));
            }
        }

        @NotNull
        abstract Task task(int u);
    }

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
