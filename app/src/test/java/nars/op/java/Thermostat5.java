package nars.op.java;

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.System.out;


/**
 * Created by me on 3/31/16.
 */
public class Thermostat5 {

    public static final float basePeriod = 35;
    public static float targetPeriod = 1f;
    public static final float speed = 0.1f;

    public static class Optimization<X> {
        public final Supplier<X> subject;

        final List<Tweak> tweaks = new ArrayList();

        public Optimization(Supplier<X> subject) {
            this.subject = subject;
        }

        public Optimization<X> with(FloatParameterSweep f) {
            this.tweaks.add(f);
            return this;
        }

        public Optimization<X> with(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
            return with(new FloatParameterSweep(parameter, min ,max, inc, apply));
        }

        public Result<X> run(FloatFunction<X> eval) {

            //apply
            float score = eval.floatValueOf(subject.get());

            return new Result();

        }

        public static class Result<X> {

            public void print() {

            }
        }

        /** a knob but cooler */
        public static class Tweak {

        }

        private class FloatParameterSweep extends Tweak {
            private final String parameter;
            private final float min;
            private final float max;
            private final float inc;
            private final FloatObjectProcedure<X> apply;

            private FloatParameterSweep(String parameter, float min, float max, float inc, FloatObjectProcedure<X> apply) {
                this.parameter = parameter;
                this.min = min;
                this.max = max;
                this.inc = inc;
                this.apply = apply;
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

        int cycles = 1000;

        new Optimization<Default>(()->new Default(1000, 5, 2,6))
            .with("activationRate", 0.1f, 1.0f, 0.1f, (a, x) -> {
                x.activationRate.setValue(a);
            })
            .with("durationFactor", 0.5f, 2.5f, 0.1f, (dFactor, x) -> {
                x.duration.set((int) (basePeriod * dFactor));
            }).run(x -> eval(x, cycles)).print();

        Default n = new Default(1000, 5, 2,6);

        //n.cyclesPerFrame.set(10);
        //n.derivationDurabilityThreshold.setValue(0.02f);
        //n.premiser.confMin.setValue(0.05f);

        //System.out.println(eval(n, 1000));
    }

    public  static float eval(NAR n, int cycles) {

        boolean print = false;

        MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            x0.setValue( 0.5f + 0.5f * Math.sin(n.time()/(targetPeriod * basePeriod)) ); //high frequency phase
            //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase

            //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
            yHidden.setValue(x0);

            float actual = yHidden.floatValue();
            float estimated = yEst.floatValue();
            //out.println( actual + "," + estimated );

            loss.add( Math.abs(actual - estimated) );

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
        n.on(new SensorConcept("diff:above", n, ()-> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > 0) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f)/*.pri(0.2f)*/);
        n.on(new SensorConcept("diff:below", n, ()-> {
            float diff = -(yHidden.floatValue() - yEst.floatValue());
            if (diff > 0) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f)/*.pri(0.2f)*/);

        n.on(new MotorConcept("t(up)", n, (v)->{
            yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
            return v;
        }));
        n.on(new MotorConcept("t(down)", n, (v)->{
            yEst.setValue(Util.clamp(-speed* v + yEst.floatValue()));
            return v;
        }));




        //n.logSummaryGT(System.out, 0.6f);
        n.goal($.$("t(up)"),  Tense.Present, 1f, 0.1f);
        n.goal($.$("t(up)"), Tense.Present, 0f, 0.1f);
        n.goal($.$("t(down)"), Tense.Present, 1f, 0.1f);
        n.goal($.$("t(down)"), Tense.Present, 0f, 0.1f);
        n.goal($.$("diff:above"), 0f, 0.99f); //not above
        n.goal($.$("diff:below"), 0f, 0.99f); //not below
        //n.ask($.$("(a:#x ==> diff:#y)"), '?'); //not above

        for (int i = 0; i < cycles; i++) {

            //n.goal($.$("((--,diff:above) && (--,diff:below))"), Tense.Present, 1f, 0.99f); //not above or below

            n.step();
            //Util.pause(1);
        }

        return loss.floatValue();

    }
}
