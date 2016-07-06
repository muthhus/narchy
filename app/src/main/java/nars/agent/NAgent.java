package nars.agent;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.concept.Concept;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.Texts;
import nars.util.Util;
import nars.util.math.FloatSupplier;
import nars.util.math.PolarRangeNormalizedFloat;
import nars.util.signal.Emotion;
import nars.util.signal.FuzzyConceptSet;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.$.*;
import static nars.nal.Tense.ETERNAL;
import static nars.util.Texts.n4;

/**
 * Agent interface wrapping a NAR
 */
public class NAgent implements Agent {

    public final NAR nar;

    private IntFunction<Compound> sensorNamer;

    float motivation[];

    float input[];

    public List<MotorConcept> actions;
    private List<SensorConcept> inputs;
    //private SensorConcept reward;
    private int lastAction = -1;
    public float reward = Float.NaN;

    private int ticksBeforeObserve = 1;
    private int ticksBeforeDecide = 1;

    float dReward;

    /**
     * learning rate
     */
    float alpha;
    public float gamma;

    /**
     * exploration rate - confidence of initial goal for each action
     */
    public float epsilon = 0.1f;

    float sensorPriority;
    float rewardPriority;
    float goalFeedbackPriority;
    float goalPriority;


    final FloatToObjectFunction sensorFreq = (v) -> {
        /*return new DefaultTruth(
                v < 0.5f ? 0 : 1f, alpha * 0.99f * Math.abs(v - 0.5f));*/

        return t(v, alpha);
        //return new DefaultTruth(1f, v);
        //0.5f + alpha /2f /* learning rate */);
    };

    /** values only on the positive half of the freq spectrum */
    final FloatToObjectFunction sensorFreqPos = (v) -> {
        float f;

        if (Math.abs(v/2f) <= Global.TRUTH_EPSILON*2f) {
            //f = 0f;
            f = 0.5f;
            //return null;
        } else if (v > 0) { //if (v > 0f) {
            f = 0.5f + v / 2f;
        } else {
            f = 0f;
        }
        return t(f, alpha);
    };

    final FloatToObjectFunction sensorFreqPosNeg = (v) -> {
        float f;
        if (Math.abs(v/2f) < Global.TRUTH_EPSILON) {
            f = 0.5f;
        } else if (v > 0f) {
            f = 0.5f + v / 2f;
        } else { //if (v < 0) {
            f = 0.5f - (-v) / 2f;
        }
        return t(f, alpha);
    };

    final FloatToObjectFunction sensorConf = (v) -> {
        /*return new DefaultTruth(
                v < 0.5f ? 0 : 1f, alpha * 0.99f * Math.abs(v - 0.5f));*/

        return t(1f, alpha * v);
        //return new DefaultTruth(1f, v);
        //0.5f + alpha /2f /* learning rate */);
    };



    private float[] lastMotivation;
    private int nextAction = -1;

    private final Budget RewardAttentionPerFrame =
            b(0.9f,0.9f,0.9f);
            //null;

    //private Budgeted ActionAttentionPerFrame = null; //b(0.9f,0.9f,0.9f);

    private SensorConcept dRewardSensorNeg;
    private final DecideAction decideAction;
    private final boolean synchronousGoalInput = false;


    private final int motorBeliefCapacity = 16;
    private final int motorGoalCapacity = 16;

    //private final int rewardBeliefCapacity = 2 * motorBeliefCapacity;

    public SensorConcept happy;
    public SensorConcept sad;
    public FuzzyConceptSet rewardConcepts;
    private Task beHappy, dontBeSad;


    public NAgent(NAR n) {

        this(n,
            new DecideActionSoftmax(0.4f)
            //new DecideActionEpsilonGreedy()
        );
    }

    public NAgent(NAR n, DecideAction decideAction) {
        this.nar = n;
        this.decideAction = decideAction;
    }

