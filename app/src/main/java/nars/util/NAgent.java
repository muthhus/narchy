package nars.util;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.data.Util;
import nars.util.data.array.Arrays;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Agent interface wrapping a NAR
 */
public class NAgent implements Agent {

    public final NAR nar;

    float motivation[];
    int motivationOrder[];

    float input[];

    private List<MotorConcept> actions;
    private List<SensorConcept> inputs;
    private SensorConcept reward;
    private int lastAction = -1;
    private float prevReward = Float.NaN, dReward = 0;

    /** learning rate */
    float alpha = 0.1f;

    /** exploration rate - confidence of initial goal for each action */
    float epsilon = 0.01f;
    private double epsilonRandom = 0.01f;

    float sensorPriority = 0.5f;
    float rewardPriority = 0.5f;
    float goalFeedbackPriority = rewardPriority;
    float goalPriority = rewardPriority;

    final FloatToObjectFunction sensorTruth =  (v) -> {
        /*return new DefaultTruth(
                v < 0.5f ? 0 : 1f, alpha * 0.99f * Math.abs(v - 0.5f));*/

        return new DefaultTruth(v, alpha);
        //return new DefaultTruth(1f, v);
        //0.5f + alpha /2f /* learning rate */);
    };



    private int discretization = 1;
    private float lastMotivation = 0;


    //private SensorConcept dRewardPos, dRewardNeg;

    public NAgent(NAR n) {
        this.nar = n;
    }

    @Override
    public void start(int inputs, int actions) {
        nar.reset();

        motivation = new float[actions];
        motivationOrder = new int[actions];
        for (int i = 0; i < actions; i++)
            motivationOrder[i] = i;

        input = new float[inputs];

        this.actions = IntStream.range(0, actions).mapToObj(i -> {

            MotorConcept.MotorFunction motorFunc = (b,d) -> {

                motivation[i] =
                        //d;
                        //Math.max(0, d-b);
                        //(1+d)/(1+b);
                        //d / (d+b);
                        d-b;
                        //d  / (1f + b);

                /*if (d < 0.5) return 0; //Float.NaN;
                if (d < b) return 0; //Float.NaN;
                return d-b;*/

                return Float.NaN;
            };

            return new MotorConcept(actionConceptName(i), nar, motorFunc);
        }).collect( toList());



        this.inputs = IntStream.range(0, inputs).mapToObj(i -> {
            return getSensorConcepts(sensorTruth, i, discretization);
        }).flatMap(x -> x).collect( toList());

        this.reward = new SensorConceptDebug("(R)", nar,
                new RangeNormalizedFloat(() -> prevReward/*, -1, 1*/), sensorTruth)
                .resolution(0.01f)
                .pri(rewardPriority)
                .sensorDT(-1); //pertains to the prevoius frame

//        FloatSupplier linearPositive = () -> dReward > 0 ? 1 : 0;
//        FloatSupplier linearNegative = () -> dReward < 0 ? 1 : 0;
//        this.dRewardPos = new SensorConcept("(dRp)", nar,
//                linearPositive, sensorTruth)
//                .resolution(0.01f).timing(-1, -1);
//        this.dRewardNeg = new SensorConcept("(dRn)", nar,
//                linearNegative, sensorTruth)
//                .resolution(0.01f).timing(-1, -1);

        init();
    }

    public class SensorConceptDebug extends SensorConcept {

        public SensorConceptDebug(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
            super(term, n, input, truth);
        }

        @Override
        protected void onConflict(@NotNull Task belief) {
            NAgent.this.onConflict(this, belief);
        }
    }

    protected void onConflict(SensorConceptDebug sensorConceptDebug, Task belief) {

    }

    @Override
    public String summary() {
        return Texts.n2(motivation) + " [" +
                reward.belief(nar.time())
//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());
        ;

    }


