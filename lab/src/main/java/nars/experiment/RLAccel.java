package nars.experiment;

import jcog.learn.Agent;
import nars.$;
import nars.NAgent;
import nars.NAgentX;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.time.Tense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * NAgent Reinforcement Learning Algorithm Accelerator
 */
public class RLAccel implements Consumer<NAgent> {

    public static final Logger logger = LoggerFactory.getLogger(RLAccel.class);

    public final NAgent env;
    public final Agent rl;
    final float[] input;
    final Runnable[] output;
    final int inD, outD;
    private final List<SensorConcept> in;

    public RLAccel(NAgent env, Agent rl) {
        this.env = env;
        this.rl = rl;

        float conf =
                //0.5f;
                env.nar.confMin.floatValue() * 2f;

        env.curiosityProb.setValue(0f);

        List<SensorConcept> sc = $.newArrayList();
        env.sense(env.nar, 0).forEach(x -> {
            sc.add((SensorConcept) x.concept(env.nar));
        });
        if (env instanceof NAgentX) {
            ((NAgentX) env).cam.values().forEach(c -> {
                c.pixels.forEach(cc -> sc.add((SensorConcept) cc));
            });
        }
        this.in = sc;
        this.inD = sc.size();

        input = new float[inD];

        final float[] actionValues = {+1f, 0.5f, 0f};

        this.outD = 1 /* nothing */ + env.actions.size() * actionValues.length /* pos/neg for each action */;
        this.output = new Runnable[outD];

        int i = +1; //reserve 0 for nothing
        output[0] = () -> {
        };


        logger.info("{} {} in={} out={}", rl, env, inD, outD);
        rl.start(inD, outD);

        for (ActionConcept a : env.actions) {
            for (float polarity : actionValues) {
                output[i++] = () -> {
                    env.nar.goal(a, Tense.Present, polarity,
                            conf);
                };
            }
        }

        env.onFrame(this);
    }

    float[] input() {
        int i = 0;
        for (SensorConcept s : in) {
            input[i++] = s.asFloat();
        }

        //TODO include previous outputs?
        return input;
    }

    @Override
    public void accept(NAgent ignored) {

        int o = rl.act(env.rewardValue, input());
        //System.out.println(now + " "  + o + " " + a.o.floatValue() + " " + " " + a.rewardValue);

        output[o].run();

    }
}
