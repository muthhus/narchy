package jcog.learn.ql;

import jcog.Util;
import jcog.decide.DecideSoftmax;
import jcog.math.ParallelCMAESOptimizer;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.MathArrays;

import java.util.Arrays;
import java.util.Random;
import java.util.function.ToDoubleFunction;

import static java.lang.System.arraycopy;
import static jcog.Texts.n4;

public class CMAESAgent implements MultivariateFunction /*implements Agent*/ {

    private final double[] mid, min, max, range, inc;
    private final ParallelCMAESOptimizer.CMAESProcess optProcess;

    /** # of dimensions holding the input */
    private final int in;

    /** # of dimensions holding the output */
    private final int out;

    private ParallelCMAESOptimizer opt;
    private int population = 10;
    private double[] ins;
    private float reward;
    private double[] search;

    private double[] outs;
    final DecideSoftmax decider = new DecideSoftmax(0.25f, 0.25f, 0.5f, new XorShift128PlusRandom(1));
    private int lastAction;

    final Random rng = new XorShift128PlusRandom(1);
    double noise = 0.01f;

    /** assumes 0..1.0 range */
    public static CMAESAgent build(int in, int out, ToDoubleFunction<double[]> eval) {
        int dim = in + out;
        double[] min = new double[ dim ];
        double[] max = new double[ dim ];
        Arrays.fill(max, 1.0);
        return new CMAESAgent(in, out, min, max);
    }

    public CMAESAgent(int in, int out, double[] min, double[] max) {
        int n = min.length;
        assert (n == max.length);
        this.in = in;
        this.out = out;

        this.ins = new double[in];
        this.outs = new double[out];

        this.mid = new double[n];
        //double[] sigma = new double[n];
        this.min = min;
        this.max = new double[n];
        this.inc = new double[n];
        this.range = new double[n];

        for (int i = 0; i < n; i++) {
            mid[i] = (max[i] + min[i]) / 2;
            range[i] = max[i] - min[i];
        }

        this.opt = new ParallelCMAESOptimizer(Integer.MAX_VALUE,
                0, true, 0,
                0, new MersenneTwister(3),
                true, null);

        int maxIterations = Integer.MAX_VALUE;
        optProcess = this.opt.start(
                new MaxEval(maxIterations),
                new ObjectiveFunction(this),
                GoalType.MAXIMIZE,
                new SimpleBounds(min, max),
                new InitialGuess(mid),
                new ParallelCMAESOptimizer.Sigma(MathArrays.scale(1f, inc)),
                new ParallelCMAESOptimizer.PopulationSize(population));

    }

    public int act(float reward, double[] nextObservation) {

        this.reward = reward;

        if (nextObservation==null)
            return -1;

        int n = nextObservation.length;
        arraycopy(nextObservation, 0, ins, 0, n);


        //System.out.println(n4(ins));

        optProcess.next();

        //3. interpret the searched action vector
        return lastAction = decider.decide(Util.doubleToFloatArray(this.outs), lastAction);
    }


    @Override
    public double value(double[] point) {
        this.search = point;

        //replace any non-finite values with random
        for (int i = 0; i < point.length; i++) {
            if (!Double.isFinite(point[i]))
                point[i] = (rng.nextFloat() + min[i])  * (max[i] - min[i]);
        }

        //intercept the CMAES vector:

        //System.out.println("pre: " + n4(point));
        //  1) replace the input information from search with current observation
        if (ins!=null) {
            for (int i = 0; i < in; i++) {
                //apply random noise, for at least avoid convergence
                point[i] = ins[i] + (noise > 0 ? ((rng.nextFloat() * noise) - 0.5f) * 2f : 0);
            }
        }

        //  2) copy the output information from search vector (for action determination)
        arraycopy(point, in, outs, 0, out);

        //System.out.println(" outs: " + n4(outs));
        //System.out.println("  ins: " + n4(ins));


        //System.out.println("post: " + n4(point));

        if (reward != reward)
            reward = 0; //assume 0, to filter any NaN

        return reward;
    }
}
