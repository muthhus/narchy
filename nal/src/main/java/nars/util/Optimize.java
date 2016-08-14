package nars.util;

import com.google.common.base.Joiner;
import com.google.common.primitives.Doubles;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Optimization solver wrapper w/ lambdas
 */
public class Optimize<X> {
    public final Supplier<X> subject;

    final List<Tweak> tweaks = new ArrayList();
    private final boolean trace = true;

    public Optimize(Supplier<X> subject) {
        this.subject = subject;
    }

//        public Optimization<X> with(FloatParameterSweep<X> f) {
//            this.tweaks.add(f);
//            return this;
//        }

    public @NotNull Optimize<X> with(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
        tweaks.add(new FloatRange(parameter, min, max, inc, apply));
        return this;
    }
    public @NotNull Optimize<X> call(String parameter, float min, float max, float inc, @NotNull String invoker) {
        Map m = new HashMap(4);

        tweaks.add(new FloatRange(parameter, min, max, inc, (v,x) -> {
            //accessible by both these:
            m.put(parameter, v);
            m.put("x", v);

            //integer rounded:
            m.put("i", Math.round(v));

            try {
                Ognl.getValue(invoker, m, x);
            } catch (OgnlException e) {
                e.printStackTrace();
            }
        }));
        return this;
    }

    @NotNull
    public Result run(int maxIterations, @NotNull FloatFunction<X> eval) {



        int i = 0;
        int n = tweaks.size();

        double[] mid = new double[n];
        //double[] sigma = new double[n];
        double[] min = new double[n];
        double[] max = new double[n];
        double[] inc = new double[n];

        for (Tweak w : tweaks) {
            //w.apply.value((float) point[i++], x);
            FloatRange s = (FloatRange) w;
            mid[i] = (s.getMax() + s.getMin()) / 2f;
            min[i] = (s.getMin());
            max[i] = (s.getMax());
            inc[i] = s.getInc();
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }



        System.out.println(Joiner.on(",").join(tweaks) + ",\tScore");

        ObjectiveFunction func = new ObjectiveFunction(point -> {
            X x = subject.get();
            int i1 = 0;
            for (Tweak w : tweaks) {
                w.apply.value((float) point[i1++], x);
            }

            float score;
            try {
                score = eval.floatValueOf(x);
            } catch (Exception e) {
                e.printStackTrace();
                score = Float.NEGATIVE_INFINITY;
            }

            //System.out.println(Arrays.toString(point) + " = " + loss);
            if (trace)
                System.out.println(Joiner.on(",").join(Doubles.asList(point)) + ",\t" + score);
            return score;
        });

        //int dim = mid.length;

//        CMAESOptimizer optim = new CMAESOptimizer(cmaesIter, stopValue, true, 0,
//                0, new MersenneTwister(3), true, null);
//        PointValuePair r = optim.optimize(new MaxEval(maxIterations),
//                func,
//                GoalType.MAXIMIZE,
//                new SimpleBounds(min, max),
//                new InitialGuess(start),
//                new CMAESOptimizer.Sigma(sigma),
//                new CMAESOptimizer.PopulationSize(pop));


//        final int numIterpolationPoints = 3 * dim; //2 * dim + 1 + 1;
//        PointValuePair r = new BOBYQAOptimizer(numIterpolationPoints, dim * 2.0, 1.0E-8D)
//                    .optimize(new MaxEval(maxIterations),
//                        func,
//                        GoalType.MAXIMIZE,
//                        new SimpleBounds(min, max),
//                        new InitialGuess(start));

        PointValuePair r = new SimplexOptimizer(1e-10, 1e-30).optimize(
                new MaxEval(maxIterations),
                func,
                GoalType.MAXIMIZE,
                new InitialGuess(mid),
                //new NelderMeadSimplex(inc)
                new MultiDirectionalSimplex(inc)
        );

        return new Result(r);

    }

    public class Result {

        private final PointValuePair optimal;

        public Result(PointValuePair p) {
            this.optimal = p;
        }

        public void print() {
            //System.out.println("optimal: " + optimal);
            System.out.println("score=" + optimal.getSecond());
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

    private class FloatRange extends Tweak {
        private final String parameter;
        private final float min;
        private final float max;
        private final float inc;

        private FloatRange(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
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