    public
    @NotNull
    Stream<SensorConcept> getSensorConcepts(FloatToObjectFunction sensorTruth, int i, int bits) {



        return IntStream.range(0, bits).mapToObj(bit -> {

            if (bits == 1) {
                //single bit case
                return new SensorConceptDebug(inputConceptName(i,bit), nar,  () -> {
                    return input[i];
                }, sensorTruth).resolution(0.01f).timing(-1, -1).pri(sensorPriority);
            }

//
//            00 0
//            01 1
//            10 2
//            11 3
//
//            return new SensorConcept(inputConceptName(i,bit), nar,  () -> {
//
//                int v = (( Math.round(input[i] * (1 << bits)) >> (bit)) % 2);
//                //System.out.println(i + ": " + input[i] + " " + bit + " = " + v);
//                return v;
//
//            }, sensorTruth).resolution(0.01f).timing(-1, -1);

            float min = bit / bits, max = min + (1f/bits);

            return new SensorConceptDebug(inputConceptName(i,bit), nar,  () -> {

                float v = input[i];
                if (v >= 1f) v = 1f - Global.TRUTH_EPSILON; //clamp below 1.0 for this discretization

                //System.out.println(i + ": " + input[i] + " " + bit + " = " + v);
                return (v >= min) && (v < max) ? 1f : 0f;

            }, sensorTruth).resolution(0.01f).timing(-1, -1).pri(sensorPriority);

        });
    }

    protected void init() {

        seekReward();

        //nar.input("(--,(r))! %0.00;1.00%");
        actions.forEach(m -> init(m));


    }

    private void seekReward() {
        //TODO get this from the sensor/digitizers
        nar.goal("(R)", Tense.Eternal, 1f, 1f); //goal reward
        //nar.goal("(dRp)", Tense.Eternal, 0.95f, 1f); //prefer increase
        //nar.goal("(dRn)", Tense.Eternal, 0.05f, 1f); //avoid decrease
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

        decide(this.lastAction);

        return lastAction;

    }

    public void observe(float[] nextObservation) {
        System.arraycopy(nextObservation, 0, input, 0, nextObservation.length);

        //nar.conceptualize(reward, UnitBudget.One);
        nar.step();
    }

    private void learn(float[] input, int action, float reward) {

        if (Float.isFinite(prevReward))
            this.dReward = reward - prevReward;
        else
            this.dReward = 0;

        this.prevReward = reward;

    }

    private void decide(int lastAction) {
        int nextAction = -1;
        float nextMotivation;
        if (Math.random() < epsilonRandom) {
            nextAction = randomMotivation();
        } else {
            nextAction = decideMotivation();
            if (nextAction == -1)
                nextAction = randomMotivation();
        }
        nextMotivation = motivation[nextAction];

        float on;
        if (lastAction!=nextAction) {
            if (lastAction != -1) {

//                //TWEAK - unbelieve/undesire previous action less if its desire was stronger than this different action's current desire
                float off = 1f;
//                if (lastMotivation > nextMotivation) {
//                    off = 0.5f; //partial off
//                } else {
//                    off = 1; //full off
//                }

                nar.believe(goalFeedbackPriority, actions.get(lastAction), Tense.Present, 0, alpha * off);
                nar.goal(goalPriority, actions.get(lastAction), Tense.Present, 0f, alpha * off);
            }

            nar.goal(goalPriority, actions.get(nextAction), Tense.Present, 1f, alpha );

            on = 1f;
        } else {

//            //TWEAK - activate a repeated chosen goal less if reward has decreased
//            if (dReward >= 0) {
                on = 1f;
//            } else {
//                on = 0.5f;
//            }
        }
        nar.believe(goalFeedbackPriority, actions.get(nextAction), Tense.Present, 1f, alpha * on);

        /*for (int a = 0; a < actions.size(); a++)
            nar.believe(actions.get(a), Tense.Present,
                    (nextAction == a ? 1f : 0f), 0.9f);*/

        this.lastAction = nextAction;
        this.lastMotivation = nextMotivation;
    }

    private int randomMotivation() {
        return (int)(Math.random() * actions.size());
    }

    private int decideMotivation() {
        int nextAction = -1;
        boolean equalToLast = true;
        float nextMotivation = Float.NEGATIVE_INFINITY;

        Arrays.shuffle(motivationOrder, nar.random);

        for (int j = 0; j < motivation.length; j++) {
            int i = motivationOrder[j];
            float m = motivation[i];

            if (m > nextMotivation) {
                nextAction = i;
                nextMotivation = m;
            }
            if (equalToLast && j > 0 && !Util.equals(m, motivation[motivationOrder[j-1]]) ) {
                equalToLast = false; //there is some variation
            }

        }
        if (equalToLast) //all equal?
            return -1;


        return nextAction;
    }

    private String actionConceptName(int i) {

        return "(a,a" + i + ")";
    }

    private String inputConceptName(int i, int component) {
        return "(i" + i +
                    (component != -1 ? ("_" + component) :"") +
                ")";

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
