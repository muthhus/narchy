//package nars.op.java;
//
//import nars.$;
//import nars.NAR;
//import nars.Narsese;
//import BeliefTable;
//import nars.nal.Tense;
//import nars.nar.Default;
//import nars.task.Task;
//import nars.util.Optimization;
//import nars.util.data.MutableInteger;
//import nars.util.data.Util;
//import nars.util.signal.MotorConcept;
//import nars.util.signal.SensorConcept;
//import org.apache.commons.lang3.mutable.MutableFloat;
//
//import java.util.TreeSet;
//
//import static java.lang.System.out;
//import static nars.util.Texts.n2;
//
///**
// * this is trying to guess how to react to a hidden variable, its only given clues when its above or below
// * and its goal is to avoid both those states
// * anything below a score of 0.5 should be better than random
// * it gets these above/below hints but it has to process these among all the other processing its thinking about
// * then to really guess right it has to learn the timing of the sine wave
// * and imagine at what rate it will travel and when it will change direction etc
// */
//public class Thermostat6 {
//
//    public static final float basePeriod = 150;
//    public static final float tolerance = 0.02f;
//    public static float targetPeriod = 8;
//    public static final float speed = 0.02f;
//    static boolean print = true;
//    static boolean printMotors = false;
//    static boolean debugError = false;
//    static int sensorPeriod = 4; //frames per max sensor silence
//    static int commandPeriod = 256;
//
//    public static void main(String[] args) {
//        Default d = new Default(1024, 8, 2, 3);
//        d.conceptRemembering.setValue(6);
//        d.cyclesPerFrame.set(4);
//        d.conceptActivation.setValue(0.2f);
//        //d.conceptBeliefsMax.set(32);
//        d.shortTermMemoryHistory.set(3);
//        //d.derivationDurabilityThreshold.setValue(0.03f);
//        //d.perfection.setValue(0.9f);
//        //d.premiser.confMin.setValue(0.02f);
//
//        float score = eval(d, 120000);
//        System.out.println("score=" + score);
//    }
//
//    public static void main2(String[] args) {
//        int cycles = 2000;
//
//        new Optimization<Default>(() -> {
//            Default d = new Default(1024, 5, 2, 4);
//            //d.perfection.setValue(0.1);
//            d.shortTermMemoryHistory.setValue(2);
//            d.premiser.confMin.setValue(0.1f);
//            d.core.conceptsFiredPerCycle.set(5);
//            return d;
//        })
//                .with("activationRate", 0.1f, 0.3f, 0.1f, (a, x) -> {
//                    x.conceptActivation.setValue(a);
//                })
//                .with("conceptDurations", 0.1f, 5f, 0.1f, (a, x) -> {
//                    x.conceptRemembering.setValue(a);
//                })
//                .with("termLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
//                    x.termLinkRemembering.setValue(a);
//                })
//                .with("taskLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
//                    x.taskLinkRemembering.setValue(a);
//                })
//                /*.with("confMin", 0.05f, 0.3f, 0.01f, (a, x) -> {
//                    x.premiser.confMin.setValue(a);
//                })*/
//                /*.with("durationFactor", 0.25f, 3.5f, 0.1f, (dFactor, x) -> {
//                    x.duration.set(Math.round(basePeriod * dFactor));
//                })*/
//                /*.with("conceptsPercycle", 2, 5, 1, (c, x) -> {
//                    x.core.conceptsFiredPerCycle.set((int)c);
//                })*/
//                .run(x -> eval(x, cycles)).print();
//
//
//        //n.cyclesPerFrame.set(10);
//        //n.derivationDurabilityThreshold.setValue(0.02f);
//        //n.premiser.confMin.setValue(0.05f);
//
//        //System.out.println(eval(n, 1000));
//    }
//
//
//    public static float eval(NAR n, int cycles) {
//
//        final MutableInteger t = new MutableInteger();
//
//
//        //Global.DEBUG = true;
//
//        //MutableFloat x0 = new MutableFloat();
//        //MutableFloat x1 = new MutableFloat();
//        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
//        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function
//
//        MutableFloat loss = new MutableFloat(0);
//
//
//        SensorConcept above, below;
//
//
//        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
//        //        .resolution(0.01f)/*.pri(0.2f)*/
//        //);
//        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
//                .resolution(0.01f).pri(0.2f)
//        );*/
//
//        //FloatToFloatFunction invert = (i) -> 1f-i;
//
//
//
//        above = new SensorConcept("(above)", n, () -> {
//            float diff = yHidden.floatValue() - yEst.floatValue();
//            if (diff > -tolerance) return 0;
//            else return Util.clamp( -diff /2f + 0.5f);
//            //return 1f;
//        }).resolution(0.01f).timing(-1, sensorPeriod).pri(0.55f);
//
//        below = new SensorConcept("(below)", n, () -> {
//            float diff = yHidden.floatValue() - yEst.floatValue();
//            if (diff < tolerance) return 0;
//            else return Util.clamp( diff /2f + 0.5f);
//            //return 1f;
//        }).resolution(0.01f).timing(-1, sensorPeriod).pri(0.55f);
//
//
//        int visionResolution = 5;
//        float dv = 1f/visionResolution;
//
//        for (int i = 0; i < visionResolution; i++) {
//            vSensor(n, dv, i, yHidden, "(w" + i + ")").pri(0.05f);
//            vSensor(n, dv, i, yEst, "(v" + i + ")").pri(0.05f);
//        }
//
//        MotorConcept up = new Motor1D(n, true, yEst);
//        MotorConcept down = new Motor1D(n, false, yEst);
//
//        up.getFeedback().pri(0.5f);
//        down.getFeedback().pri(0.5f);
//
////        DebugMotorConcept up, down;
////        n.on(up = new DebugMotorConcept(n, "(up)", yEst, yHidden,
////                (v) -> {
////                    if (v > 0) {
////                        yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
////                        return (v/2) + 0.5f;
////                    }
////
////                    return Float.NaN;
////                },
////                (v) -> {
////                    if (t.intValue()==0) return false; //training
////                    //if already above the target value
////                    return yHidden.floatValue() - yEst.floatValue() > errorThresh;
////                }
////        ));
////        n.on(down = new DebugMotorConcept(n, "(down)", yEst, yHidden,
////                (v) -> {
////                    if (v > 0) {
////                        yEst.setValue(Util.clamp(-speed * v + yEst.floatValue()));
////                        return (v/2) + 0.5f;
////                    }
////                    return Float.NaN;
////                },
////                (v) -> {
////                    if (t.intValue()==0) return false; //training
////                    //if already above the target value
////                    return -(yHidden.floatValue() - yEst.floatValue()) > errorThresh;
////                }
////        ));
//
//
//        n.onFrame(nn -> {
//
//            //float switchPeriod = 20;
//            //float highPeriod = 5f;
//
//            float estimated = yEst.floatValue();
//
//            int tt = t.intValue();
//            float actual;
//            if (tt > 0) {
//
//                //double y = 0.5f + 0.45f * Math.sin(tt / (targetPeriod * basePeriod));
//
//                //float nnn = 3; //steps
//                //double y = 0.5f + 0.5f * Math.round(nnn * Math.sin(tt / (targetPeriod * basePeriod)))/nnn;
//
//                double y = 0.5f + 0.5f * Math.sin(tt / (targetPeriod * basePeriod));
//                //y = y > 0.5f ? 0.95f : 0.05f;
//
//                //x0.setValue(y); //high frequency phase
//                //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase
//
//                //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
//                yHidden.setValue(y);
//
//                actual = yHidden.floatValue();
//                //out.println( actual + "," + estimated );
//
//                loss.add(Math.abs(actual - estimated));
//            } else {
//                actual = 0.5f;
//            }
//
//            if (tt > 0 && printMotors) {
//                up.print();
//                down.print();
//                //System.out.println(up.current);
//                //System.out.println(down.current);
//            }
//            if (print) {
//
//                int cols = 50;
//                int colActual = (int) Math.round(cols * actual);
//                int colEst = (int) Math.round(cols * estimated);
//                for (int i = 0; i <= cols; i++) {
//
//                    char c;
//                    if (i == colActual)
//                        c = '#';
//                    else if (i == colEst)
//                        c = '|';
//                    else
//                        c = '.';
//
//                    out.print(c);
//                }
//
//                out.print(" \t<-" + n2(below.get()) +"|" +n2(above.get()) + "-> " +
//                        " \t-" + n2(down.expectation(n)) +
//                        " \t+" + n2(up.expectation(n))
//                );
//                out.println();
//            }
//        });
//
//        //n.logSummaryGT(System.out, 0.0f);
//
//        float str = 0.75f;
//
//        //n.log();
//
//
//        int trainingRounds = 2;
//        for (int i = 0; i < trainingRounds; i++) {
//            float dd = 0.2f * trainingRounds; //(trainingRounds-1-i);
//
//            System.out.println("training up");
//            yEst.setValue(0.5f -dd);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal("(up)", Tense.Present, 1f, str);
//                n.goal("(down)", Tense.Present, 0f, str);
//                n.step();
//            } while (above.get() < 0.6f);
//
//            System.out.println("training down");
//            yEst.setValue(0.5f + dd);
//            //n.goal($.$("(up)"), Tense.Present, 0f, str);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal("(up)", Tense.Present, 0f, str);
//                n.goal("(down)", Tense.Present, 1f, str);
//                n.step();
//                //System.out.println(diffness.get());
//            } while (below.get() < 0.6f);
//        }
//
//        System.out.println("training finished");
//
//        /*System.out.println("beliefs formed during training:");
//        printBeliefs(n, true);
//        printBeliefs(n, false);*/
//
//
//
//
//        //move.beliefs().clear();
//        //move.goals().clear();
//        //n.goal($.$("(up)"), 0.5f, str);
//        //n.goal($.$("(up)"), 0f, str);
//        //n.goal($.$("(up)"), Tense.Present, 0, str);
//
//
//
//        yEst.setValue(0.5f);
//        //n.run(16); //pause before beginning
//
//
//        mission(n);
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
//
//        return loss.floatValue() / t.intValue();
//
//    }
//
//    public static @org.jetbrains.annotations.NotNull SensorConcept vSensor(NAR n, float dv, int ii, MutableFloat zz, String cname) {
//        return new SensorConcept(cname, n, () -> {
//            float low = ii * dv;
//            float high = ii * dv;
//            float v = zz.floatValue();
//            if ((v >= low && v <= high)) {
//                return 1f;
//            } else {
//                return 0f;
//            }
//        });
//    }
//
//    public static void mission(NAR n) {
//        n.goal("(above)", Tense.Eternal, 0f, 0.9f); //not above nor below
//        n.goal("(below)", Tense.Eternal, 0f, 0.9f); //not above nor below
//
//        //n.goal($.$("((above) && (below))"), Tense.Eternal, 0f, 0.99f); //neither above or below
//        //n.goal($.$("((above) || (below))"), Tense.Eternal, 0f, 0.99f); //not above nor below
//
//    }
//
//    public static void command(NAR n) {
//
//        n.goal("(up)", Tense.Present, 1f, 0.75f);
//        n.goal("(down)", Tense.Present, 1f, 0.75f);
//
//        //n.goal($.$("(up)"), Tense.Present, 0f, 0.25f);
//        //n.goal($.$("(down)"), Tense.Present, 0f, 0.25f);
//
//
//
//        //EXTREME CHEATS: "if i am up i should go down"
////        n.input("((above) ==>+0 (down))!");
////        n.input("((below) ==>+0 (up))!");
////        n.input("((above) ==>+0 (--,(up)))!");
////        n.input("((below) ==>+0 (--,(down)))!");
//
//        //MODERATE CHEATS: "being up leads to me going down"
////        n.input("((above) ==>+0 (down)).");
////        n.input("((below) ==>+0 (up)).");
//        //n.input("((above) ==> (down)). :|:");
//        //n.input("((below) ==> (up)). :|:");
//        //n.input("<(above) ==> (--,(up))>. :|:");
//        //n.input("<(below) ==> (--,(down))>. :|:");
//
//        //n.input("(up)@");
//        //n.input("(down)@");
//    }
//
//    public static void printBeliefs(NAR n, boolean beliefsOrGoals) {
//        TreeSet<Task> bt = new TreeSet<>((a, b) -> { return a.term().toString().compareTo(b.term().toString()); });
//        n.forEachConcept(c -> {
//            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();
//            if (!table.isEmpty()) {
//                bt.add(table.top(n.time()));
//                //System.out.println("\t" + c.beliefs().top(n.time()));
//            }
//        });
//        bt.forEach(xt -> {
//            System.out.println(xt);
//        });
//    }
//
//    private static class Motor1D extends MotorConcept implements MotorConcept.MotorFunction {
//
//        final boolean up;
//        private final MutableFloat yEst;
//
//        public Motor1D(NAR n, boolean up, MutableFloat yEst) throws Narsese.NarseseException {
//            //super((up ? "up" : "down") + "()", n, null);
//            super("(" + (up ? "up" : "down") + ")", n, null);
//            this.up = up;
//            this.yEst = yEst;
//
//            setMotor(this);
//        }
//
//        @Override
//        protected int capacity(int cap, boolean beliefOrGoal, boolean eternalOrTemporal) {
//            return eternalOrTemporal ? 0 : cap; //no eternal
//        }
//
//        @Override
//        public float motor(float b, float d) {
//
//            if (d < 0.51f) return Float.NaN;
//            if (d < b) return Float.NaN;
//            //if (d < 0.5f && b < d) return Float.NaN;
//
//            float current = Util.clamp(yEst.floatValue());
//            float delta = speed * (up ? 1 : -1) * (d - b);
//            float next = Util.clamp(delta + current);
//            yEst.setValue(next);
//
//            return (d-b)*0.5f; //0.5f + desired/2f;
//
//
//        }
//
//    }
//}
