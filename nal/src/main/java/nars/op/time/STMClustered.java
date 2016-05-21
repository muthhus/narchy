package nars.op.time;

import com.google.common.base.Joiner;
import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.bag.BLink;
import nars.bag.impl.ArrayBag;
import nars.budget.Budgeted;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.util.data.MutableInteger;
import nars.util.gng.NeuralGasNet;
import nars.util.gng.Node;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
            return id + "<<" + super.toString() + ">>:" + tasks;
        }

        public void enter(TLink x) {
            TasksNode previous = x.node;
            if (previous == this)
                return; //nothing to do

            if (previous!=null) {
                previous.remove(x);
            }
            tasks.add(x);
            x.node = this;
        }

        protected void remove(TLink x) {
            tasks.remove(x);
        }

    }

    /** temporal link, centroid */
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

        @Override public boolean commit() {
            priSub(cycleCost(id));
            net.learn(coord).enter(this);
            return super.commit();
        }

        @Override
        public void delete() {
            if (node!=null) {
                node.remove(this);
            }
            super.delete();
        }
    }

    /** amount of priority subtracted from the priority each iteration */
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

    public STMClustered(@NotNull NAR nar, MutableInteger capacity) {
        super(nar, capacity);

        this.bag = new ArrayBag<Task>(1) {
            @Override
            protected BLink<Task> newLink(Task i, Budgeted b, float scale) {
                return new TLink(i, b, scale);
            }
        };
        this.net = new NeuralGasNet<>(DIMENSIONS, clusters) {
            @NotNull @Override public STMClustered.TasksNode newNode(int i, int dims) {
                return new TasksNode(i, dims);
            }

            @Override
            protected void beforeRemove(TasksNode furthest) {
                System.err.println("node being removed: " + furthest);
            }
        };

        now = nar.time();

        nar.onFrame(n-> {
            //update each frame
            bag.setCapacity(capacity.intValue());
        });
        nar.onCycle(n -> {
            cycle();
        });

        start();
    }

    private void cycle() {
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
        bag.put(t, t.budget());
    }

    public void print(PrintStream out) {
        out.println(this + " @" + now);
        bag.forEach(b -> {
            out.println(b);
        });
        out.println(Joiner.on('\n').join(net.vertexSet()));
        out.println(Joiner.on(' ').join(net.edgeSet()));
        out.println();
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
        STMClustered stm = new STMClustered(n, new MutableInteger(64));

        new EventGenerator(n, 4f, 8) {

            Compound term(int u) {
                return $.sete($.the(u));
            }

            @Override Task task(int u) {
                return new MutableTask(term(u), '.', new DefaultTruth( (float)Math.random(), 0.5f) ).time(now, now);
            }
        };

        n.run(16);
    }
}
