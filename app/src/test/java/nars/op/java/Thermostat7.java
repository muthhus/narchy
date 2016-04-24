package nars.op.java;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
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
public class Thermostat7 {

    public static final float basePeriod = 300;
    public static final float tolerance = 0.01f;
    public static float targetPeriod = 8;
    public static final float speed = 0.06f;
    static boolean print = true;
    static boolean printMotors = false;
    static boolean debugError = false;
    static int sensorPeriod = 16; //frames per max sensor silence
    static int commandPeriod = 1024;

    public static void main(String[] args) {
        Default d = new Default(1024, 15, 3, 3);
        d.conceptRemembering.setValue(50);
        d.cyclesPerFrame.set(3);
        d.activationRate.setValue(0.05f);
        //d.conceptBeliefsMax.set(32);
        d.shortTermMemoryHistory.set(2);
        //d.derivationDurabilityThreshold.setValue(0.03f);
        //d.perfection.setValue(0.9f);
        //d.premiser.confMin.setValue(0.02f);

        float score = eval(d, 100);
        System.out.println("score=" + score);
    }


    public static float eval(NAR n, int cycles) {

        final MutableInteger t = new MutableInteger();


        //Global.DEBUG = true;

        //MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        SensorConcept above, below;


        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
        //        .resolution(0.01f)/*.pri(0.2f)*/
        //);
        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
                .resolution(0.01f).pri(0.2f)
        );*/

        //FloatToFloatFunction invert = (i) -> 1f-i;



        above = new SensorConcept("(above)", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > -tolerance) return 0;
            else return Util.clamp( -diff /2f + 0.5f);
            //return 1f;
        }).resolution(0.01f).timing(-1, sensorPeriod);//.pri(0.35f);

        below = new SensorConcept("(below)", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff < tolerance) return 0;
            else return Util.clamp( diff /2f + 0.5f);
            //return 1f;
        }).resolution(0.01f).timing(-1, sensorPeriod);//.pri(0.35f);

        MotorConcept up = new Motor1D("(up)", true, n, yEst);
        MotorConcept down = new Motor1D("(--,(up))", false, n, yEst);



