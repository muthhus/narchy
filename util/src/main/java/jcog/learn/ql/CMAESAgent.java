package jcog.learn.ql;

import jcog.Util;
import jcog.decide.DecideSoftmax;
import jcog.math.MyCMAESOptimizer;
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

import static java.lang.System.arraycopy;

public class CMAESAgent implements MultivariateFunction /*implements Agent*/ {

    private final double[] mid, min, max, range;
    private final MyCMAESOptimizer.CMAESProcess proc;

    /**
     * # of dimensions holding the input
     */
    private final int in;

    /**
     * # of dimensions holding the output
     */
    private final int out;

    private MyCMAESOptimizer opt;
    private int population = 64;
    private double[] ins;
    private float reward;
    private double[] search;

    private double[] outsNext;
    public double[] outs;
//    final DecideSoftmax decider = new DecideSoftmax(0.25f, 0.25f, 0.5f, new XorShift128PlusRandom(1));
//    private int lastAction;

    final Random rng = new XorShift128PlusRandom(1);
    double noise = 0.0f;

    /**
     * assumes 0..1.0 range
     */
    public static CMAESAgent build(int in, int out) {
        int dim = in + out;
        double[] min = new double[dim];
        double[] max = new double[dim];
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
        this.outsNext = new double[out];

        this.mid = new double[n];
        this.min = min;
        this.max = max;
        this.range = new double[n];

        for (int i = 0; i < n; i++) {
            mid[i] = (max[i] + min[i]) / 2;
            range[i] = max[i] - min[i];
        }

        this.opt = new MyCMAESOptimizer(Integer.MAX_VALUE,
                0, true, 0,
                0, new MersenneTwister(3),
                false, null);

        int maxIterations = Integer.MAX_VALUE;
        proc = this.opt.start(
                new MaxEval(maxIterations),
                new ObjectiveFunction(this),
                GoalType.MAXIMIZE,
                new SimpleBounds(min, max),
                new InitialGuess(mid),
                new MyCMAESOptimizer.Sigma(MathArrays.scale(0.01f, range)),
                new MyCMAESOptimizer.PopulationSize(population));
        proc.pre();
    }

    final static MyCMAESOptimizer.ValuePenaltyPair notChosenResult = new MyCMAESOptimizer.ValuePenaltyPair(0, 0f);

    public void act(float reward, double[] nextObservation) {

        this.reward = reward;

        if (nextObservation == null)
            return;



        int s = proc.size();
        int best = -1;
        double minDist = Double.POSITIVE_INFINITY;
        //find best match for observation vector
        for (int i = 0; i < s; i++) {
            double[] pi = proc.next(i);
            double dist = dist(nextObservation, pi, in, minDist);
            if (dist < minDist) {
                best = i;
                minDist = dist;
            }
        }

        MyCMAESOptimizer.ValuePenaltyPair result = proc.evalActual(best); //should call value(point) below



        for (int i = 0; i < s; i++)
            proc.valuePenaltyPairs[i] = (i==best) ? result : notChosenResult; //set all to the result

        proc.post(); //commit

        int n = nextObservation.length;
        arraycopy(nextObservation, 0, ins, 0, n);


        //System.out.println(n4(ins));

        proc.next();


        //3. interpret the searched action vector
        //return lastAction = decider.decide(Util.doubleToFloatArray(this.outs), lastAction);
    }


    private double dist(double[] a, double[] b, int prefixDimensions, double minDist) {
        //hamming distance
        double t = 0;
        for (int i = 0; i < prefixDimensions; i++) {
            t += Math.abs(a[i] - b[i]);
            if (t > minDist)
                return Double.POSITIVE_INFINITY;
        }
        return t;
    }


    @Override
    public double value(double[] point) {
        this.search = point;


        //intercept the CMAES vector:

        //System.out.println("pre: " + n4(point));

        if (ins != null) {
            for (int i = 0; i < in; i++) {
                //apply random noise, for at least avoid convergence
                point[i] =
                        ins[i] + (noise > 0 ? (((rng.nextFloat()) - 0.5f) * 2f * noise) : 0);
            }
        }

        //replace any non-finite values with random
        for (int i = 0; i < point.length; i++) {
            if (!Double.isFinite(point[i]))
                point[i] = (rng.nextFloat() + min[i]) * (max[i] - min[i]);

            point[i] =
                    //Util.sigmoid
                    Util.unitize(
                            point[i]
                    );
        }

        //  2) copy the output information from search vector (for action determination)
        arraycopy(point, in, outsNext, 0, out);

        //  3) replace the action with the last actual action
        arraycopy(outs, 0, point, in, out);

        //  4) save the next action as the current action
        arraycopy(outsNext, 0, outs, 0, out);

        //System.out.println(" outs: " + n4(outs));
        //System.out.println("  ins: " + n4(ins));


        //System.out.println("post: " + n4(point));

        if (reward != reward)
            reward = 0; //assume 0, to filter any NaN

        return reward;
    }
}
