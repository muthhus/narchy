package nars.op.java;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.util.HaiQ;
import nars.util.Hsom;
import nars.util.Optimization;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Arrays;
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
public class Thermostat6Hai {

    public static final float basePeriod = 12;
    public static float targetPeriod = 8;
    public static final float speed = 0.05f;
    static boolean print = true;


    public static void main(String[] args) {
        eval(120000);
    }

    public static void eval(int cycles) {

        int resolution = 12;

        int inputs = resolution * 2;
        int states = resolution;

        Hsom som = new Hsom(inputs, states, new XorShift128PlusRandom(1));
        HaiQ h = new HaiQ(states*states, 3) {
            
            @Override
            protected int perceive(float[] input) {
        		som.learn(input);
		        return som.winnerx + (som.winnery * states);
            }
        };

        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        float[] ins = new float[inputs];

        for (int tt = 0; tt < cycles; tt++) {

            int a = Math.round(yHidden.floatValue() * (resolution-1));
            int b = Math.round(yEst.floatValue() * (resolution-1));

            for (int i = 0; i < resolution; i++) {
                ins[i] = (i == a) ?  1 : -1;
                ins[i+resolution] = (i == b) ? 1 : -1;
            }

            //System.out.println(Arrays.toString(ins).replace((char)0, '0').replace((char)1, '1'));
            System.out.print(som.winnerx + "," + som.winnery);

            float reward = -1f + 1f / (1f + Math.abs(yHidden.floatValue() - yEst.floatValue()));
            int aa = h.act(ins, reward);
            float de = 0;
            switch (aa) {
                case 1:
                    de = 1f * speed;
                    break;
                case 2:
                    de = -1f * speed;
                    break;
                case 0:
                default:
                    de = 0f; //nothing
                    break;
            }
            yEst.setValue( Util.clamp(yEst.floatValue() + de) );

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            float estimated = yEst.floatValue();


            float actual;
            if (tt > 0) {

                //double y = 0.5f + 0.45f * Math.sin(tt / (targetPeriod * basePeriod));

                //float nnn = 3; //steps
                //double y = 0.5f + 0.5f * Math.round(nnn * Math.sin(tt / (targetPeriod * basePeriod)))/nnn;

                double y = 0.5f + 0.5f * Math.sin(tt / (targetPeriod * basePeriod));
                //y = y > 0.5f ? 0.95f : 0.05f;

                //x0.setValue(y); //high frequency phase
                //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase

                //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
                yHidden.setValue(y);

                actual = yHidden.floatValue();
                //out.println( actual + "," + estimated );

                loss.add(Math.abs(actual - estimated));
            } else {
                actual = 0.5f;
            }

            if (print) {

                int cols = 50;
                int colActual = (int) Math.round(cols * actual);
                int colEst = (int) Math.round(cols * estimated);
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


                out.println();
            }
        }

        //n.logSummaryGT(System.out, 0.0f);

        //float str = 0.75f;

        //n.log();


//        int trainingRounds = 2;
//        for (int i = 0; i < trainingRounds; i++) {
//            float dd = 0.2f * trainingRounds; //(trainingRounds-1-i);
//
//            System.out.println("training up");
//            yEst.setValue(0.5f -dd);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal($.$("(up)"), Tense.Present, 1f, str);
//                n.goal($.$("(down)"), Tense.Present, 0f, str);
//                n.step();
//            } while (above.get() < 0.6f);
//
//            System.out.println("training down");
//            yEst.setValue(0.5f + dd);
//            //n.goal($.$("(up)"), Tense.Present, 0f, str);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal($.$("(up)"), Tense.Present, 0f, str);
//                n.goal($.$("(down)"), Tense.Present, 1f, str);
//                n.step();
//                //System.out.println(diffness.get());
//            } while (below.get() < 0.6f);
//        }
//
//        System.out.println("training finished");
//
//        yEst.setValue(0.5f);
//
//        for (int i = 0; i < cycles; i++) {
//
//            n.step();
//
//
//            t.add(1); //cause delays in the sine wave
//
//
//
//            if (i % commandPeriod == 0) {
//                command(n);
//            }
//        }
//
//        printBeliefs(n, true);
//        printBeliefs(n, false);

        //return loss.floatValue() / t.intValue();

    }

    public static @org.jetbrains.annotations.NotNull SensorConcept vSensor(NAR n, float dv, int ii, MutableFloat zz, String cname) {
        return new SensorConcept(cname, n, () -> {
            float low = ii * dv;
            float high = ii * dv;
            float v = zz.floatValue();
            if ((v >= low && v <= high)) {
                return 1f;
            } else {
                return 0f;
            }
        });
    }

    public static void mission(NAR n) {
        n.goal($.$("(above)"), Tense.Eternal, 0f, 0.9f); //not above nor below
        n.goal($.$("(below)"), Tense.Eternal, 0f, 0.9f); //not above nor below

        //n.goal($.$("((above) && (below))"), Tense.Eternal, 0f, 0.99f); //neither above or below
        //n.goal($.$("((above) || (below))"), Tense.Eternal, 0f, 0.99f); //not above nor below

    }

    public static void command(NAR n) {

        n.goal($.$("(up)"), Tense.Present, 1f, 0.75f);
        n.goal($.$("(down)"), Tense.Present, 1f, 0.75f);

        //n.goal($.$("(up)"), Tense.Present, 0f, 0.25f);
        //n.goal($.$("(down)"), Tense.Present, 0f, 0.25f);



        //EXTREME CHEATS: "if i am up i should go down"
//        n.input("((above) ==>+0 (down))!");
//        n.input("((below) ==>+0 (up))!");
//        n.input("((above) ==>+0 (--,(up)))!");
//        n.input("((below) ==>+0 (--,(down)))!");

        //MODERATE CHEATS: "being up leads to me going down"
//        n.input("((above) ==>+0 (down)).");
//        n.input("((below) ==>+0 (up)).");
        //n.input("((above) ==> (down)). :|:");
        //n.input("((below) ==> (up)). :|:");
        //n.input("<(above) ==> (--,(up))>. :|:");
        //n.input("<(below) ==> (--,(down))>. :|:");

        //n.input("(up)@");
        //n.input("(down)@");
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

            if (d < 0.51f) return Float.NaN;
            if (d < b) return Float.NaN;
            //if (d < 0.5f && b < d) return Float.NaN;

            float current = Util.clamp(yEst.floatValue());
            float delta = speed * (up ? 1 : -1) * (d - b);
            float next = Util.clamp(delta + current);
            yEst.setValue(next);

            return (d-b)*0.5f; //0.5f + desired/2f;


        }

    }
}
