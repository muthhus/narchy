package nars.op.time;

import nars.NAR;
import nars.Symbols;
import nars.bag.BLink;
import nars.bag.impl.ArrayBag;
import nars.budget.Budgeted;
import nars.task.Task;
import nars.util.data.MutableInteger;
import nars.util.gng.NeuralGasNet;
import nars.util.gng.Node;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


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

        public TLink(Task t, @NotNull Budgeted b, float scal) {
            super(t, b, scal);
            this.coord = getCoord(t);
        }

        @Override
        public String toString() {
            return id + "<<" +
                    Arrays.toString(coord) +
                    "|" + node +
                    ">>";
        }

        @Override public boolean commit() {
            priSub(cycleCost(id));
            net.closest(coord).enter(this);
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
                return super.newLink(i, b, scale);
            }
        };
        this.net = new NeuralGasNet<>(DIMENSIONS, clusters) {
            @NotNull @Override public STMClustered.TasksNode newNode(int dimension, int i) {
                return new TasksNode(dimension, i);
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
    }
}
