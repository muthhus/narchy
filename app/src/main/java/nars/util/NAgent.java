package nars.util;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.nal.Tense;
import nars.truth.DefaultTruth;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Agent interface wrapping a NAR
 */
public class NAgent implements Agent {

    private final NAR nar;

    float motivation[];
    float input[];

    private List<MotorConcept> actions;
    private List<SensorConcept> inputs;
    private SensorConcept reward;
    private int lastAction = -1;
    private float prevReward;

    int observeFrames, learnFrames;


    public NAgent(NAR n) {
        this.nar = n;
    }

    @Override
    public void start(int inputs, int actions) {
        nar.reset();

        motivation = new float[actions];
        input = new float[inputs];

        this.actions = IntStream.range(0, actions).mapToObj(i -> {

            MotorConcept.MotorFunction motorFunc = (b,d) -> {

                motivation[i] = d-b;

                /*if (d < 0.5) return 0; //Float.NaN;
                if (d < b) return 0; //Float.NaN;
                return d-b;*/

                return Float.NaN;
            };

            return new MotorConcept(actionConceptName(i), nar, motorFunc);
        }).collect( Collectors.toList());

        FloatToObjectFunction sensorTruth = (v) -> {
            return new DefaultTruth(v, 0.9f);
        };
        this.inputs = IntStream.range(0, inputs).mapToObj(i -> {
            return new SensorConcept(inputConceptName(i), nar,  () -> {

                return input[i];

            }, sensorTruth).resolution(0.01f).timing(-1, -1);
        }).collect( Collectors.toList());

        this.reward = new SensorConcept("(r)", nar,  () -> prevReward, sensorTruth)
                .resolution(0.01f).timing(-1, -1);

        init();
    }

    protected void init() {

        nar.input("(r)! %1.00;1.00%");
        actions.forEach(m -> init(m));

        observeFrames = 64;
        learnFrames = 64;

    }

    private void init(MotorConcept m) {
        //nar.ask($.$("(?x &&+0 " + m + ")"), '@');
        nar.goal(m, 1f, 0.1f);

    }

    @Override
    public int act(float reward, float[] nextObservation) {

        if (lastAction!=-1) {
            learn(input, lastAction, reward);
        }

        observe(nextObservation);

        return this.lastAction = decide();
    }

    public void observe(float[] nextObservation) {
        System.arraycopy(nextObservation, 0, input, 0, nextObservation.length);
        nar.run(observeFrames);
    }

    private void learn(float[] input, int action, float reward) {
        this.prevReward = reward;

        nar.believe(actions.get(action), Tense.Present, 1f, 0.9f);

        nar.run(learnFrames);
    }

    private int decide() {
        int b = -1;
        float best = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < motivation.length; i++) {
            float m = motivation[i];
            if (m > best) {
                best = m;
                b = i;
            }
        }

        return b;
    }

    private String actionConceptName(int i) {
        return "(a" + i + ")";
    }
    private String inputConceptName(int i) {
        return "(i" + i + ")";
    }


}
