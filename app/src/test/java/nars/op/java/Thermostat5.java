//package nars.op.java;
//
//import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
//import com.gs.collections.api.block.predicate.primitive.FloatPredicate;
//import nars.$;
//import nars.Global;
//import nars.NAR;
//import nars.Narsese;
//import nars.concept.OperationConcept;
//import nars.nal.Tense;
//import nars.nar.Default;
//import nars.task.Task;
//import nars.util.Optimization;
//import nars.util.data.MutableInteger;
//import nars.util.data.Util;
//import nars.util.signal.MotorConcept;
//import nars.util.signal.SensorConcept;
//import org.apache.commons.lang3.mutable.MutableBoolean;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
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
//public class Thermostat5 {
//
//    public static final float basePeriod = 5;
//    public static final float tolerance = 0.05f;
//    public static float targetPeriod = 16;
//    public static final float speed = 0.15f;
//    public static final int commandPeriod = 32;
//    static boolean print = true;
//    static boolean printMotors = false;
//    static boolean debugError = false;
//    static boolean printStupid = true;
//
//    public static void main(String[] args) {
//        Default d = new Default(1024, 32, 2, 3);
//        d.cyclesPerFrame.set(1);
//        d.duration.set(Math.round(2f * basePeriod));
//        d.activationRate.setValue(0.15f);
//        d.shortTermMemoryHistory.set(2);
//        //d.premiser.confMin.setValue(0.02f);
//
//        float score = eval(d, 5000);
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
//            d.duration.set(Math.round(1.5f * basePeriod));
//            d.core.conceptsFiredPerCycle.set(5);
//            return d;
//        })
//                .with("activationRate", 0.1f, 0.3f, 0.1f, (a, x) -> {
//                    x.activationRate.setValue(a);
//                })
//                .with("conceptDurations", 0.1f, 5f, 0.1f, (a, x) -> {
//                    x.conceptForgetDurations.setValue(a);
//                })
//                .with("termLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
//                    x.termLinkForgetDurations.setValue(a);
//                })
//                .with("taskLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
//                    x.taskLinkForgetDurations.setValue(a);
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
//        Global.DEBUG = true;
//
//        //MutableFloat x0 = new MutableFloat();
//        //MutableFloat x1 = new MutableFloat();
//        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
//        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function
//
//        MutableFloat loss = new MutableFloat(0);
//
//
//        SensorConcept diffness;
//
//
//        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
//        //        .resolution(0.01f)/*.pri(0.2f)*/
//        //);
//        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
//                .resolution(0.01f).pri(0.2f)
//        );*/
//
//
//        diffness = new SensorConcept("(diff)", n, () -> {
//            float diff = yHidden.floatValue() - yEst.floatValue();
//            return Util.clamp( 0.5f + 0.5f * diff );
//        }).resolution(0.01f);
//
//
//
//        //OperationConcept up, down;
//        OperationConcept move = new OperationConcept("t(move)", n) {
//
//            @Nullable
//            @Override
//            public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
//
//                Task g = super.processGoal(goal, nar);
//                if (g!=null) {
//                    float upness = g.getDesire().motivation();
//                    if (t.intValue() > 0 && printStupid) {
//                        boolean up = (upness > 0f);
//                        float d = diffness.get();
//                        if ((up && d < 0.5f) || (!up && d > 0.5f)) {
//                            System.err.println("STUPID: " + n2(upness) + "^" + n2(d) + ":\t" + goal + "\n" + goal.explanation());
//                        } else {
//                            System.err.println(" SMART: " + n2(upness) + "^" + n2(d) + ":\t" + goal + "\n" + goal.explanation());
//                        }
//                    }
//
//                    float d2 = upness;// / (1 + Math.abs(upness));
//                    yEst.setValue(Util.clamp(speed * d2 + yEst.floatValue()));
//                    n.believe(term, Tense.Future, g.freq(), g.conf()); //(d2*2f + 0.5f), 0.1f);
//                }
//                return g;
//            }
//
//
//        };
//
////        DebugMotorConcept up, down;
////        n.on(up = new DebugMotorConcept(n, "t(up)", yEst, yHidden,
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
////        n.on(down = new DebugMotorConcept(n, "t(down)", yEst, yHidden,
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
//                double y = 0.5f + 0.45f * Math.sin(tt / (targetPeriod * basePeriod));
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
//                move.print();
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
//                out.print(" \t<:" + n2(diffness.get())  +
//                        " \t:" + n2(move.motivation(n)));
//                out.println();
//            }
//        });
//
//        //n.logSummaryGT(System.out, 0.0f);
//
//        float str = 0.25f;
//
//        //n.log();
//
//        int trainingRounds = 8;
//        for (int i = 0; i < trainingRounds; i++) {
//            float dd = 0.2f * trainingRounds; //(trainingRounds-1-i);
//
//            System.out.println("training up");
//            yEst.setValue(0.5f -dd);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal($.$("t(move)"), Tense.Present, 1f, str);
//                n.step();
//                //System.out.println(diffness.get());
//            } while (diffness.get() >= 0.49f);
//
//            System.out.println("training down");
//            yEst.setValue(0.5f + dd);
//            //n.goal($.$("t(up)"), Tense.Present, 0f, str);
//            //move.beliefs().clear();
//            //move.goals().clear();
//            do {
//                n.goal($.$("t(move)"), Tense.Present, 0, str);
//                n.step();
//                //System.out.println(diffness.get());
//            } while (diffness.get() <= 0.51f);
//        }
//
//        System.out.println("training finished");
//
//        System.out.println("beliefs formed during training:");
//        printBeliefs(n);
//
//
//
//
//        //move.beliefs().clear();
//        //move.goals().clear();
//        //n.goal($.$("t(move)"), 0.5f, str);
//        //n.goal($.$("t(move)"), 0f, str);
//        //n.goal($.$("t(move)"), Tense.Present, 0, str);
////        n.input("t(move)@");
//
//
//        yEst.setValue(0.5f);
//
//        command(n);
//
//        for (int i = 0; i < cycles; i++) {
//            n.step();
//            t.add(1);
//
////            if (i % commandPeriod == 0)
////                command(n);
//        }
//
//        printBeliefs(n);
//
//        return loss.floatValue() / t.intValue();
//
//    }
//
//    public static void command(NAR n) {
//        n.goal($.$("(diff)"), Tense.Present, 0.5f, 0.99f); //not above nor below
//        n.goal($.$("t(move)"), Tense.Present, 1f, 0.5f);
//        n.goal($.$("t(move)"), Tense.Present, 0f, 0.5f);
//        //n.goal($.$("t(move)"), Tense.Present, 0f, 0.9f);
//    }
//
//    public static void printBeliefs(NAR n) {
//        TreeSet<Task> bt = new TreeSet<>((a, b) -> { return a.term().toString().compareTo(b.term().toString()); });
//        n.forEachConcept(c -> {
//            if (c.hasBeliefs()) {
//                bt.add(c.beliefs().top(n.time()));
//                //System.out.println("\t" + c.beliefs().top(n.time()));
//            }
//        });
//        bt.forEach(xt -> {
//            System.out.println(xt);
//        });
//    }
//
//    private static class DebugMotorConcept extends MotorConcept {
//
//
//        long lastTime;
//
//        /**
//         * tasks collected from last cycle in which goals were received
//         */
//        protected final List<Task> current = Global.newArrayList();
//
//        public DebugMotorConcept(NAR n, String term, MutableFloat yEst, MutableFloat yHidden, FloatToFloatFunction motor, FloatPredicate errorful) throws Narsese.NarseseException {
//            super(term, n);
//
//            setMotor((v) -> {
//                float next = motor.valueOf(v);
//                if (debugError) {
//                    if (errorful.accept(v)) {
//                        for (Task t : current) {
//                            if (!t.isInput())
//                                System.err.println(t.explanation());
//                        }
//                    }
//                }
//                return next;
//            });
//            lastTime = -1;
//        }
//
//
//        @Nullable
//        @Override
//        public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
//            long now = nar.time();
//            if (now != lastTime) {
//                current.clear();
//            }
//            Task g = super.processGoal(goal, nar);
//            if (g != null)
//                current.add(g);
//            return g;
//        }
//    }
//}
