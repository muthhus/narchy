package nars.util;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.data.Util;
import nars.util.data.array.Arrays;
import nars.util.math.FloatSupplier;
import nars.util.math.RangeNormalizedFloat;
import nars.util.signal.Emotion;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.$.$;
import static nars.nal.Tense.ETERNAL;

/**
 * Agent interface wrapping a NAR
 */
public class NAgent implements Agent {

    public final NAR nar;

    private IntFunction<Compound> sensorNamer = null;

    float motivation[];
    int motivationOrder[];

    float input[];

    private List<MotorConcept> actions;
    private List<SensorConcept> inputs;
    private SensorConcept reward;
    private int lastAction = -1;
    private float prevReward = Float.NaN;
    private int clockMultiplier = 16; //introduces extra timing delay between frames

    float dReward;

    /**
     * learning rate
     */
    float alpha, gamma;

    /**
     * exploration rate - confidence of initial goal for each action
     */
    float epsilon = 0.02f;
    private final double epsilonRandom = 0.01f;

    float sensorPriority;
    float rewardPriority;
    float goalFeedbackPriority;
    float goalPriority;

    final FloatToObjectFunction sensorTruth = (v) -> {
        /*return new DefaultTruth(
                v < 0.5f ? 0 : 1f, alpha * 0.99f * Math.abs(v - 0.5f));*/

        return new DefaultTruth(v, alpha);
        //return new DefaultTruth(1f, v);
        //0.5f + alpha /2f /* learning rate */);
    };



    private float lastMotivation;
    private int nextAction = -1;
    private SensorConcept dRewardSensor;


    public NAgent(NAR n) {
        this.nar = n;
    }

    @Override
    public void start(int inputs, int actions) {

        nar.reset();

        sensorPriority = nar.priorityDefault(Symbols.BELIEF);
        rewardPriority = goalFeedbackPriority = goalPriority = nar.priorityDefault(Symbols.GOAL);

        alpha = nar.confidenceDefault(Symbols.BELIEF);
        gamma = nar.confidenceDefault(Symbols.GOAL);

        motivation = new float[actions];
        motivationOrder = new int[actions];
        for (int i = 0; i < actions; i++)
            motivationOrder[i] = i;

        input = new float[inputs];

        this.actions = IntStream.range(0, actions).mapToObj(i -> {

            MotorConcept.MotorFunction motorFunc = (b, d) -> {

                motivation[i] =
                        d / (d+b);
                        //d - b;
                        //d;
                        //(d*d) - (b*b);
                        //Math.max(0, d-b);
                        //(1+d)/(1+b);
                        //d;
                        //d / (1 + b);

                //d  / (1f + b);

                /*if (d < 0.5) return 0; //Float.NaN;
                if (d < b) return 0; //Float.NaN;
                return d-b;*/

                return Float.NaN;
            };

            return new MotorConcept(actionConceptName(i), nar, motorFunc) {
                @Override
                public float floatValueOf(Term anObject) {
                    //feedback (belief)
                    if (i == nextAction) {
                        float m = motivation[i];
                        return 0.5f + 0.5f * (m);
                    }
                    return 0.0f; //this motor was not 'executed'
                    //return 0.5f;
                }
            };
        }).collect(toList());


        this.inputs = IntStream.range(0, inputs).mapToObj(i -> {
            return getSensorConcepts(sensorTruth, i);
        }).flatMap(x -> x).collect(toList());

        this.reward = new SensorConceptDebug("(R)", nar,
                new RangeNormalizedFloat(() -> prevReward/*, -1, 1*/), sensorTruth)
                .resolution(0.01f)
                .pri(rewardPriority);
                //.sensorDT(-1); //pertains to the prevoius frame

        final float dRewardThresh = 0.1f; //bigger than a change in X%
        FloatSupplier differential = () -> {
            if (Math.abs(dReward) < dRewardThresh)
                return 0.5f;
            else if (dReward < 0)
                return 0.0f; //negative
            else
                return 1f; //positive
        };

        this.dRewardSensor = new SensorConcept("(dR)", nar, differential, sensorTruth)
                .pri(rewardPriority)
                .resolution(0.01f);

//        FloatSupplier linearNegative = () -> dReward < -dRewardThresh ? 1 : 0;
//        this.dRewardNeg = new SensorConcept("(dRn)", nar,
//                linearNegative, sensorTruth)
//                .resolution(0.01f).timing(-1, -1);

        init();
    }

    public void setSensorNamer(IntFunction<Compound> sensorNamer) {
        this.sensorNamer = sensorNamer;
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

        @NotNull Emotion emotion = nar.emotion;

        return                    Texts.n2(motivation) + "\t + "
                 + "rwrd=" + Texts.n4(prevReward) + " "
                 + "hapy=" + Texts.n4(emotion.happy()) + " "
                 + "busy=" + Texts.n4(emotion.busy.getSum()) + " "
                 + "lern=" + Texts.n4(emotion.learning()) + " "
                 + "strs=" + Texts.n4(emotion.stress.getSum()) + " "
                 + "alrt=" + Texts.n4(emotion.alert.getSum()) + " "
                 + "\t" + nar.index.summary()

//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());
                ;

    }


