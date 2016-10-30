package nars;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.budget.BudgetFunctions;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.concept.WiredCompoundConcept;
import nars.link.BLink;
import nars.nal.UtilityFunctions;
import nars.nar.Default;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.util.Loop;
import nars.util.data.list.FasterList;
import nars.util.math.FirstOrderDifferenceFloat;
import nars.util.math.FloatNormalized;
import nars.util.math.FloatPolarNormalized;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nars.$.t;
import static nars.Op.IMPL;
import static nars.Symbols.BELIEF;
import static nars.Symbols.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;
import static nars.util.Texts.n2;
import static nars.util.Texts.n4;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent implements NSense, NAction {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    /**
     * general reward signal for this agent
     */
    @NotNull
    public final SensorConcept happy;

    /**
     * d(happy)/dt = change in happiness over time (ie. first-order difference of happiness signal)
     */
    @Nullable
    public final SensorConcept joy;

    @NotNull
    public final FloatNormalized rewardNormalized;

    public NAR nar;

    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<ActionConcept> actions = $.newArrayList();

    public float alpha, gamma, epsilonProbability = 0.02f;
    @Deprecated
    public float gammaEpsilonFactor = 0.5f;

    final int curiosityMonitorDuration = 8; //frames
    final DescriptiveStatistics avgActionDesire = new DescriptiveStatistics(curiosityMonitorDuration);
    final DescriptiveStatistics rewardWindow = new DescriptiveStatistics(curiosityMonitorDuration);


    public float rewardValue;

    float predictorProbability = 1f;

    protected final List<MutableTask> predictors = $.newArrayList();
    private float predictorPriFactor = 0.25f;

    public boolean trace = false;


    /**
     * >=0 : NAR frames that are computed between each Agent frame
     */
    public final int frameRate;

    protected long now;


    //private float curiosityAttention;
    private float rewardSum = 0;
    private MutableFloat maxSensorPriority;

    public NAgent(@NotNull NAR nar) {
        this(nar, 1);
    }

    public NAgent(@NotNull NAR nar, int frameRate) {

        this.nar = nar;
        alpha = this.nar.confidenceDefault(BELIEF);
        gamma = this.nar.confidenceDefault(GOAL);
        this.frameRate = frameRate;

        float rewardConf = alpha;

        rewardNormalized = new FloatPolarNormalized(() -> rewardValue);

        happy = new SensorConcept("" +
                //"happy" + "(" + nar.self + ")", nar,
                "(happy)",
                nar,
                rewardNormalized,
                (x) -> t(x, rewardConf)
        );


        joy = new SensorConcept(
                //"joy" + "(" + nar.self + ")", nar,
                "(joy)",
                nar,
                new FloatPolarNormalized(
                        new FirstOrderDifferenceFloat(
                                () -> nar.time(), () -> rewardValue
                        )
                ),
                (x) -> t(x, rewardConf)
        );

    }

    @NotNull
    @Override
    public final Collection<SensorConcept> sensors() {
        return sensors;
    }

    @NotNull
    @Override
    public final Collection<ActionConcept> actions() {
        return actions;
    }

    @Override
    public final NAR nar() {
        return nar;
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    public void sense(SensorConcept... s) {
        sense(Lists.newArrayList(s));
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    public void sense(@NotNull Iterable<SensorConcept> s) {
        Iterables.addAll(sensors, s);
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    public void action(ActionConcept... s) {
        action(Lists.newArrayList(s));
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    public void action(@NotNull Iterable<ActionConcept> s) {
        Iterables.addAll(actions, s);
    }


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();

    int actionFrame = 0;

    protected void frame() {

        int phase = (actionFrame++) % (frameRate);
        if (phase == 0) {
            ticks(0); //freeze clock
        }
        if ((phase == frameRate - 1) || (frameRate < 2)) {
            ticks(1);
            doFrame();
        }


    }

    private void ticks(int t) {
        Clock clock = (Clock) nar.clock;
        if (clock instanceof FrameClock)
            ((FrameClock) clock).tick(t); //resume clock for the last cycle before repeating
    }

    private void doFrame() {
        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));
        now = nar.time();

        float r = rewardValue = act();
        if (r == r) {
            rewardSum += r;
            rewardWindow.addValue(rewardValue);
        }

        /** safety valve: if overloaded, enter shock / black out and do not receive sensor input */
        if (nar.exe.load() < 1) {


            predict();

            nar.runLater(sensors, SensorConcept::run, 4);
            happy.run();
            joy.run();

            updateActions();

            curiosity();

        } else {
            logger.warn("sensor overwhelm");
        }


        if (trace) {
            logger.info(summary());
        }
    }


    @NotNull
    public String summary() {

        @NotNull Emotion emotion = nar.emotion;

        //long now = nar.time();


        return
//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "
                new StringBuilder().append("rwrd=").append(n2(rewardValue)).append("\t").append("motv=").append(n4(desireConf())).append(" ").append("hapy=").append(n4(emotion.happy() - emotion.sad())).append(" ").append("busy=").append(n4(emotion.busy.getSum())).append(" ").append("lern=").append(n4(emotion.learning())).append(" ").append("strs=").append(n4(emotion.stress.getSum())).append(" ").append("alrt=").append(n4(emotion.alert.getSum())).append(" ").append(" var=").append(n4(varPct(nar))).append(" ").append("\t").append(nar.concepts.summary()).toString()

//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());
                ;

    }


    protected void init() {


        //this.curiosityAttention = reinforcementAttention / actions.size();


        /** set the sensor budget policy */
        int numSensors = sensors.size();
        int numActions = actions.size();

//        /** represents the approx equivalent number of sensors which can be fully budgeted at any time */
//        float activeSensors =
//                //(float) Math.sqrt(numSensors); //HEURISTIC
//                numSensors / 2f;

        //minSensorPriority = new MutableFloat(Param.BUDGET_EPSILON * 4);

        maxSensorPriority = new MutableFloat(nar.priorityDefault(BELIEF));

//        Iterable<? extends WiredCompoundConcept.Prioritizable> p = Iterables.concat(
//                sensors,
//                actions,
//                newArrayList(happy, joy)
//        );
//        SensorConcept.activeAttention(p, minSensorPriority, maxSensorPriority, nar);

        //in separate banks so they dont compete with eachother for attention:
        SensorConcept.activeAttention(sensors, new MutableFloat(maxSensorPriority.floatValue()/sensors.size()), maxSensorPriority, nar);
        SensorConcept.activeAttention(actions, new MutableFloat(maxSensorPriority.floatValue()/actions.size()), maxSensorPriority, nar);
        SensorConcept.activeAttention(newArrayList(happy, joy), new MutableFloat(maxSensorPriority.floatValue()/2f), maxSensorPriority, nar);

        //SensorConcept.flatAttention(p, minSensorPriority);


        //@NotNull Term what = $.$("?w"); //#w
        //@NotNull Term what = $.$("#s"); //#w

        @NotNull Compound happiness = happy.term();

        {
            float rewardGamma =
                    1.0f - Param.TRUTH_EPSILON
                    //1.0f
                    //gamma
                    ;


//            float happinssDurability =
//                    nar.durabilityDefault(Symbols.GOAL);

            predictors.add(
                    new MutableTask(happy, '!', 1f, rewardGamma)
                            .eternal()
                    //.present(nar.time()+dt)
            );
//                    happy.desire($.t(1f, rewardGamma),
//                            nar.priorityDefault(Symbols.GOAL),
//                            happinssDurability));
        }

//        predictors.addAll(
//                //what will imply reward
//                new MutableTask($.equi(what, dt, happiness), '?', null).time(now, now),
//                //new MutableTask($.equi(sth, dt, happiness), '.', null).time(now,now),
//
//                //what will imply non-reward
//                //new MutableTask($.equi(what, dt, $.neg(happiness)), '?', null).time(now, now),
//                //new MutableTask($.equi(sth, dt, $.neg(happiness)), '.', null).time(now,now),
//
//                //what co-occurs with reward
//                new MutableTask($.parallel(what, happiness), '?', null).time(now, now)
//
//                //what co-occurs with non-reward
//                //new MutableTask($.parallel(what, $.neg(happiness)), '?', null).time(now, now)
//        );

//        predictors.add(
//                nar.ask($.seq(what, dt, happy.term()), '?', now)
//        );
//        predictors.add( //+2 cycles ahead
//                nar.ask($.seq(what, dt*2, happy.term()), '?', now)
//        );


        for (Concept a : actions) {
            Term action = a.term();

            int lookahead = 1;
            for (int i = 0; i < lookahead; i++) {
//                predictors.addAll(
//                    new MutableTask($.seq(action, 1+lookahead, happiness), '?', null).eternal()
//                    //new MutableTask($.impl(action, 1+lookahead, happiness), '?', null).eternal()
//                    //new MutableTask($.impl(action, dt, happiness), '?', null).time(now, then),
//                    //new MutableTask(action, '@', null).time(now, then)
//                );
            }

            ((FasterList)predictors).addAll(
                    new MutableTask($.seq($.varQuery(0), 1, action), '?', null).eternal(),
                    new MutableTask($.impl($.varQuery(0), 1, action), '?', null).eternal(),
                    new MutableTask($.seq(action, 1, happiness), '?', null).eternal(),
                    new MutableTask($.impl(action, 1, happiness), '?', null).eternal()
            );

        }

        predictors.add(
                new MutableTask($.seq($.varQuery(0 /*"what"*/), 1, happiness), '?', null).time(now, now)
        );

        System.out.println(Joiner.on('\n').join(predictors));
    }

    @NotNull
    public NAgent run(final int cycles) {

        init();

        nar.runLater(() -> {

            nar.onFrame(nn -> frame());
        });
        nar.run(cycles);
        return this;
    }

    /**
     * run Real-time
     */
    @NotNull
    public Loop runRT(float fps) {

        init();

        return new Loop("agent", fps) {

            @Override
            public void next() {
                doFrame();

                now = nar.time();

                nar.run(1);

                now = nar.time();

            }
        };

//        new Thread(()->{
//
//            while (true) {
//
//                doFrame();
//
//                now = nar.time();
//
//                nar.run(1);
//
//                now = nar.time();
//
//                if (fps < 500) {
//                    try {
//                        Thread.sleep((long) (1000f / fps));
//                    } catch (InterruptedException e) {
//                    }
//                }
//            }
//        }).start();

    }


    private void curiosity() {
        //Budget curiosityBudget = Budget.One.clone().multiplied(minSensorPriority.floatValue(), 0.5f, 0.9f);


        for (ActionConcept c : actions) {

            float motorEpsilonProbability = epsilonProbability * (1f - Math.min(1f, c.goalConf(now, 0) / gamma));

            if (nar.random.nextFloat() < motorEpsilonProbability) {

                logger.info("curiosity: {} conf={}", c, c.goalConf(now, 0));

                nar.inputLater(
                        new GeneratedTask(c, GOAL,
                                $.t(nar.random.nextFloat()
                                        //Math.random() > 0.5f ? 1f : 0f
                                        , Math.max(nar.truthResolution.floatValue(), (nar.random.nextFloat() * gamma * gammaEpsilonFactor))))
                                .time(now, now).budgetByTruth(c.pri.asFloat(),
                                Math.max(nar.durMin.floatValue(), gammaEpsilonFactor * nar.durabilityDefault(GOAL))).log("Curiosity"));

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
                }*/

            }

            //boost(c);
        }
    }

    private void updateActions() {
        float m = 0;
        int n = actions.size();
        for (ActionConcept a : actions) {

//            Term at = a.term();
//
//            for (BLink<Term> x : a.termlinks()) {
//                Term t = x.get();
//                System.out.println(t + " -> " + a);
//                if (t instanceof Compound) {
//                    if (t.op() == IMPL && ((Compound)t).term(1).equals(at)) {
//                        System.out.println("\t***");
//                    } else {
//
//                    }
//                }
//            }

            Truth d = a.desire();
            if (d != null)
                m += d.confWeight();
        }
        m /= n;
        avgActionDesire.addValue(w2c(m)); //resulting from the previous frame

        nar.runLater(actions, ActionConcept::run, 1);
    }

    protected void predict() {

        float pri =
                UtilityFunctions.aveAri(nar.priorityDefault('.'), nar.priorityDefault('!'))
                       /* / (predictors.size()/predictorProbability)*/ * predictorPriFactor;

        for (int i = 0, predictorsSize = predictors.size(); i < predictorsSize; i++) {
            predictors.set(i, boost(predictors.get(i)));
        }
    }

    public float desireConf() {
        return Math.min(1f, ((float) avgActionDesire.getMean()));
    }

//    @Nullable
//    protected Concept boost(Concept c) {
//
//        new Activation(boostBudget, c, nar, 1) {
//
//            @Override
//            public void commit(float scale) {
//                linkTermLinks(c, scale); //activate all termlinks of this concept
//                super.commit(scale);
//            }
//        };
//
//        return c;
//    }


    private MutableTask boost(@NotNull MutableTask t) {

        if (nar.random.nextFloat() > predictorProbability)
            return t;

        MutableTask s;
        char pp = t.punc();
        if (t.occurrence() != ETERNAL) {
            s = new GeneratedTask(t.term(), pp, t.truth())
                    .time(now, now + (t.occurrence() - t.creation()));

            s.evidence(t)
                    .log("Agent Predictor");

            nar.inputLater(s);
            return s;
        } else {


            if (t.isDeleted()) {
                //TODO check if dur or qua changed?
                t.budgetSafe(nar.priorityDefault(pp),
                             nar.durabilityDefault(pp),
                             t.isQuestOrQuestion() ? nar.qualityDefault(pp) : BudgetFunctions.truthToQuality(t));
            }

            nar.inputLater(t);
            return t;
        }


    }

    public float rewardSum() {
        return rewardSum;
    }

    public static float varPct(NAR nar) {
        if (nar instanceof Default) {
            DoubleSummaryStatistics is = new DoubleSummaryStatistics();
            nar.forEachActiveConcept(xx -> {

                if (xx != null) {
                    Term tt = xx.term();
                    float v = tt.volume();
                    int c = tt.complexity();
                    is.accept((v - c) / v);
                }

            });

            return (float) is.getAverage();
        }
        return Float.NaN;
    }


}