    @Override
    public void start(int inputs, int actions) {

        List<MotorConcept> outputConcepts = IntStream.range(0, actions).mapToObj(i -> {

            MotorConcept mc = new MotorConcept(actionConceptName(i), nar) {

                @Override protected void beliefCapacity(ConceptPolicy p) {
                    beliefs().capacity(0, motorBeliefCapacity);
                    goals().capacity(1, motorGoalCapacity);
                }

                @Override
                public float floatValueOf(Term anObject) {
//                    //shouldnt get called
//                    throw new UnsupportedOperationException();
//////                    //feedback (belief)
//                    float m = motivation[i];
//                    if (i == nextAction) {
//                        m = 0.5f + m / 2f;
//                    } else {
//                        m = 0f;
//                    }
//                    return m;
//                        //return 0.5f + 0.5f * (m);
//                        //return 0.9f;
//                    }
//                    return 0f;
//                    //return 0.5f; //this motor was not 'executed'
//                    //return 0.5f;
                    return Float.NaN;
                }
            };

            mc.feedback.pri(goalFeedbackPriority);
            //mc.feedback.maxTimeBetweenUpdates(8);

            return mc;
        }).collect(toList());


        List<SensorConcept> inputContepts = IntStream.range(0, inputs).mapToObj(i -> {
            return getSensorConcepts(sensorFreq, i);
        }).flatMap(x -> x).collect(toList());

        start(inputContepts, outputConcepts);


//        float rewardResolution = 0.05f;
//        this.happy = new SensorConcept($.prop(nar.self, the("happy")), nar,
//                new PolarRangeNormalizedFloat(()->
//                    prevReward
//                ), sensorFreqPos) {
//                    //HACK TODO make this a parameter for SensorConcept
//                    @Override protected void beliefCapacity(ConceptPolicy p) {
//                        beliefs().capacity(0, rewardBeliefCapacity);
//                        goals().capacity(1, motorGoalCapacity);
//                    }
//
//                }
//                .resolution(rewardResolution)
//                .pri(rewardPriority)
//                //.seek(true)
//        ;
//
//        this.sad = new SensorConcept(
//                $.prop(nar.self, the("sad")),
//                //$.inh(nar.self, $.neg($.seti(the("happy")))),
//                nar,
//                new PolarRangeNormalizedFloat(()->
//                    -prevReward
//                ), sensorFreqPos) {
//                    //HACK TODO make this a parameter for SensorConcept
//                    @Override protected void beliefCapacity(ConceptPolicy p) {
//                        beliefs().capacity(0, rewardBeliefCapacity);
//                        goals().capacity(1, motorGoalCapacity);
//                    }
//                }
//                .resolution(rewardResolution)
//                .pri(rewardPriority)
//                //.seek(false)
//        ;

//        this.reward = new SensorConceptDebug("(R)", nar,
//                //new RangeNormalizedFloat(
//                new PolarRangeNormalizedFloat(
//                    () -> prevReward/*, -1, 1*/
//                ),
//                sensorTruth)
//                .resolution(0.01f)
//                .pri(rewardPriority);
//                //.sensorDT(-1); //pertains to the prevoius frame



//        final float dRewardThresh = 0.1f; //bigger than a change in X%
//        FloatSupplier differential = () -> {
//            if (Math.abs(dReward) < dRewardThresh)
//                return 0.0f;
//            else if (dReward < 0)
//                return 0.0f; //negative
//            else
//                return 1f; //positive
//        };
//        FloatSupplier differentialNeg = () -> {
//            if (Math.abs(dReward) < dRewardThresh)
//                return 0.0f;
//            else if (dReward > 0)
//                return 0.0f;
//            else
//                return 1f;
//        };
//
//        this.dRewardSensor = new SensorConcept("(dR)", nar, differential, sensorFreq)
//                .pri(rewardPriority)
//                .resolution(0.01f);
//        this.dRewardSensorNeg = new SensorConcept("(dRn)", nar, differentialNeg, sensorFreq)
//                .pri(rewardPriority)
//                .resolution(0.01f);
        init();
    }