    public
    @NotNull
    Stream<SensorConcept> getSensorConcepts(FloatToObjectFunction sensorTruth, int i) {


        return Stream.of(
                new SensorConcept(inputConceptName(i), nar, () -> {
                    return input[i];
                }, sensorTruth).resolution(0.01f).timing(-1, -1).pri(sensorPriority)
        );


    }

    protected void init() {

        updateMotors();

        seekReward();

        //nar.input("(--,(r))! %0.00;1.00%");
        actions.forEach(m -> init(m));


    }

    private void seekReward() {


        //nar.believe("((A:#x && I:#y) ==>+0 (R)).");

        //TODO specify goal via a method in the sensor/digitizers
        nar.goal("(R)", Tense.Eternal, 1f, 1f); //goal reward
        nar.goal("(R)", Tense.Present, 1f, 1f); //prefer increase

        nar.goal("(dR)", Tense.Eternal, 1f, 0.5f); //prefer increase usually

        //nar.goal("(dRp)", Tense.Eternal, 1f, 1f); //prefer increase
        //nar.goal("(dRp)", Tense.Present, 1f, 1f); //prefer increase

        //nar.goal("(dRn)", Tense.Eternal, 0f, 1f); //prefer increase
        //nar.goal("(dRn)", Tense.Present, 0f, 1f); //prefer increase

        Task whatCauseDeltaReward = nar.ask("(?x ==> (dR))", ETERNAL, causeOfIncreasedReward -> {
            System.out.println(causeOfIncreasedReward.explanation());
            return true;
        });
        Task howToAcheiveDeltaReward = nar.ask($("(dR)"), ETERNAL, '@', how -> {
            System.out.println(how.explanation());
            return true;
        });
        Task howToAcheiveReward = nar.ask($("(R)"), ETERNAL, '@', how -> {
            System.out.println(how.explanation());
            return true;
        });
        for (MotorConcept a : actions) {
            nar.ask(a, ETERNAL, '@', how -> {
                System.out.println(how.explanation());
                return true;
            });
        }

        //nar.goal("(dRn)", Tense.Eternal, 0f, 1f); //avoid decrease
    }

    private void init(MotorConcept m) {
        //nar.ask($.$("(?x &&+0 " + m + ")"), '@');
        nar.goal(m, Tense.Present, 1f, epsilon);
        nar.goal(m, Tense.Eternal, 1f, epsilon);
        //nar.goal(m, Tense.Present, 0f, epsilon);


    }

    @Override
    public int act(float reward, float[] nextObservation) {

        if (lastAction != -1) {
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
        nar.clock.tick(clockMultiplier);

    }

    private void learn(float[] input, int action, float reward) {

        if (Float.isFinite(prevReward))
            this.dReward = (reward - prevReward);
        else
            this.dReward = 0;

        this.prevReward = reward;

    }

    private void decide(int lastAction) {
        this.nextAction = -1;
        float nextMotivation;
        if (Math.random() < epsilonRandom) {
            nextAction = randomMotivation();
        } else {
            nextAction = decideMotivation();
            if (nextAction == -1)
                nextAction = randomMotivation();
        }

        nextMotivation = motivation[nextAction];

//        if (lastAction != nextAction) {
//            if (lastAction != -1) {
//                nar.goal(goalPriority, actions.get(lastAction), Tense.Present, 0.5f, gamma);
//            }
//
//            nar.goal(goalPriority, actions.get(nextAction), Tense.Present, 1f, gamma);
//        }

        updateMotors();

        this.lastAction = nextAction;
        this.lastMotivation = nextMotivation;
    }

    private void updateMotors() {
        //update all motors and their feedback
        for (MotorConcept m : actions) {
            m.commit();
        }
    }

    private int randomMotivation() {
        return (int) (Math.random() * actions.size());
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
            if (equalToLast && j > 0 && !Util.equals(m, motivation[motivationOrder[j - 1]])) {
                equalToLast = false; //there is some variation
            }

        }
        if (equalToLast) //all equal?
            return -1;


        return nextAction;
    }

    private String actionConceptName(int i) {
        //return "A:a" + i;
        //return "A:{a" + i + "}";
        return "(a" + i + ")";
        //return "{a" + i + "}";
    }

    private Compound inputConceptName(int i) {
        if (sensorNamer!=null) {
            return sensorNamer.apply(i);
        } else {
            //return inputConceptName(i, -1);
            //return $("(i" + i + ")");
            return $("I:i" + i);
            //return "I:{i" + i + "}";
            //return $("{i" + i + "}");

        }
    }

    private String inputConceptName(int i, int component) {
        return "(i" + i +
                (component != -1 ? ("_" + component) : "") +
                ")";

        //return "{i" + i + "}";
        //return "(input, i" + i + ")";
        //return "input:i" + i;
        //return "input:{i" + i + '}';

    }


}
