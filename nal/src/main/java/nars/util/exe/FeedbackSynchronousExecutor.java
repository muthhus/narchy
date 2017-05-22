package nars.util.exe;


import jcog.Texts;
import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.control.DerivePremise;
import nars.nar.Default;
import nars.premise.Premise;
import nars.task.ITask;
import nars.test.agent.Line1DSimplest;
import nars.time.CycleTime;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

import static nars.Op.*;

/**
 * instruments tasks for tracking causal factors, as a precursor for full feedback system
 */
public class FeedbackSynchronousExecutor extends BufferedSynchronousExecutor {

    final static int CLASSES = 36; //TODO calculate based on active classifiers

    private static final float TRAFFIC_DECAY = 0.5f;

    /** between 0 and 1, lower means filter is stronger */
    public static final float priRetained = 0.1f;

    final BlockRealMatrix traffic = new BlockRealMatrix(CLASSES, CLASSES);
    final ArrayRealVector goal = new ArrayRealVector(CLASSES);
    private RealVector filter = null;

    public final RecycledSummaryStatistics saving = new RecycledSummaryStatistics();

    @Override
    protected void actuallyRun(@NotNull ITask input) {

        ITask[] outputs = input.run(nar); //adapted from SynchronousExecutor's


        if (outputs != null) {

            if (input instanceof DerivePremise) {
                RoaringBitmap c = classify(((DerivePremise) input).premise());
                if (outputs.length > 1)
                    c.trim();
                for (ITask output : outputs) {
                    RoaringBitmap oo = classify((Task) output);
                    learn(input.pri() * output.pri(), c, oo);
                    if (filter != null) {

                        //dot product: estimated utility towards goal
                        final float[] eu = {0};
                        oo.forEach((int i) -> {
                            eu[0] += goal.getEntry(i);
                        });

                        float estimatedUtility = eu[0];
                        float s = filter(output, estimatedUtility);
                        this.saving.accept(s);
                        if (estimatedUtility >= 0) {
                            run(output);
                        } else {
                            //System.out.println("culled: " + output);
                        }
                    }
                }
            } else {
                for (ITask output : outputs) {
                    run(output);
                }
            }
        }
    }

    private float filter(ITask output, float estimatedUtility) {
        if (estimatedUtility < 0) {
            return output.priSafe(0); //all of it
        }

        if (estimatedUtility < 1f) {
            float pre = output.priSafe(0);
            if (pre > 0) {
                float saving = 0;
                //System.out.println(Texts.n4(estimatedUtility) + " est " + output);
                output.priMult(priRetained + (1f - priRetained) * estimatedUtility);
                saving = pre - output.pri();
                return saving/pre;
            }
        }
        return 0;
    }

    private int classify(RoaringBitmap b, Task n, int offset) {

        int o = offset;

        if (n != null) {
            switch (n.punc()) {
                case BELIEF:
                    b.add(o + 0);
                    break;
                case GOAL:
                    b.add(o + 1);
                    break;
                case QUESTION:
                    b.add(o + 2);
                    break;
                case QUEST:
                    b.add(o + 3);
                    break;
            }
        }
        o += 4;

        final int truthBins = 3;
        if (n != null) {
            if (n.isBeliefOrGoal()) {
                int conf = Util.bin(n.conf(), truthBins);
                b.add(o + conf);
                int freq = Util.bin(n.freq(), truthBins);
                b.add(o + truthBins + freq);
            }
        }
        o += truthBins * 2;

        int opBins = Op.values().length;
        b.add(o + n.term().op().ordinal());
        o += opBins;

        //TODO sub-structures

        return o;
    }

    public RoaringBitmap classify(Task input) {
        RoaringBitmap r = new RoaringBitmap();
        classify(r, input, 0);
        return r;
    }

    private RoaringBitmap classify(Premise input) {
        RoaringBitmap r = new RoaringBitmap();

        int j = classify(r, input.task, 0);
        Task b = input.belief;
        if (b != null) {
            //j = classify(r, b, j);
            classify(r, b, 0);
        }
        return r;
    }


    private void learn(float p, @NotNull RoaringBitmap ii, RoaringBitmap oo) {
        //System.out.println(nar.time() + ": " + ii + " -> " + output  + " (" + oo + ")");
        ii.forEach((int i) -> {
            oo.forEach((int o) -> {
                traffic.addToEntry(i, o, p);
            });
        });
    }

    private void goalClear() {
        goal.set(0);
    }

    public void goalAdd(RoaringBitmap b) {
        goalAdd(b, 1f);
    }

    public void goalAdd(RoaringBitmap b, float strength) {
        b.forEach((int x) -> {
            goal.setEntry(x, strength);
        });
    }

    public void goalNormalize() {
        double n = goal.getNorm();
        if (n > 0)
            goal.mapMultiplyToSelf(1f / n);
    }

    public RealVector updateFilter() {


        filter = traffic.preMultiply(goal);
        double norm = filter.getNorm();
        if (norm > 0)
            filter.mapMultiplyToSelf(1f / norm);

        mult(traffic, TRAFFIC_DECAY);

        return filter;
    }

    protected static void mult(BlockRealMatrix m, float factor) {
        double[][] tt = m.getData();
        for (double[] tr : tt) {
            for (int i = 0; i < tr.length; i++)
                tr[i] *= factor;
        }
    }

    public static void print(RealMatrix exe) {
        for (int i = 0; i < CLASSES; i++)
            System.out.println(Texts.n4(exe.getRow(i)));
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        FeedbackSynchronousExecutor exe = new FeedbackSynchronousExecutor();
        Default n = new Default(128, new Default.DefaultTermIndex(128), new CycleTime(),
                exe);

        Line1DSimplest l = new Line1DSimplest(n);

        exe.goalAdd(exe.classify(n.task("(o-->L)! %1.0;0.5%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %0.0;0.5%")));
        exe.goalNormalize();


        for (int i = 0; i < 10; i++) {
            l.runCycles(64);

            exe.updateFilter();

            System.out.println("saved: " + exe.saving);
            exe.saving.clear();
        }


    }

}

//    public class InstrumentedTask extends ProxyTask {
//
//        public String log = "";
//        final RoaringBitmap cls;
//
//        public InstrumentedTask(ITask x, InstrumentedTask parent) {
//            super(x);
//
//            cls = classify(this);
//            if (parent!=null)
//                learn(parent, this);
//
//            StackWalker.StackFrame callee = StackWalker.getInstance().walk(s ->
//                    s.skip(3).findFirst().get()); //limit(3) .collect(Collectors.toList()
//            log = callee.toString();
//        }
//
//        private void learn(InstrumentedTask cause, InstrumentedTask effect) {
//            float u = utility(effect);
//            cause.cls.forEach((int row) -> {
//                effect.cls.forEach((int col) -> {
//                    //matrix.put(...
//                });
//            });
//        }
//
//        private float utility(InstrumentedTask instrumentedTask) {
//            return 0;
//
//        }
//
//
//
//        @Override
//        public ITask merge(ITask incoming) {
//            ITask next = super.merge(incoming);
//            assert(next==this);
//            if (incoming instanceof InstrumentedTask)
//                log += " " + ((InstrumentedTask) incoming).log;
//            return this;
//        }
//
//        @Override
//        public ITask[] run(NAR n) {
//            return super.run(n);
//        }
//
//        @Override
//        public String toString() {
//            return the.toString();
//        }
//
//        public void print() {
//            System.out.println(toString());
//            System.out.println("\t" + log);
//        }
//    }