    public void start(List<SensorConcept> inputContepts, List<MotorConcept> outputConcepts) {
        this.inputs = inputContepts;
        this.actions = outputConcepts;

        input = new float[this.inputs.size()];
        motivation = new float[this.actions.size()];
        lastMotivation = new float[this.actions.size()];

        sensorPriority = nar.priorityDefault(Symbols.BELIEF);
        rewardPriority = goalFeedbackPriority = goalPriority = nar.priorityDefault(Symbols.GOAL);

        alpha = nar.confidenceDefault(Symbols.BELIEF);
        gamma = nar.confidenceDefault(Symbols.GOAL);


        rewardConcepts = rewardConcepts(() -> this.reward, nar).pri(rewardPriority);
        this.sad = rewardConcepts.sensors.get(0);
        this.happy = rewardConcepts.sensors.get(2);
    }

    public static FuzzyConceptSet rewardConcepts(FloatSupplier input, NAR nar) {
        return new FuzzyConceptSet(new PolarRangeNormalizedFloat(input), nar,
                "(I --> sad)", "(I --> neutral)", "(I --> happy)").resolution(0.1f);
    }

    public void setSensorNamer(IntFunction<Compound> sensorNamer) {
        this.sensorNamer = sensorNamer;
    }

    public void printActions() {
        for (MotorConcept a : actions) {
            a.print();
        }
        happy.print();
        sad.print();
    }



    @Override
    public String summary() {

        @NotNull Emotion emotion = nar.emotion;

        long now = nar.time();


        return                    Texts.n2(motivation) + "\t + "
//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "
                 + "hapy=" + n4(emotion.happy()) + " "
                 + "busy=" + n4(emotion.busy.getSum()) + " "
                 + "lern=" + n4(emotion.learning()) + " "
                 + "strs=" + n4(emotion.stress.getSum()) + " "
                 + "alrt=" + n4(emotion.alert.getSum()) + " "
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
                }, sensorTruth) {

//                    @Override
//                    public float pri() {
//                        //input priority modulated by concept priority
//                        float min = 0.5f;
//                        return sensorPriority * (min /* min */ + (1f-min) * nar.conceptPriority(this));
//                    }
                }.resolution(0.02f).timing(-1, -1).pri(sensorPriority)
        );


    }

    protected void init() {

        //updateMotors();

        seekReward();

        //nar.input("(--,(r))! %0.00;1.00%");
        actions.forEach(m -> init(m));


    }

    private void seekReward() {


        //nar.believe("((A:#x && I:#y) ==>+0 (R)).");

        //TODO specify goal via a method in the sensor/digitizers
        float eternalGoalSeekConf = 1f /* gamma */;
        this.beHappy = nar.goal(happy, Tense.Eternal, 1f, eternalGoalSeekConf);
        //nar.goal(happy, Tense.Present, 1f, gamma);

        this.dontBeSad = nar.goal(sad, Tense.Eternal, 0f, eternalGoalSeekConf);
        //nar.goal(sad, Tense.Present, 0f, gamma);

        /*nar.goal("(dR)", Tense.Eternal, 1f, 1f); //prefer increase usually
        nar.goal("(dR)", Tense.Present, 1f, 1f); //avoid decrease usually
        nar.goal("(dRn)", Tense.Eternal, 0f, 1f); //avoid decrease usually
        nar.goal("(dRn)", Tense.Present, 0f, 1f); //avoid decrease usually
        */

        //nar.goal("(dRp)", Tense.Eternal, 1f, 1f); //prefer increase
        //nar.goal("(dRp)", Tense.Present, 1f, 1f); //prefer increase

        //nar.goal("(dRn)", Tense.Eternal, 0f, 1f); //prefer increase
        //nar.goal("(dRn)", Tense.Present, 0f, 1f); //prefer increase

        //Task whatCauseReward = nar.ask("(?x ==> (R))", ETERNAL, (Task causeOfIncreasedReward) -> {
            //System.out.println(causeOfIncreasedReward.explanation());

//            //BOOST
//            if (causeOfIncreasedReward.expectation() > 0.75f) {
//                Term strategy = causeOfIncreasedReward.term(0);
//                System.err.println("\nSTRATEGY BOOST! :" + strategy);
//                nar.conceptualize(strategy, UnitBudget.Full.cloneMult(0.9f, 0.7f, 0.7f));
//            }

            //return true;
        //});
        for (Termed x : new Termed[] { happy, sad } ) {
            nar.ask($.impl($("?w"), x.term()), '?', ETERNAL);
            nar.ask(x, '@', ETERNAL);
        }

//        for (MotorConcept a : actions) {
//            nar.ask(a, ETERNAL, '@', how -> {
//                //System.out.println(how.explanation());
//                return true;
//            });
//        }

        //nar.goal("(dRn)", Tense.Eternal, 0f, 1f); //avoid decrease
    }

    private void init(MotorConcept m) {

//        nar.goal(m, Tense.Eternal, 1f, epsilon);


        //nar.ask($.$("(?x &&+0 " + m + ")"), '@');
        //nar.goal(m, Tense.Present, 1f, epsilon);
        //nar.goal(m, Tense.Present, 0f, epsilon);


    }

    @Override
    public int act(float rewardValue, float[] nextObservation) {

        if (lastAction != -1) {
            learn(input, lastAction, rewardValue);
        }

        observe(nextObservation);

        decide(this.lastAction);



        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));
        if (RewardAttentionPerFrame!=null) {

            //boost(happy);
            boost(beHappy);
            //boost(sad);
            boost(dontBeSad);

            for (MotorConcept c : actions)
                boost(c);

            //nar.goal(RewardAttentionPerFrame.pri(), sad, now+1, 0f, gamma);
            //nar.goal(RewardAttentionPerFrame.pri(), happy, now+1, 1f, gamma);
        }
