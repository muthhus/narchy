package nars.op;

import nars.*;
import nars.budget.Budget;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Activation;
import nars.concept.Concept;
import nars.task.GeneratedTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.list.FasterList;
import nars.util.math.FirstOrderDifferenceFloat;
import nars.util.math.PolarRangeNormalizedFloat;
import nars.util.math.RangeNormalizedFloat;
import nars.util.signal.Emotion;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.$.t;
import static nars.agent.NAgentOld.varPct;
import static nars.nal.Tense.ETERNAL;
import static nars.nal.UtilityFunctions.or;
import static nars.nal.UtilityFunctions.w2c;
import static nars.util.Texts.n2;
import static nars.util.Texts.n4;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent {


    static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    public final SensorConcept happy;

    public final SensorConcept joy;
    public final RangeNormalizedFloat rewardNormalized;
    public NAR nar;

    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<MotorConcept> actions = $.newArrayList();

    public float alpha, gamma, epsilonProbability = 0.1f;
    @Deprecated public float gammaEpsilonFactor = 0.5f;

    final int CURIOSITY_DURATION = 32; //frames
    final DescriptiveStatistics motorDesireEvidence = new DescriptiveStatistics(CURIOSITY_DURATION);
    final DescriptiveStatistics rewardWindow = new DescriptiveStatistics(CURIOSITY_DURATION);




    public float rewardValue;
    private final FasterList<Task> predictors = $.newArrayList();
    public boolean trace = false;

    protected int ticksBeforeObserve;
    int ticksBeforeDecide;
    protected long now;
    private long stopTime;
    private NARLoop loop;
    private Budget boostBudget, curiosityBudget;
    private final float reinforcementAttention;
    //private float curiosityAttention;
    private float rewardSum = 0;

    public NAgent(NAR nar) {
        this.nar = nar;
        alpha = this.nar.confidenceDefault(Symbols.BELIEF);
        gamma = this.nar.confidenceDefault(Symbols.GOAL);

        float rewardGamma =
                1.0f - Param.TRUTH_EPSILON
                //1.0f
                //gamma
        ;

        this.reinforcementAttention = or(nar.DEFAULT_BELIEF_PRIORITY, nar.DEFAULT_GOAL_PRIORITY);


        float rewardConf = alpha;

        rewardNormalized = new PolarRangeNormalizedFloat(() -> rewardValue);

        happy = new SensorConcept("(happy)", nar,
                rewardNormalized,
                (x) -> t(x, rewardConf)
        );
        predictors.add(happy.desire($.t(1f, rewardGamma), reinforcementAttention, 0.75f));


        joy = new SensorConcept("(joy)", nar,
                new PolarRangeNormalizedFloat(
                    new FirstOrderDifferenceFloat(
                        ()->nar.time(), () -> rewardValue
                    )
                ),
                (x) -> t(x, rewardConf)
        );

    }

    /**
     * install motors and sensors in the NAR
     */
    abstract protected void init(NAR n);


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();

    protected void next() {

        for (int i = 0; i < ticksBeforeObserve; i++)
            nar.clock.tick();

        reinforce();

        for (int i = 0; i < ticksBeforeDecide; i++)
            nar.clock.tick();



        now = nar.time();

        float r = rewardValue = act();
        rewardSum += r;
        rewardWindow.addValue(rewardValue);

        if (trace)
            System.out.println(summary());


        if (stopTime > 0 && now >= stopTime) {
            if (loop!=null) {
                loop.stop();
                this.loop = null;
            }
        }
    }

    public String summary() {

        @NotNull Emotion emotion = nar.emotion;

        //long now = nar.time();


        return
//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "
                  "rwrd=" + n2(rewardValue) + "\t"
                + "motv=" + n4(desireConf()) + " "
                + "hapy=" + n4(emotion.happy()-emotion.sad()) + " "
                + "busy=" + n4(emotion.busy.getSum()) + " "
                + "lern=" + n4(emotion.learning()) + " "
                + "strs=" + n4(emotion.stress.getSum()) + " "
                + "alrt=" + n4(emotion.alert.getSum()) + " "
                + " var=" + n4( varPct(nar) ) + " "
                   + "\t" + nar.index.summary()

//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());
                ;

    }


    protected void mission() {

        int dt = 1 + ticksBeforeObserve + ticksBeforeDecide;
        //this.curiosityAttention = reinforcementAttention / actions.size();


        /** set the sensor budget policy */
        int numSensors = sensors.size();

        /** represents the approx equivalent number of sensors which can be fully budgeted at any time */
        float activeSensors =
                //(float) Math.sqrt(numSensors); //HEURISTIC
                numSensors / 2f;

        for (SensorConcept sensor : sensors) {
            Term ts = sensor.term();
            sensor.pri(() -> {

                        final float gain = nar.priorityDefault(Symbols.BELIEF); //1f;

                        float cp = nar.conceptPriority(ts);

                        float p = gain/activeSensors - cp;

                        return Math.max(gain/numSensors, Math.min(1f, p));
                    }
            );
        }

        @NotNull Term what = $.$("?w"); //#w

        predictors.add(
                //what will soon imply reward R
                nar.ask($.impl(what, dt, happy.term()), '?', ETERNAL)
        );

        //what co-occurs with reward R
        predictors.add(
                nar.ask($.seq(what, dt, happy.term()), '?', ETERNAL)
        );
        predictors.add(
                nar.ask($.seq(what, dt, happy.term()), '?', now)
        );
        predictors.add( //+2 cycles ahead
                nar.ask($.seq(what, dt*2, happy.term()), '?', now)
        );


        for (Concept x : actions) {

            //quest for each action
            predictors.add(nar.ask(x, '@', ETERNAL));
            predictors.add(nar.ask(x, '@', now));

            //does action A co-occur with reward R?
            predictors.add(
                    nar.ask($.seq(x.term(), dt, happy.term()), '?', ETERNAL)
            );
            predictors.add(
                    nar.ask($.seq(x.term(), dt, happy.term()), '?', now)
            );



        }

        //System.out.println(predictors);

    }

    public NARLoop run(final int cycles) {
        return run(cycles, 0);
    }


    public NAgent runSync(final int cycles) {
        //run(cycles, 0).join();
        start();
        nar.run(cycles);
        return this;
    }

    public NARLoop run(final int cycles, int frameDelayMS) {

        start();

        this.stopTime = nar.time() + cycles;

        this.loop = new NARLoop(nar, frameDelayMS);


        return loop;

//        nar.next(); //step one for any mission() results to process first
//
//        for (int t = 0; t < cycles; t++) {
//            next();
//
//            if (frameDelayMS > 0)
//                Util.pause(frameDelayMS);
//        }
    }

    private void start() {
        if (this.loop!=null)
            throw new UnsupportedOperationException();

        ticksBeforeDecide = 0;
        ticksBeforeObserve = 0;

        nar.runLater(()->{
            init(nar);

            mission();

            nar.onFrame(nn -> next());
        });
    }

    protected void reinforce() {
        long now = nar.time();

        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));
        if (reinforcementAttention > 0) {

            boostBudget = UnitBudget.One.clone().multiplied(reinforcementAttention, 0.5f, 0.5f);
            curiosityBudget = UnitBudget.One.clone().multiplied(0, 0f, 0f);

            //boost(happy);
            //boost(happy); //boosted by the (happy)! task that is boosted below
            //boost(sad);

            float m = 0;
            int a = actions.size();
            for (MotorConcept c : actions) {
                Truth d = c.desire(now);
                if (d!=null)
                    m += d.confWeight();
            }
            motorDesireEvidence.addValue(m);



            float motorEpsilonProbability = epsilonProbability/a * (1f - (desireConf()/gamma));
            for (MotorConcept c : actions) {
                if (nar.random.nextFloat() < motorEpsilonProbability) {
                    nar.inputLater(
                        new GeneratedTask(c, Symbols.GOAL,
                            $.t(nar.random.nextFloat()
                            //Math.random() > 0.5f ? 1f : 0f
                            , gamma * gammaEpsilonFactor))

                    //in order to auto-destruct corectly, the task needs to remove itself from the taskindex too
                    /* {
                        @Override
                        public boolean onConcept(@NotNull Concept c) {
                            if (super.onConcept(c)) {
                                //self-destruct later
                                nar.runLater(()->{
                                    delete();
                                });
                                return true;
                            }
                            return false;
                        }
                    }*/.
                            present(now).budget(curiosityBudget).log("Curiosity"));

                }

                boost(c);
            }

            for (Task x : predictors)
                boost(x);


        }


    }

    public float desireConf() {
        return Math.min(1f, w2c((float)motorDesireEvidence.getMean()));
    }

    @Nullable
    protected Concept boost(Concept c) {

        new Activation(boostBudget, c, nar, 1) {

            @Override
            public void activate(@NotNull NAR nar, float activation) {
                linkTermLinks(c, alpha, nar);
                super.activate(nar, activation);
            }
        };

        return c;
        //return c;
        //return nar.activate(c, null);
    }


    private void boost(@NotNull Task t) {
        BudgetMerge.max.apply(t.budget(), boostBudget, 1);
        if (t.isDeleted())
            throw new RuntimeException();

        float REINFORCEMENT_DURABILITY = 0.9f;
        if (t.occurrence() != ETERNAL) {
            nar.inputLater(new GeneratedTask(t.term(), t.punc(), t.truth()).time(now, now)
                    .budget(boostBudget).log("Predictor"));
        } else {
            //re-use existing eternal task
            Activation a = new Activation(t, nar, 1f) {
                @Override
                public void linkTerms(Concept src, Term[] tgt, float scale, float minScale, @NotNull NAR nar) {
                    super.linkTerms(src, tgt, scale, minScale, nar);

                    linkTermLinks(src, scale, nar);
                }
            };
        }

    }

    public float rewardSum() {
        return rewardSum;
    }

}