//        DebugMotorConcept up, down;
//        n.on(up = new DebugMotorConcept(n, "(up)", yEst, yHidden,
//                (v) -> {
//                    if (v > 0) {
//                        yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
//                        return (v/2) + 0.5f;
//                    }
//
//                    return Float.NaN;
//                },
//                (v) -> {
//                    if (t.intValue()==0) return false; //training
//                    //if already above the target value
//                    return yHidden.floatValue() - yEst.floatValue() > errorThresh;
//                }
//        ));
//        n.on(down = new DebugMotorConcept(n, "(down)", yEst, yHidden,
//                (v) -> {
//                    if (v > 0) {
//                        yEst.setValue(Util.clamp(-speed * v + yEst.floatValue()));
//                        return (v/2) + 0.5f;
//                    }
//                    return Float.NaN;
//                },
//                (v) -> {
//                    if (t.intValue()==0) return false; //training
//                    //if already above the target value
//                    return -(yHidden.floatValue() - yEst.floatValue()) > errorThresh;
//                }
//        ));


        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            float estimated = yEst.floatValue();

            int tt = t.intValue();
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

            if (tt > 0 && printMotors) {
                up.print();
                //System.out.println(up.current);
                //System.out.println(down.current);
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

                out.print(" \t<-" + n2(below.get()) +"|" +n2(above.get()) + "-> " +
                        " \t-" + n2(up.belief(n.time()).expectationNegative()) +
                        " \t+" + n2(up.belief(n.time()).expectationPositive())
                );
                out.println();
            }
        });

        //n.logSummaryGT(System.out, 0.0f);

        float str = 0.55f;

        //n.log();


        int trainingRounds = 2;
        for (int i = 0; i < trainingRounds; i++) {
            float dd = 0.2f * trainingRounds; //(trainingRounds-1-i);

            System.out.println("training up");
            yEst.setValue(0.5f -dd);
            //move.beliefs().clear();
            //move.goals().clear();
            do {
                n.goal($.$("(up)"), Tense.Present, 1f, str);
                n.step();
            } while (above.get() < 0.6f);

            System.out.println("training down");
            yEst.setValue(0.5f + dd);
            //n.goal($.$("(up)"), Tense.Present, 0f, str);
            //move.beliefs().clear();
            //move.goals().clear();
            do {
                n.goal($.$("(up)"), Tense.Present, 0f, str);
                n.step();
                //System.out.println(diffness.get());
            } while (below.get() < 0.6f);
        }

        System.out.println("training finished");

        /*System.out.println("beliefs formed during training:");
        printBeliefs(n, true);
        printBeliefs(n, false);*/

        n.goal($.$("(up)"), Tense.Present, 0.5f, str);
        yEst.setValue(0.5f);



        mission(n);

        for (int i = 0; i < cycles; i++) {

            n.step();


            t.add(1); //cause delays in the sine wave



            if (i % commandPeriod == 0) {
                command(n);
            }
        }

        printBeliefs(n, true);
        printBeliefs(n, false);

        return loss.floatValue() / t.intValue();

    }

    public static void mission(NAR n) {
        n.input("$1.0;1.0;1.0$ (above)! :|: %0.0;0.75%"); //sometimes necessary to go up or down, so not freq 0
        n.input("$1.0;1.0;1.0$ (below)! :|: %0.0;0.75%");

        n.input("((below) ==> (up)). %1.0;0.99%");
        n.input("((above) ==> (--,(up))). %1.0;0.99%");
        //n.input("( ((--,(below))&&(--,(above))) ==> ((--,(up))&&(--,(down))) )! %1.0;1.0%");

        //n.goal($.$("((above) && (below))"), Tense.Eternal, 0f, 0.99f); //neither above or below
        //n.goal($.$("((above) || (below))"), Tense.Eternal, 0f, 0.99f); //not above nor below

    }

    public static void command(NAR n) {

//        n.goal($.$("(up)"), Tense.Present, 1f, 0.85f);

        //n.goal($.$("(up)"), Tense.Present, 0f, 0.25f);
        //n.goal($.$("(down)"), Tense.Present, 0f, 0.25f);



//        //EXTREME CHEATS: "if i am up i should go down"
//        n.input("((above) ==> (down))!");
//        n.input("((below) ==> (up))!");

        //n.input("((above) ==> (--,(up)))! :|:");
        //n.input("((below) ==> (--,(down)))! :|:");

        //MODERATE CHEATS: "being up leads to me going down"
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
            System.out.println(n.conceptPriority(c, 0) + " " + c);
        });
        bt.forEach(xt -> {
            System.out.println(xt);
        });
    }

    private static class Motor1D extends MotorConcept implements MotorConcept.MotorFunction {

        final boolean up;
        private final MutableFloat yEst;

        public Motor1D(String term, boolean up, NAR n, MutableFloat yEst) throws Narsese.NarseseException {
            //super((up ? "up" : "down") + "()", n, null);
            super(term, n, null);
            this.up = up;
            this.yEst = yEst;

            setMotor(this);
        }

        @Override
        public float motor(float believed, float desired) {

            desired = (desired-0.5f)*2f;

//            if (hasBeliefs()) {
//                System.out.println(beliefs().top(nar.time()).explanation());
//                //beliefs().print(System.out);
//            }
//            if (hasGoals()) {
//                System.out.println(goals().top(nar.time()).explanation());
//                goals().print(System.out);
//            }

            float current = Util.clamp(yEst.floatValue());
            float delta = speed * (up ? 1 : -1) * desired;
            float next = Util.clamp(delta + current);
            yEst.setValue(next);

            return Float.NaN; //0.5f + desired/2f;

        }

    }
}
