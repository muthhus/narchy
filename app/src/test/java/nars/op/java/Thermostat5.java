package nars.op.java;

import com.google.common.base.Joiner;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Primitives;
import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatObjectToFloatFunction;
import com.gs.collections.api.block.procedure.primitive.FloatObjectProcedure;
import nars.$;
import nars.NAR;
import nars.nal.Tense;
import nars.nar.Default;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;

import static java.lang.System.out;


public class Thermostat5 {

    public static final float basePeriod = 8;
    public static final float tolerance = 0.15f;
    public static float targetPeriod = 1f;
    public static final float speed = 0.1f;

    public static class Optimization<X> {
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

            System.out.println( Joiner.on(",").join(tweaks) + ",       Loss");

            PointValuePair r = optim.optimize(new MaxEval(maxEvaluations),
                    new ObjectiveFunction(point -> {
                        X x = subject.get();
                        int i1 = 0;
                        for (Tweak w : tweaks) {
                            w.apply.value((float) point[i1++], x);
                        }
                        float loss = eval.floatValueOf(x);
                        //System.out.println(Arrays.toString(point) + " = " + loss);
                        System.out.println( Joiner.on(",").join(Doubles.asList(point)) + ",      " + loss);
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

    public static void main(String[] args) {

        int cycles = 2000;

        new Optimization<Default>(() -> {
            Default d = new Default(1024, 5, 2, 4);
            d.perfection.setValue(0.1);
            d.premiser.confMin.setValue(0.1f);
            d.duration.set(Math.round(2.5f * basePeriod));
            d.core.conceptsFiredPerCycle.set(5);
            return d;
        })
                .with("activationRate", 0.1f, 0.3f, 0.1f, (a, x) -> {
                    x.activationRate.setValue(a);
                })
                .with("conceptDurations", 0.1f, 5f, 0.1f, (a, x) -> {
                    x.conceptForgetDurations.setValue(a);
                })
                .with("termLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
                    x.termLinkForgetDurations.setValue(a);
                })
                .with("taskLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
                    x.taskLinkForgetDurations.setValue(a);
                })
                /*.with("confMin", 0.05f, 0.3f, 0.01f, (a, x) -> {
                    x.premiser.confMin.setValue(a);
                })*/
                /*.with("durationFactor", 0.25f, 3.5f, 0.1f, (dFactor, x) -> {
                    x.duration.set(Math.round(basePeriod * dFactor));
                })*/
                /*.with("conceptsPercycle", 2, 5, 1, (c, x) -> {
                    x.core.conceptsFiredPerCycle.set((int)c);
                })*/
                .run(x -> eval(x, cycles)).print();


        //n.cyclesPerFrame.set(10);
        //n.derivationDurabilityThreshold.setValue(0.02f);
        //n.premiser.confMin.setValue(0.05f);

        //System.out.println(eval(n, 1000));
    }

    public static float eval(NAR n, int cycles) {

        boolean print = true;

        //MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            double y = 0.5f + 0.5f * Math.sin(n.time() / (targetPeriod * basePeriod));
            y = Math.round(y);

            //x0.setValue(y); //high frequency phase
            //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase

            //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
            yHidden.setValue(y);

            float actual = yHidden.floatValue();
            float estimated = yEst.floatValue();
            //out.println( actual + "," + estimated );

            loss.add(Math.abs(actual - estimated));

            if (print) {

                int cols = 50;
                int colActual = (int) Math.round(cols * actual);
                int colEst = (int) Math.round(cols * estimated);
                for (int i = 0; i <= cols; i++) {

                    char c;
                    if (i == colActual)
                        c = '*';
                    else if (i == colEst)
                        c = '|';
                    else
                        c = '.';

                    out.print(c);
                }

                out.println();
            }
        });
        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
        //        .resolution(0.01f)/*.pri(0.2f)*/
        //);
        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
                .resolution(0.01f).pri(0.2f)
        );*/
        n.on(new SensorConcept("diff:above", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.03f).pri(0.5f));
        n.on(new SensorConcept("diff:below", n, () -> {
            float diff = -(yHidden.floatValue() - yEst.floatValue());
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.03f).pri(0.5f));

        n.on(new MotorConcept("t(up)", n, (v) -> {
            yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
            return v;
        }));
        n.on(new MotorConcept("t(down)", n, (v) -> {
            yEst.setValue(Util.clamp(-speed * v + yEst.floatValue()));
            return v;
        }));


        //n.logSummaryGT(System.out, 0.6f);
        n.goal($.$("t(up)"), 1f, 0.25f);
        n.goal($.$("t(up)"), 0f, 0.25f);
        n.goal($.$("t(down)"), 1f, 0.25f);
        n.goal($.$("t(down)"), 0f, 0.25f);
        n.goal($.$("diff:above"), 0f, 0.99f); //not above
        n.goal($.$("diff:below"), 0f, 0.99f); //not below
        //n.ask($.$("(a:#x ==> diff:#y)"), '?'); //not above

        for (int i = 0; i < cycles; i++) {

            //n.goal($.$("((--,diff:above) && (--,diff:below))"), Tense.Present, 1f, 0.99f); //not above or below

            n.step();
            //Util.pause(1);
        }

        return loss.floatValue()/n.time();

    }
}