//        for (Concept a : actions) {
//            nar.conceptualize(a, ActionAttentionPerFrame);
//        }

        return lastAction;

    }

    private void boost(Task t) {
        BudgetMerge.max.apply(t.budget(), RewardAttentionPerFrame);
        nar.activate(t);
    }


    public @Nullable Concept boost(Concept c) {
        return nar.activate(c, RewardAttentionPerFrame, 1f, 1f, null);
    }

    public void observe(float[] nextObservation) {

        System.arraycopy(nextObservation, 0, input, 0, nextObservation.length);

        nar.clock.tick(ticksBeforeObserve-1);
        nar.step();


    }

    private void learn(float[] input, int action, float reward) {

        float prevReward = this.reward;
        if (Float.isFinite(prevReward))
            this.dReward = (reward - prevReward);
        else
            this.dReward = 0;

        this.reward = reward;

    }

    float lastOnConf = 0;

    public int decide(int _lastAction) {

        nar.clock.tick(ticksBeforeDecide);

        this.nextAction = -1;


        float nextMotivation;

        nextAction = decideMotivation();

        if (nextAction == -1) {
            nextAction = randomMotivation();
        }

        nextMotivation = motivation[nextAction];

        long now = nar.time();

//        float onConf = 0.5f + 0.5f * Math.max(
//                //w2c(d *motivation.length) * gamma,
//                //(decisiveness(nextAction) * 0.5f + 0.5f) * gamma,
//                //w2c(decisiveness(nextAction) * 0.5f + 0.5f) * gamma,
//                w2c(motivation[nextAction]),
//                //gamma,
//            Global.TRUTH_EPSILON);
//
//        float offConf = 0.5f + 0.5f * Math.max(
//                lastAction!=-1 ? w2c(lastMotivation[lastAction]) : 0,
//            Global.TRUTH_EPSILON);
                //TODO find an accurate way to do this
                //lastOnConf*(motivation[lastAction]-lastMotivation[lastAction]);

        float onConf = gamma, offConf = gamma;


        if (synchronousGoalInput || lastAction != nextAction) {


            //belief/goal feedback levels
            float off =
                    0;
                    //0.5f - (n-1)/n;
                    //0f; //0.25f; //0.49f;
            float on = 1f; //0.75f;
            float preOff = (off+on*2f)/3f; //0.75f;
            float preOn = (on+off*2f)/3f; // 0.75f;

            if (lastAction != -1) {
                MotorConcept lastActionMotor = actions.get(lastAction);
                //nar.goal(goalPriority, lastActionMotor, now-1, preOff, conf); //downward step function top
                //nar.believe(goalPriority, lastActionMotor, now, preOff, conf); //downward step function top

                nar.goal(goalPriority, lastActionMotor, now-1, off, offConf); //downward step function bottom
                nar.believe(goalPriority, lastActionMotor, now, off, offConf); //downward step function bottom
            }

            MotorConcept nextAction = actions.get(this.nextAction);
            //nar.goal(goalPriority, nextAction, now, preOn-1, conf); //upward step function bottom
            //nar.believe(goalPriority, nextAction, now, preOn, conf); //upward step function bottom


            nar.goal(goalPriority, nextAction, now, on, onConf); //upward step function top
            nar.believe(goalPriority, nextAction, now+1, on, onConf); //upward step function top
        }

        //updateMotors();

        this.lastAction = nextAction;
        System.arraycopy(motivation, 0, lastMotivation, 0, motivation.length);
        this.lastOnConf = onConf;

        return nextAction;
    }

    /** measure of the motivation decisiveness (inverse of confusion) of the next selected action relative to the other actions
     * @return value in (0..1.0]
     */
    private float decisiveness(int nextAction) {

        float[] minmax = Util.minmax(motivation);
        int actions = motivation.length;
        float[] motNorm = new float[actions];
        float min = minmax[0];
        float max = minmax[1];
        if ( min == max) return 1f;
        float s = 0;
        for (int i = 0; i < actions; i++) {
            float m;
            motNorm[i] = m = Util.normalize(motivation[i], min, max);
            s += m;
        }
        if (s == 0) return 1f;
        float p = motNorm[nextAction] / s;
        return p;

    }

