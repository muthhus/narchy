package nars.op.java;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.predicate.primitive.FloatPredicate;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.util.Optimization;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.lang.System.out;


public class Thermostat5 {

    public static final float basePeriod = 32;
    public static final float tolerance = 0.15f;
    public static float targetPeriod = 2f;
    public static final float speed = 0.01f;
    static boolean print = true, debugError = false;

    public static void main(String[] args) {
        Default d = new Default(1024, 32, 2, 3);
        d.duration.set(Math.round(2.5f * basePeriod));
        d.activationRate.setValue(0.05f);
        d.premiser.confMin.setValue(0.1f);

        float score = eval(d, 15000);
        System.out.println("score=" + score);
    }
    public static void main2(String[] args) {
        int cycles = 2000;

        new Optimization<Default>(() -> {
            Default d = new Default(1024, 5, 2, 4);
            //d.perfection.setValue(0.1);
            d.premiser.confMin.setValue(0.1f);
            d.duration.set(Math.round(1.5f * basePeriod));
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

    static int t = 0;

    public static float eval(NAR n, int cycles) {


        Global.DEBUG = true;

        //MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        SensorConcept aboveness, belowness;


        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
        //        .resolution(0.01f)/*.pri(0.2f)*/
        //);
        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
                .resolution(0.01f).pri(0.2f)
        );*/


        n.on(aboveness = new SensorConcept("diffx:above", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.05f));

        n.on(belowness = new SensorConcept("diffy:below", n, () -> {
            float diff = -(yHidden.floatValue() - yEst.floatValue());
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.05f));


        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            double y = 0.5f + 0.5f * Math.sin(t / (targetPeriod * basePeriod));
            //y = Math.round(y);

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

                out.print(" <:" + belowness.get() + " >:" + aboveness.get());
                out.println();
            }
        });

        /** difference in order to diagnose an error */
        final float errorThresh = 0.07f;

        n.on(new DebugMotorConcept(n, "t(up)", yEst, yHidden,
                (v) -> {
                    yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
                    return v;
                },
                (v) -> {
                    //if already above the target value
                    return yHidden.floatValue() - yEst.floatValue() > errorThresh;
                }
        ));
        n.on(new DebugMotorConcept(n, "t(down)", yEst, yHidden,
                (v) -> {
                    yEst.setValue(Util.clamp(-speed * v + yEst.floatValue()));
                    return v;
                },
                (v) -> {
                    //if already above the target value
                    return -(yHidden.floatValue() - yEst.floatValue()) > errorThresh;
                }
        ));



        //n.logSummaryGT(System.out, 0.0f);

        t = 0;

        int trainMotionCycles = 32;
        float str = 0.1f;
        System.out.println("training up");

        n.goal($.$("t(up)"), Tense.Present, 1f, str);
        n.goal($.$("t(down)"), Tense.Present, 0f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }
        System.out.println("training down");
        n.goal($.$("t(up)"), Tense.Present, 0f, str);
        n.goal($.$("t(down)"), Tense.Present, 1f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }
        System.out.println("training oscillation");
        n.goal($.$("t(up)"), Tense.Present, 0.75f, str);
        n.goal($.$("t(down)"), Tense.Present, 0.75f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }

        System.out.println("training finished");

        //n.goal($.$("t(up)"), Tense.Present, 0f, 0.1f);
        //n.goal($.$("t(down)"), Tense.Present, 1f, 0.1f);
        n.goal($.$("diffx:above"), 0f, 0.99f); //not above
        n.goal($.$("diffy:below"), 0f, 0.99f); //not below
        //n.ask($.$("(a:#x ==> diff:#y)"), '?'); //not above

        for (int i = 0; i < cycles; i++) {
            t++;
            //n.goal($.$("((--,diff:above) && (--,diff:below))"), Tense.Present, 1f, 0.99f); //not above or below

            n.step();
            //Util.pause(1);
        }

        return loss.floatValue()/n.time();

    }

    private static class DebugMotorConcept extends MotorConcept {



        long lastTime;

        /** tasks collected from last cycle in which goals were received */
        final List<Task> current = Global.newArrayList();

        public DebugMotorConcept(NAR n, String term, MutableFloat yEst, MutableFloat yHidden, FloatToFloatFunction motor, FloatPredicate errorful) throws Narsese.NarseseException {
            super(term, n);
            setMotor( (v) -> {
                float next = motor.valueOf(v);
                if (debugError) {
                    if (errorful.accept(v)) {
                        for (Task t : current) {
                            if (!t.isInput())
                                System.err.println(t.explanation());
                        }
                    }
                }
                return next;
            });
            lastTime = -1;
        }

        @Nullable
        @Override
        public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
            long now = nar.time();
            if (now !=lastTime) {
                current.clear();
            }
            Task g = super.processGoal(goal, nar);
            if (g!=null)
                current.add(g);
            return g;
        }
    }
}
