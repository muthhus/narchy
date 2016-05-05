package nars.util;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;

import java.util.List;
import java.util.TreeSet;
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

    /** exploratoin rate - confidence of initial goal for each action */
    float epsilon = 0.05f;

    float actionBeliefConfidence = 0.9f;

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

                motivation[i] =
                        //d-b;
                        //Math.max(0, d-b);
                        //d-b;
                        d  / (1f + b);

                /*if (d < 0.5) return 0; //Float.NaN;
                if (d < b) return 0; //Float.NaN;
                return d-b;*/

                return Float.NaN;
            };

            return new MotorConcept(actionConceptName(i), nar, motorFunc);
        }).collect( Collectors.toList());

        FloatToObjectFunction sensorTruth = (v) -> {
            return new DefaultTruth(v, 0.95f);
        };

        this.inputs = IntStream.range(0, inputs).mapToObj(i -> {
            return new SensorConcept(inputConceptName(i), nar,  () -> {

                return input[i];

            }, sensorTruth).resolution(0.01f).timing(-1, -1);
        }).collect( Collectors.toList());

        this.reward = new SensorConcept("(r)", nar,  new RangeNormalizedFloat(() -> prevReward), sensorTruth)
                .resolution(0.01f).timing(-1, -1);

        init();
    }

    protected void init() {

        seekReward();

        //nar.input("(--,(r))! %0.00;1.00%");
        actions.forEach(m -> init(m));


    }

    private void seekReward() {
        nar.goal("(r)", Tense.Eternal, 1f, 1f);
    }

    private void init(MotorConcept m) {
        //nar.ask($.$("(?x &&+0 " + m + ")"), '@');
        nar.goal(m, Tense.Present, 1f, epsilon);
        //nar.goal(m, Tense.Present, 0f, epsilon);


    }

    @Override
    public int act(float reward, float[] nextObservation) {

        if (lastAction!=-1) {
            learn(input, lastAction, reward);
        }

        observe(nextObservation);

        int nextAction = decide(this.lastAction);

        return this.lastAction = nextAction;
    }

    public void observe(float[] nextObservation) {
        System.arraycopy(nextObservation, 0, input, 0, nextObservation.length);

        //nar.conceptualize(reward, UnitBudget.One);
        nar.step();
    }

    private void learn(float[] input, int action, float reward) {
        this.prevReward = reward;

    }

    private int decide(int lastAction) {
        int nextAction = -1;
        float best = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < motivation.length; i++) {
            float m = motivation[i];
            if (m > best) {
                best = m;
                nextAction = i;
            }
        }

        if (lastAction!=nextAction) {
            if (lastAction != -1) {
                nar.believe(actions.get(lastAction), Tense.Present, 0f, actionBeliefConfidence);
            }
            nar.believe(actions.get(nextAction), Tense.Future, 1f, actionBeliefConfidence);
        }

        /*for (int a = 0; a < actions.size(); a++)
            nar.believe(actions.get(a), Tense.Present,
                    (nextAction == a ? 1f : 0f), 0.9f);*/

        return nextAction;
    }

    private String actionConceptName(int i) {
        return "(a" + i + ")";
    }
    private String inputConceptName(int i) {
        return "(i" + i + ")";

        //return "{i" + i + "}";
        //return "(input, i" + i + ")";
        //return "input:i" + i;
        //return "input:{i" + i + '}';

    }

    public static void printTasks(NAR n, boolean beliefsOrGoals) {
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


}
