package nars.op.time;

import com.google.common.base.Joiner;
import nars.*;
import nars.bag.BLink;
import nars.bag.impl.ArrayBag;
import nars.budget.Budgeted;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.truth.DefaultTruth;
import nars.util.data.MutableInteger;
import nars.util.gng.NeuralGasNet;
import nars.util.gng.Node;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;


public class STMClustered extends STM {

    static final int DIMENSIONS = 3;
    final int TIME = 0;
    final int FREQ = 1;
    final int PUNC = 2;


    final int clusters = 9;

    private final ArrayBag<Task> bag;
    private final NeuralGasNet<TasksNode> net;
    private long now;

    final float forgetRate = 0.01f; //TODO tune based on capacity, window size, etc.

    public final class TasksNode extends Node {

        public final Set<TLink> tasks = new HashSet();

        public TasksNode(int id, int dimensions) {
            super(id, dimensions);
        }

        @Override
        public String toString() {
            return super.toString() + ":" + tasks;
        }

        public void transfer(TLink x) {
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

        public void insert(TLink x) {
            tasks.add(x);
            x.node = this;
        }

        public void delete() {
            tasks.clear();
        }
    }

    /**
     * temporal link, centroid
     */
    public final class TLink extends BLink.StrongBLink<Task> {

        public final double[] coord;
        TasksNode node;

        public TLink(Task t, @NotNull Budgeted b, float scale) {
            super(t, b, scale);
            this.coord = getCoord(t);
        }

        @Override
        public String toString() {
            return id + "<<" +
                    Arrays.toString(coord) +
                    "|" + node.id +
                    ">>";
        }

        @Override
        public boolean commit() {
            priSub(cycleCost(id));
            nearest().transfer(this);
            return super.commit();
        }

        public TasksNode nearest() {
            return net.learn(coord);
        }

        @Override
        public void delete() {
            if (node != null) {
                node.remove(this);
            }
            super.delete();
        }

        public void migrate() {
            nearest().insert(this);
        }
    }

    /**
     * amount of priority subtracted from the priority each iteration
     */
    private float cycleCost(Task id) {
        float dt = Math.abs(id.occurrence() - now);
        return forgetRate * dt / (1f + id.conf());
    }

    public static double[] getCoord(Task t) {
        double[] c = new double[DIMENSIONS];
        c[0] = t.occurrence();
        c[1] = 2f * (t.freq() - 0.5f); //-1 .. +1
        c[2] = (t.punc() == Symbols.BELIEF) ? +1f : -1f;
        return c;
    }

    final Deque<TasksNode> removed = new ArrayDeque<>();

    public STMClustered(@NotNull NAR nar, MutableInteger capacity) {
        super(nar, capacity);

        this.bag = new ArrayBag<Task>(1) {
            @Override
            protected BLink<Task> newLink(Task i, Budgeted b, float scale) {
                return new TLink(i, b, scale);
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
                System.err.println("node removed: " + furthest);
                removed.add(furthest);
            }
        };

        now = nar.time();

        nar.onFrame(n -> {
            //update each frame
            bag.setCapacity(capacity.intValue());
        });
        nar.onCycle(n -> {
            cycle();
        });

        start();
    }

    private void cycle() {

        int rr = removed.size();
        for (int i = 0; i < rr; i++) {
            TasksNode t = removed.pollFirst();
            t.tasks.forEach(TLink::migrate);
            t.delete();
        }



        now = nar.time();
        bag.commit();

        print(System.out);
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Override
    public void accept(@NotNull Task t) {

        TLink displaced = (TLink) bag.put(t, t.budget());

        if (displaced != null)
            drop(displaced);

    }

    protected void drop(TLink displaced) {
        TasksNode owner = displaced.node;
        if (owner != null)
            owner.remove(displaced);
    }

    public int size() {
        return bag.size();
    }

    public void print(PrintStream out) {
        out.println(this + " @" + now + ", x " + size() + " tasks");
        out.println("\t" + distributionStatistics() + "\t" + removed.size() + " nodes pending migration ("
         + removed.stream().mapToInt(TasksNode::size).sum() + " tasks)");
        /*bag.forEach(b -> {
            out.println(b);
        });*/
        out.println(Joiner.on('\n').join(net.vertexSet()));
        out.println(Joiner.on(' ').join(net.edgeSet()));
        out.println();
    }

    public IntSummaryStatistics distributionStatistics() {
        return net.vertexSet().stream().mapToInt(v -> v.size()).summaryStatistics();
    }


    abstract static class EventGenerator implements Consumer<NAR> {

        private final NAR n;
        private final float averageTasksPerFrame;
        //private final float variation;
        private final int uniques;
        protected long now;

        public EventGenerator(NAR n, float averageTasksPerFrame, /*float variation,*/ int uniques) {
            this.n = n;
            this.averageTasksPerFrame = averageTasksPerFrame;
            //this.variation = variation;
            this.uniques = uniques;

            n.onFrame(this);
        }

        @Override
        public void accept(NAR nar) {
            now = n.time();

            int numInputs = (int) Math.round(Math.random() * averageTasksPerFrame);
            for (int i = 0; i < numInputs; i++) {
                int u = (int) Math.floor(Math.random() * uniques);
                nar.input(task(u));
            }
        }

        abstract Task task(int u);
    }

    public static void main(String[] args) {
        Default n = new Default();
        STMClustered stm = new STMClustered(n, new MutableInteger(16));

        new EventGenerator(n, 2f, 8) {

            Compound term(int u) {
                return $.sete($.the(u));
            }

            @Override
            Task task(int u) {
                return new MutableTask(term(u), '.', new DefaultTruth((float) Math.random(), 0.5f)).time(now, now);
            }
        };

        n.run(24);
    }
}
