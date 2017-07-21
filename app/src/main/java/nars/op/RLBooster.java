//package nars.op;
//
//import jcog.learn.Agent;
//import nars.$;
//import nars.NAgent;
//import nars.NAgentX;
//import nars.concept.ActionConcept;
//import nars.concept.SensorConcept;
//import nars.task.NALTask;
//import nars.time.Tense;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//import java.util.function.Consumer;
//
///**
// * NAgent Reinforcement Learning Algorithm Accelerator
// */
//public class RLBooster implements Consumer<NAgent> {
//
//    public static final Logger logger = LoggerFactory.getLogger(RLBooster.class);
//
//    public final NAgent env;
//    public final Agent rl;
//    final float[] input;
//    final Runnable[] output;
//    final int inD, outD;
//    private final List<SensorConcept> in;
//
//    public RLBooster(NAgent env, Agent rl, int actionDiscretization) {
//        this.env = env;
//        this.rl = rl;
//
//        float conf =
//                //0.5f;
//                env.nar.confMin.floatValue() * 2f;
//
//        env.curiosity().setValue(0f);
//
//        List<SensorConcept> sc = $.newArrayList();
//        env.sense(env.nar, 0).forEach(x -> {
//            sc.add((SensorConcept) ((NALTask)x).concept(env.nar, true));
//        });
//        if (env instanceof NAgentX) {
//            ((NAgentX) env).cam.values().forEach(c -> {
//                c.pixels.forEach(cc -> sc.add((SensorConcept) cc));
//            });
//        }
//        this.in = sc;
//        this.inD = sc.size();
//
//        input = new float[inD];
//
//        boolean nothingAction = false; //reserve 0 for nothing
//
//        this.outD = (nothingAction ? 1 : 0) /* nothing */ + env.actions.size() * actionDiscretization /* pos/neg for each action */;
//        this.output = new Runnable[outD];
//
//        int i = 0;
//        if (nothingAction) {
//            output[i++] = () -> {            };
//        }
//
//        logger.info("{} {} in={} out={}", rl, env, inD, outD);
//        rl.start(inD, outD);
//
//        for (ActionConcept a : env.actions) {
//            for (int j = 0; j < actionDiscretization; j++) {
//                float value = ((float)j) / (actionDiscretization-1);
//                output[i++] = () -> {
//                    env.nar.goal(a, Tense.Present, value, conf);
//                };
//            }
//        }
//
//        env.onFrame(this);
//    }
//
//    float[] input() {
//        int i = 0;
//        for (SensorConcept s : in) {
//            input[i++] = s.asFloat();
//        }
//
//        //TODO include previous outputs?
//        return input;
//    }
//
//    @Override
//    public void accept(NAgent ignored) {
//
//        int o = rl.act(env.reward, input());
//        //System.out.println(now + " "  + o + " " + a.o.floatValue() + " " + " " + a.rewardValue);
//
//        output[o].run();
//
//    }
//}
