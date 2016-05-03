package nars.op.java;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.TreeSet;

import static java.lang.System.out;
import static nars.util.Texts.n2;

/**
 * this is trying to guess how to react to a hidden variable, its only given clues when its above or below
 * and its goal is to avoid both those states
 * anything below a score of 0.5 should be better than random
 * it gets these above/below hints but it has to process these among all the other processing its thinking about
 * then to really guess right it has to learn the timing of the sine wave
 * and imagine at what rate it will travel and when it will change direction etc
 */
public class Thermostat6s {


    public static final float tolerance = 0.03f;
    public static float targetPeriod = 160;
    public static final float speed = 0.05f;
    static boolean print = true;

    static int sensorPeriod = 4; //frames per max sensor silence

    public static void main(String[] args) {
        Default d = new Default(1024, 3, 2, 3);

        d.cyclesPerFrame.set(8);
        d.conceptActivation.setValue(0.25f);
        //d.conceptBeliefsMax.set(32);
        //d.shortTermMemoryHistory.set(3);
        //d.derivationDurabilityThreshold.setValue(0.03f);
        //d.perfection.setValue(0.9f);
        //d.premiser.confMin.setValue(0.02f);

        float score = eval(d, 12000);
        System.out.println("score=" + score);
    }


    public static float eval(NAR n, int cycles) {

        final MutableInteger t = new MutableInteger();


        Global.DEBUG = true;


        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        SensorConcept above, below;


        FloatToObjectFunction sensorTruth = (v) -> {
            return new DefaultTruth(v, 0.9f);
        };
        above = new SensorConcept("(above)", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > -tolerance) return 0;
            else return Util.clamp( -diff/2f  + 0.5f);
            //else return Util.clamp( -diff  + 0.5f);
            //else return 1f;
        }, sensorTruth).resolution(0.01f).timing(-1, -1);

        below = new SensorConcept("(below)", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff < tolerance) return 0;
            else return diff; //Util.clamp( diff /2f + 0.5f);
            //else return Util.clamp( diff + 0.5f);
            //else return 1f;
        }, sensorTruth).resolution(0.01f).timing(-1, -1);

        MotorConcept up = new Motor1D(n, true, yEst);
        MotorConcept down = new Motor1D(n, false, yEst);


        n.onFrame(nn -> {


            float estimated = yEst.floatValue();

            int tt = t.intValue();
            float actual;

            yHidden.setValue(0.5f + 0.5f * Math.sin(tt / (targetPeriod)));
            actual = yHidden.floatValue();

            loss.add(Math.abs(actual - estimated));


            if (print) {

                int cols = 50;
                int colActual = Math.round(cols * actual);
                int colEst = Math.round(cols * estimated);
                for (int i = 0; i <= cols; i++) {

                    char c;
                    if (i == colActual)
                        c = '#';
                    else if (i == colEst)
                        c = '|';
                    else
                        c = '.';

                    out.print(c);
                }

                out.print(" \t<-" + n2(below.get()) +"|" +n2(above.get()) + "-> " +
                        " \t-" + n2(down.expectation(n)) +
                        " \t+" + n2(up.expectation(n))
                );
                out.println();
            }
        });


        yEst.setValue(0.5f);



        mission(n);


        for (int i = 0; i < cycles; i++) {

            n.step();


            t.add(1); //cause delays in the sine wave

        }

        printBeliefs(n, true);
        printBeliefs(n, false);

        return loss.floatValue() / t.intValue();

    }


    public static void mission(NAR n) {


        //n.goal($.$("(up)"), Tense.Present, 0f, 0.25f);
        //n.goal($.$("(down)"), Tense.Present, 0f, 0.25f);



        //EXTREME CHEATS: "if i am up i should go down"
//        n.input("((above) ==>+0 (down))! %0.90;1.00%");
//        n.input("((below) ==>+0 (up))! %0.90;1.00%");
//        n.input("((above) ==>+0 (--,(up)))! %0.90;1.00%");
//        n.input("((below) ==>+0 (--,(down)))! %0.90;1.00%");

        float f = 0.85f;
//        n.goal("((above) ==>+0 (down))", Tense.Eternal, f, 1f);
//        n.goal("((below) ==>+0 (up))", Tense.Eternal, f, 1f);
//        n.goal("((above) ==>+0 (--,(up)))", Tense.Eternal, f, 1f);
//        n.goal("((below) ==>+0 (--,(down)))", Tense.Eternal, f, 1f);

        n.goal("((above) &&+1 (down))", Tense.Eternal, f, 1f);
        n.goal("((below) &&+1 (up))", Tense.Eternal, f, 1f);
        n.goal("((above) &&+1 (--,(up)))", Tense.Eternal, f, 1f);
        n.goal("((below) &&+1 (--,(down)))", Tense.Eternal, f, 1f);

        //n.goal("(above)", Tense.Eternal, 0.25f, 1f); //not above nor below
        //n.goal("(below)", Tense.Eternal, 0.25f, 1f); //not above nor below

        //MODERATE CHEATS: "being up leads to me going down"
//        n.input("((above) ==>+0 (down)).");
//        n.input("((below) ==>+0 (up)).");
        //n.input("((above) ==> (down)). :|:");
        //n.input("((below) ==> (up)). :|:");
        //n.input("<(above) ==> (--,(up))>. :|:");
        //n.input("<(below) ==> (--,(down))>. :|:");

        //n.input("(up)@");
        //n.input("(down)@");


        //n.goal($.$("(up)"), Tense.Present, 1f, 0.75f);
        //n.goal($.$("(down)"), Tense.Present, 1f, 0.75f);



        //n.goal($.$("((above) && (below))"), Tense.Eternal, 0f, 0.99f); //neither above or below
        //n.goal($.$("((above) || (below))"), Tense.Eternal, 0f, 0.99f); //not above nor below

    }


    public static void printBeliefs(NAR n, boolean beliefsOrGoals) {
        TreeSet<Task> bt = new TreeSet<>((a, b) -> { return a.term().toString().compareTo(b.term().toString()); });
        n.forEachConcept(c -> {
            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();
            if (!table.isEmpty()) {
                bt.add(table.top(n.time()));
                //System.out.println("\t" + c.beliefs().top(n.time()));
            }
        });
        bt.forEach(xt -> {
            System.out.println(xt);
        });
    }

    private static class Motor1D extends MotorConcept implements MotorConcept.MotorFunction {

        final boolean up;
        private final MutableFloat yEst;

        public Motor1D(NAR n, boolean up, MutableFloat yEst) throws Narsese.NarseseException {
            //super((up ? "up" : "down") + "()", n, null);
            super("(" + (up ? "up" : "down") + ")", n, null);
            this.up = up;
            this.yEst = yEst;

            setMotor(this);
        }

        @Override
        protected int capacity(int cap, boolean beliefOrGoal, boolean eternalOrTemporal) {
            return eternalOrTemporal ? 0 : cap; //no eternal
        }

        @Override
        public float motor(float b, float d) {

            //System.out.println(this + " " + b + " " + d + " " + );

            if (d < 0.55) return Float.NaN;
            if (d < b) return Float.NaN;
            //if (d < 0.5f && b < d) return Float.NaN;

            float current = Util.clamp(yEst.floatValue());
            float delta = speed * (up ? 1 : -1) * (d - b);
            float next = Util.clamp(delta + current);
            yEst.setValue(next);

            return (d-b); //0.5f + desired/2f;


        }

    }
}