//    private void updateMotors() {
//        //update all motors and their feedback
//        for (MotorConcept m : actions) {
//            m.commit();
//        }
//    }

    private int randomMotivation() {
        System.err.println("RANDOM");
        return (int) (Math.random() * actions.size());
    }

    private final int decideMotivation() {

        float[] motivation = this.motivation;

        for (int i = 0, actionsSize = actions.size(); i < actionsSize; i++) {
            motivation[i] = motivation(actions.get(i));
        }

        return decideAction.decideAction(motivation.clone(), lastAction, nar.random);
    }

    /** maps a concept's belief/goal state to a number */
    protected float motivation(MotorConcept m) {
        return m.goals().motivation(nar.time());

        //                    //(d > 0.5 && d > b ? d - b : 0);
//                    //(d > 0.5 ? d : 0) / (d+b);
//                    //d / (d+b);
//                    //(d > 0.1f ? d - b : -1f);
//                    //d - b;
//                    d;
//            //(d*d) - (b*b);
//            //Math.max(0, d-b);
//            //(1+d)/(1+b);
//            //d;
//            //d / (1 + b);/
//            //d  / (1f + b);
//            return Float.NaN;
//        };

    }


    private String actionConceptName(int i) {
        return "(a" + i + ")";
        //return "a:a" + i;
        //return "A:a" + i;
        //return "A:{a" + i + "}";
        //return "(a,a" + i + ")";
        //return "(a" + i + ")";
        //return "{a" + i + "}";
    }

    private Compound inputConceptName(int i) {
        if (sensorNamer!=null) {
            return sensorNamer.apply(i);
        } else {
            //return inputConceptName(i, -1);
            //return $("(i" + i + ")");
            return $("i:i" + i);
            //return "I:{i" + i + "}";
            //return $("{i" + i + "}");

        }
    }

//    private String inputConceptName(int i, int component) {
//        return "(i" + i +
//                (component != -1 ? ("_" + component) : "") +
//                ")";
//
//        //return "{i" + i + "}";
//        //return "(input, i" + i + ")";
//        //return "input:i" + i;
//        //return "input:{i" + i + '}';
//
//    }


}
