package nars.exe;

import jcog.Util;
import jcog.constraint.continuous.exceptions.InternalSolverError;
import jcog.event.On;
import jcog.exe.Can;
import jcog.exe.Schedulearn;
import jcog.learn.deep.RBM;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.NAR;
import nars.NARLoop;
import nars.Param;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.control.Traffic;
import nars.task.ITask;
import nars.task.NativeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;

    private On onClear;


    /**
     * schedules the task for execution but makes no guarantee it will ever actually execute
     */
    abstract public void add(/*@NotNull*/ ITask input);

    public void add(/*@NotNull*/ Iterator<? extends ITask> input) {
        input.forEachRemaining(this::add);
    }

    public final void add(/*@NotNull*/ Iterable<? extends ITask> input) {
        add(input.iterator());
    }

    public void add(/*@NotNull*/ Stream<? extends ITask> input) {
        add(input.iterator());
    }

    protected void execute(ITask x) {

        try {

            Iterable<? extends ITask> y = x.run(nar);
            if (y != null)
                add(y.iterator());

        } catch (Throwable e) {
            if (Param.DEBUG) {
                throw e;
            } else {
                logger.error("{} {}", x, e); //(Param.DEBUG) ? e : e.getMessage());
                x.delete();
            }
        }


    }

    abstract public void fire(Predicate<Activate> each);

    /**
     * an estimate or exact number of parallel processes this runs
     */
    abstract public int concurrency();


    abstract public Stream<Activate> active();

    public synchronized void start(NAR nar) {
        if (this.nar != null) {
            this.onClear.off();
            this.onClear = null;
        }

        this.nar = nar;

        onClear = nar.eventClear.on((n) -> clear());
    }

    public abstract void cycle();

    public synchronized void stop() {
        if (onClear != null) {
            onClear.off();
            onClear = null;
        }
    }

    abstract void clear();

    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }


    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }

    public void print(PrintStream out) {
        out.println(this);
    }


    public float load() {
        if (nar.loop.isRunning()) {
            return Util.unitize(nar.loop.lag());
        } else {
            return 0;
        }
    }





    abstract public void activate(Concept c, float activationApplied);

    public interface Revaluator {
        /**
         * goal and goalSummary instances correspond to the possible MetaGoal's enum
         */
        void update(FasterList<Cause> causes, float[] goal);
    }

    public static class DefaultRevaluator implements Revaluator {

        final RecycledSummaryStatistics[] causeSummary = new RecycledSummaryStatistics[MetaGoal.values().length];

        {
            for (int i = 0; i < causeSummary.length; i++)
                causeSummary[i] = new RecycledSummaryStatistics();
        }

        float momentum =
//                    0f;
                0.995f;

        @Override
        public void update(FasterList<Cause> causes, float[] goal) {

            for (RecycledSummaryStatistics r : causeSummary) {
                r.clear();
            }

            int cc = causes.size();
            for (int i = 0, causesSize = cc; i < causesSize; i++) {
                causes.get(i).commit(causeSummary);
            }


            int goals = goal.length;
//        float[] goalFactor = new float[goals];
//        for (int j = 0; j < goals; j++) {
//            float m = 1;
//                        // causeSummary[j].magnitude();
//            //strength / normalization_magnitude
//            goalFactor[j] = goal[j] / ( Util.equals(m, 0, epsilon) ? 1 : m );
//        }

            final float momentum = this.momentum;
            for (int i = 0, causesSize = cc; i < causesSize; i++) {
                Cause c = causes.get(i);

                Traffic[] cg = c.goalValue;

                //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
                float next = 0;
                for (int j = 0; j < goals; j++) {
                    next += goal[j] * cg[j].current;
                }

                float prev = c.value();

//                    0.99f * (1f - Util.unitize(
//                            Math.abs(next) / (1 + Math.max(Math.abs(next), Math.abs(prev)))));

                //c.setValue(Util.lerp(momentum, next, prev));
                c.setValue(0.9f * (next + prev));
            }
        }

    }

    /**
     * uses an RBM as an adaptive associative memory to learn and reinforce the co-occurrences of the causes
     * the RBM is an unsupervised network to learn and propagate co-occurring value between coherent Causes
     */
    public static class RBMRevaluator extends DefaultRevaluator {

        private final Random rng;
        public double[] next;

        /**
         * learning iterations applied per NAR cycle
         */
        public int learning_iters = 1;

        public double learning_rate = 0.05f;

        public double[] cur;
        public RBM rbm;

        /**
         * hidden to visible neuron ratio
         */
        private float hiddenMultipler = 0.5f;

        float rbmStrength = 0.1f;

        public RBMRevaluator(Random rng) {
            this.rng = rng;
            momentum = 1f - rbmStrength;
        }

        @Override
        public void update(FasterList<Cause> causes, float[] goal) {
            super.update(causes, goal);

            int numCauses = causes.size();
            if (numCauses < 2)
                return;

            if (rbm == null || rbm.n_visible != numCauses) {
                int numHidden = Math.round(hiddenMultipler * numCauses);

                rbm = new RBM(numCauses, numHidden, null, null, null, rng) {
                    @Override
                    public double activate(double a) {
                        return super.activate(a);
                        //return Util.tanhFast((float) a);
                        //return Util.sigmoidBipolar((float) a, 5);
                    }
                };
                cur = new double[numCauses];
                next = new double[numCauses];
            }


            for (int i = 0; i < numCauses; i++)
                cur[i] = Util.tanhFast(causes.get(i).value());

            rbm.reconstruct(cur, next);
            rbm.contrastive_divergence(cur, learning_rate, learning_iters);

            //float momentum = 0.5f;
            //float noise = 0.1f;
            for (int i = 0; i < numCauses; i++) {
                //float j = Util.tanhFast((float) (cur[i] + next[i]));
                float j = /*((rng.nextFloat()-0.5f)*2*noise)*/ +
                        //((float) (next[i]));
                        //(float)( Math.abs(next[i]) > Math.abs(cur[i]) ? next[i] : cur[i]);
                        (float) (cur[i] + rbmStrength * next[i]);
                causes.get(i).setValue(j);
            }
        }
    }

}
