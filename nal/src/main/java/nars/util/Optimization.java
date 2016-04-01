package nars.util;

import com.google.common.base.Joiner;
import com.google.common.primitives.Doubles;
import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Optimization solver wrapper w/ lambdas
 */
public class Optimization<X> {
    public final Supplier<X> subject;

    final List<Tweak> tweaks = new ArrayList();

    public Optimization(Supplier<X> subject) {
        this.subject = subject;
    }

//        public Optimization<X> with(FloatParameterSweep<X> f) {
//            this.tweaks.add(f);
//            return this;
//        }

    public Optimization<X> with(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
        tweaks.add(new FloatParameterSweep(parameter, min, max, inc, apply));
        return this;
    }

    public Result<X> run(FloatFunction<X> eval) {
        double stopValue = Double.POSITIVE_INFINITY;
        int maxIterations = 30000;
        CMAESOptimizer optim = new CMAESOptimizer(maxIterations, stopValue, true, 0,
                0, new MersenneTwister(3), true, null);

        int i = 0;
        int n = tweaks.size();

        double[] start = new double[n];
        double[] sigma = new double[n];
        double[] min = new double[n];
        double[] max = new double[n];

        for (Tweak w : tweaks) {
            //w.apply.value((float) point[i++], x);
            FloatParameterSweep s = (FloatParameterSweep) w;
            start[i] = (s.getMax() + s.getMin()) / 2f;
            min[i] = (s.getMin());
            max[i] = (s.getMax());
            sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }

        int maxEvaluations = 1000;
        int pop = 64;

        System.out.println(Joiner.on(",").join(tweaks) + ",       Loss");

        PointValuePair r = optim.optimize(new MaxEval(maxEvaluations),
                new ObjectiveFunction(point -> {
                    X x = subject.get();
                    int i1 = 0;
                    for (Tweak w : tweaks) {
                        w.apply.value((float) point[i1++], x);
                    }
                    float loss = eval.floatValueOf(x);
                    //System.out.println(Arrays.toString(point) + " = " + loss);
                    System.out.println(Joiner.on(",").join(Doubles.asList(point)) + ",      " + loss);
                    return loss;
                }),
                GoalType.MINIMIZE,
                new SimpleBounds(min, max),
                new InitialGuess(start),
                new CMAESOptimizer.Sigma(sigma),
                new CMAESOptimizer.PopulationSize(pop));


        return new Result(r);

    }

    public class Result<X> {

        private final PointValuePair optimal;

        public Result(PointValuePair p) {
            this.optimal = p;
        }

        public void print() {
            System.out.println("optimal: " + optimal);
            double[] p = optimal.getPoint();
            for (int i = 0; i < p.length; i++) {
                System.out.println(tweaks.get(i).id + " " + p[i]);
            }

        }
    }

    /**
     * a knob but cooler
     */
    public class Tweak {
        public final FloatObjectProcedure<X> apply;
        private final String id;

        public Tweak(String id, FloatObjectProcedure<X> apply) {
            this.id = id;
            this.apply = apply;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private class FloatParameterSweep extends Tweak {
        private final String parameter;
        private final float min;
        private final float max;
        private final float inc;

        private FloatParameterSweep(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
            super(parameter, apply);
            this.parameter = parameter;
            this.min = min;
            this.max = max;
            this.inc = inc;
        }

        public String getParameter() {
            return parameter;
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }

        public float getInc() {
            return inc;
        }


    }
}
