package nars;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.list.FasterList;
import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.Loop;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static nars.$.t;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent implements NSense, NAction {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);
    public static final int HAPPINESS_TERMLINK_CAPACITY_MULTIPLIER = 1;

    /**
     * general reward signal for this agent
     */
    @NotNull
    public final SensorConcept happy;

//    /**
//     * d(happy)/dt = change in happiness over time (ie. first-order difference of happiness signal)
//     */
//    @Nullable
//    public final SensorConcept joy;

    @NotNull
    public final FloatNormalized rewardNormalized;
    private final float actionBoost;
    private final String id;

    public final NAR nar;

    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<ActionConcept> actions = $.newArrayList();



    public float alpha, gamma;

    float curiosityFreqMin = 0.5f;
    float curiosityFreqMax = 1.5f;

    class CuriosityPhasor {
        public float freq, phase;
        public CuriosityPhasor() {
            Random r = nar.random;
            freq = curiosityFreqMin + (curiosityFreqMax - curiosityFreqMin) * r.nextFloat();
            phase = r.nextFloat() * (float)Math.PI;
        }

        public float next() {

            float mutateRate = (curiosityFreqMax - curiosityFreqMin)/20f;
            freq = Util.clamp(freq + (nar.random.nextFloat() - 0.5f) * mutateRate, curiosityFreqMin, curiosityFreqMax);

            return (float)Math.sin(freq * nar.time()/nar.time.dur()/(2*(float)Math.PI) + phase)/2f + 0.5f;
        }
    }

    final List<CuriosityPhasor> curiosityPhasor = $.newArrayList();

    public final FloatParam epsilonProbability = new FloatParam(0.1f);

    public final FloatParam gammaEpsilonFactor = new FloatParam(0.1f);

    //final int curiosityMonitorDuration; //frames
    final DescriptiveStatistics avgActionDesire;
    final DescriptiveStatistics rewardWindow;


    public float rewardValue;

    float predictorProbability = 1f;

    public final List<MutableTask> predictors = $.newArrayList();
    public final FloatParam predictorPriority = new FloatParam(1);

    public boolean trace = false;


    /**
     * >=0 : NAR frames that are computed between each Agent frame
     */
    public final int frameRate;

    protected long prev, now;




    //private float curiosityAttention;
    private float rewardSum = 0;

    public final FloatParam sensorPriority;
    public final FloatParam actionPriority;
    public final FloatParam rewardPriority;

    //private MutableFloat maxSensorPriority;

    public NAgent(@NotNull NAR nar) {
        this("", nar, 1);
    }

    public NAgent(String id, @NotNull NAR nar, int frameRate) {

        this.id = id;
        this.nar = nar;
        this.alpha = this.nar.confidenceDefault(BELIEF);
        this.gamma = this.nar.confidenceDefault(GOAL);
        this.frameRate = frameRate;

        this.sensorPriority = new FloatParam(alpha);
        this.actionPriority = new FloatParam(gamma);
        this.rewardPriority = new FloatParam(gamma);

        this.prev = nar.time();

        this.actionBoost = gamma;


        this.rewardNormalized = new FloatPolarNormalized(() -> rewardValue);


        this.happy = new SensorConcept(
                //"happy" + "(" + nar.self + ")", nar,
                id.isEmpty() ? $.p("happy") : $.func("happy", id),
                nar,
                rewardNormalized,
                (x) -> t(x, alpha)
        );

        int curiosityMonitorDuration = Math.round((1 + nar.time.dur())); //TODO handle changing duration value
        avgActionDesire = new DescriptiveStatistics(curiosityMonitorDuration);
        rewardWindow = new DescriptiveStatistics(curiosityMonitorDuration);

        /*
        joy = new SensorConcept(
                //"joy" + "(" + nar.self + ")", nar,
                "change(" + happy.term() + ")",
                nar,
                new FloatPolarNormalized(
                        new FirstOrderDifferenceFloat(
                                () -> nar.time(), () -> rewardValue
                        )
                ),
                (x) -> t(x, rewardConf)
        );
        */

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

        prev = now;
        now = nar.time();

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
        Time time = (Time) nar.time;
        if (time instanceof FrameTime)
            ((FrameTime) time).tick(t); //resume clock for the last cycle before repeating
    }

    private void doFrame() {
        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));



        float r = rewardValue = act();
        if (r == r) {
            rewardSum += r;
            rewardWindow.addValue(rewardValue);
        }


        /** safety valve: if overloaded, enter shock / black out and do not receive sensor input */
        float load = nar.exe.load();
        if (load < 1) {

            happy.run();
            //joy.run();

            nar.runLater(sensors, SensorConcept::run, 4);

            predict();

            updateActions();

            curiosity();

        } else {
            logger.warn("sensor overwhelm: load={}",load);
        }


        if (trace)
            logger.info(summary());
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
                new StringBuilder().append("rwrd=").append(n2(rewardValue)).append("\t").append("motv=").append(n4(desireConf())).append(" ").append("hapy=").append(n4(emotion.happy() - emotion.sad())).append(" ").append("busy=").append(n4(emotion.busyMass.getSum())).append(" ").append("lern=").append(n4(emotion.learning())).append(" ").append("strs=").append(n4(emotion.stress.getSum())).append(" ").append("alrt=").append(n4(emotion.alert.getSum())).append(" ").append(" var=").append(n4(varPct(nar))).append(" ").append("\t").append(nar.concepts.summary()).toString()

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

        //maxSensorPriority = new MutableFloat(nar.priorityDefault(BELIEF));

//        Iterable<? extends WiredCompoundConcept.Prioritizable> p = Iterables.concat(
//                sensors,
//                actions,
//                newArrayList(happy, joy)
//        );
//        SensorConcept.activeAttention(p, minSensorPriority, maxSensorPriority, nar);

        //in separate banks so they dont compete with eachother for attention:
        //SensorConcept.activeAttention(sensors, new MutableFloat(maxSensorPriority.floatValue()/sensors.size()), maxSensorPriority, nar);
//        SensorConcept.activeAttention(sensors, new MutableFloat(maxSensorPriority.floatValue()/sensors.size()), maxSensorPriority, nar);
//        SensorConcept.activeAttention(actions, new MutableFloat(maxSensorPriority.floatValue()/actions.size()), maxSensorPriority, nar);
//        SensorConcept.activeAttention(newArrayList(happy, joy), new MutableFloat(maxSensorPriority.floatValue()/2f), maxSensorPriority, nar);

        sensors.forEach(s -> s.pri(sensorPriority));
        actions.forEach(s -> s.pri(actionPriority));
        happy.pri(rewardPriority);
        //joy.pri(rewardPriority);

        //SensorConcept.flatAttention(p, minSensorPriority);

        for (int i = 0; i < numActions; i++) {
            curiosityPhasor.add(new CuriosityPhasor());
        }

        //@NotNull Term what = $.$("?w"); //#w
        //@NotNull Term what = $.$("#s"); //#w

        @NotNull Compound happiness = happy.term();

        {


            predictors.add(
                    new MutableTask(happy, '!', 1f, nar.confidenceDefault('!'))
                            .budgetByTruth(rewardPriority.floatValue())
                            .eternal()
                            //.present(nar)
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

        int dur = (int) Math.ceil(nar.time.dur());

        for (Concept a : actions) {
            Term action = a.term();




            ((FasterList)predictors).addAll(



//                    new PredictionTask($.impl(action, dur, happiness), '?').time(nar, dur),
//                    new PredictionTask($.impl($.neg(action), dur, happiness), '?').time(nar, dur),

//                    new PredictionTask($.impl($.parallel(action, $.varQuery(1)), happiness), '?')
//                            .eternal(),
//                            //.time(nar, dur),
//                    new PredictionTask($.impl($.parallel($.neg(action), $.varQuery(1)), happiness), '?')
//                            .eternal(),
//                            //.time(nar, dur)

//                    new PredictionTask($.impl(action, dur, happiness), '?')
//                            .time(nar.time()),
//                    new PredictionTask($.impl($.neg(action), dur, happiness), '?')
//                            .time(nar.time()),

//                    new PredictionTask($.impl(action, dur, $.varQuery(1)), '?')
//                            .time(nar.time())

                    new PredictionTask($.seq(action, dur, happiness), '?')
                            .time(nar.time()),
                    new PredictionTask($.seq($.neg(action), dur, happiness), '?')
                            .time(nar.time()),
                    new PredictionTask($.seq(action, dur, $.neg(happiness)), '?')
                            .time(nar.time()),
                    new PredictionTask($.seq($.neg(action), dur, $.neg(happiness)), '?')
                            .time(nar.time())

//                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq(action, dur, happiness)), '?').eternal(),
//                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq($.neg(action), dur, happiness)), '?').eternal()



//                    new PredictionTask($.seq(action, dur, varQuery(1)), '@')
//                        .present(nar),
//
//
//                    new PredictionTask($.seq($.neg(action), dur, varQuery(1)), '@')
//                        .present(nar)

//                    new MutableTask($.impl(action, dur, happiness), '?', null)
//                            .present(nar),
//                            //.eternal(),
//                    new MutableTask($.impl($.neg(action), dur, happiness), '?', null)
//                            .present(nar)
//                            //.eternal()


                    //new MutableTask($.seq($.varQuery(0), dur, action), '?', null).eternal(),
                    //new MutableTask($.impl($.varQuery(0), dur, action), '?', null).eternal(),

                    //new MutableTask($.impl($.parallel($.varDep(0), action), dur, happiness), '?', null).time(now, now + dur),
                    //new MutableTask($.impl($.parallel($.varDep(0), $.neg( action )), dur, happiness), '?', null).time(now, now + dur)
            );

        }

//        predictors.add(
//                new MutableTask($.seq($.varQuery(0 /*"what"*/), dur, happiness), '?', null).time(now, now)
//        );

        System.out.println(Joiner.on('\n').join(predictors));
    }

    @NotNull
    public NAgent run(final int cycles) {

        init();

        nar.runLater(() -> {
            nar.onCycle(nn -> frame());
        });
        nar.run(cycles);
        return this;
    }

    @NotNull
    public Loop runRT(float fps) {
        return runRT(fps, -1);
    }
    /**
     * run Real-time
     */
    @NotNull
    public Loop runRT(float fps, int stopTime) {

        init();

        return new Loop("agent", fps) {

            @Override
            public void next() {

                prev = now;
                now = nar.time();

                if (stopTime > 0 && now > stopTime)
                    stop();

                doFrame();

                nar.run(1);

            }
        };

    }


    private void curiosity() {
        //Budget curiosityBudget = Budget.One.clone().multiplied(minSensorPriority.floatValue(), 0.5f, 0.9f);


        float gammaEpsilonFactor = this.gammaEpsilonFactor.floatValue();

        for (int i = 0, actionsSize = actions.size(); i < actionsSize; i++) {
            ActionConcept action = actions.get(i);

            float motorEpsilonProbability = epsilonProbability.floatValue() * (1f - Math.min(1f, action.goalConf(now, nar.time.dur(), 0) / gamma));


            if (nar.random.nextFloat() < motorEpsilonProbability) {

                //logger.info("curiosity: {} conf={}", action, action.goalConf(now, 0));

                float f = curiosityPhasor.get(i).next();
                float cc = gamma * gammaEpsilonFactor;// * (1f - Math.min(1f, action.goalConf(now, 0) / gamma));
                Truth t = $.t(f, cc);

                //action.biasDesire(t);

                if (t!=null) {
                    nar.input(
                            new GeneratedTask(action, GOAL, t)
                                    .time(now, now - Math.round(nar.time.dur()), now)
                                    .budgetByTruth(action.pri.asFloat())
                                    .log("Curiosity")
                    );
                }

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

            boost(action, actionBoost);
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

            Truth d = a.goals().truth(now, nar.time.dur());
            if (d != null)
                m += d.evi();
        }
        avgActionDesire.addValue(w2c(m/n)); //resulting from the previous frame

        nar.runLater(actions, ActionConcept::run, 1);
    }


    protected void predict() {

        float pri =
                predictorPriority.floatValue();
                //UtilityFunctions.aveAri(nar.priorityDefault('.'), nar.priorityDefault('!'))
                       ///* / (predictors.size()/predictorProbability)*/ * predictorPriFactor;

        float lookAhead = 8;

        if (pri > 0) {
            //long frameDelta = now-prev;
            float dur = nar.time.dur();
            for (int i = 0, predictorsSize = predictors.size(); i < predictorsSize; i++) {
                predictors.set(i, boost(predictors.get(i), pri, dur, lookAhead));
            }
        }

//        Task beHappy = new MutableTask(happy, '!', 1f, nar).time(Tense.Present, nar).budgetByTruth(1f);
//        beHappy.normalize(nar);
//        for (ActionConcept a : actions) {
//            a.tasklinks().put(beHappy);
//        }

//        Compound term = happy.term();
//        happy.termlinks().forEach(tl -> {
//            Term x = tl.get();
//            if (x.op().temporal) {
//                if (((Compound)x).containsTerm(term)) {
//                    System.out.println(x);
//                }
//            }
////            if (x.op()==IMPL) {
////                Compound i = (Compound)x;
////                int dt = i.dt();
////                if (i.term(1).equals(term)) {
////                    System.out.println(i);
////                }
////            }
//        });
    }

    public float desireConf() {
        return Math.min(1f, ((float) avgActionDesire.getMean()));
    }

    @Nullable
    protected Concept boost(Concept c, float amount) {

        //HACK
        if (nar instanceof Default) {
            ((Default) nar).core.active.add(c, amount);
        }

//        new Activation(boostBudget, c, nar, 1) {
//
//            @Override
//            public void commit(float scale) {
//                linkTermLinks(c, scale); //activate all termlinks of this concept
//                super.commit(scale);
//            }
//        };

        return c;
    }


    private MutableTask boost(@NotNull MutableTask t, float p, float dur, float lookAhead /* in multiples of dur */) {

        if (nar.random.nextFloat() > predictorProbability)
            return t;

        MutableTask s;
        char pp = t.punc();
        if (t.start() != ETERNAL) {
            s = (t instanceof PredictionTask) ? new PredictionTask(t.term(), pp) : new MutableTask(t.term(), pp, t.truth());
            s.budgetByTruth(p, nar)
                .time(now, now + Math.round(dur * lookAhead * nar.random.nextFloat()))
                .log("Agent Predictor");

            //s.evidence(t)
            nar.input(s);
            return s;
        } else if (t.isDeleted()) {
            s = (t instanceof PredictionTask) ?
                    new PredictionTask(t.term(), pp) :
                    new MutableTask(t.term(), pp, t.truth());

            s.budgetByTruth(p, nar).eternal().log("Agent Predictor");

            t.delete(); //allow the new one to replace it on re-activation

            nar.input(s);
            return s;
        } else {
            t.budgetByTruth(p, nar).eternal().log("Agent Predictor");
            nar.input(t);
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

    public static class PredictionTask extends GeneratedTask {

        public PredictionTask(@NotNull Termed<Compound> term, char punct) {
            super(term, punct, null);
            assert(punct == Op.QUESTION || punct == Op.QUEST);
        }

        /*
        @Override
        public Task onAnswered(Task answer, NAR nar) {
            if (!answer.isDeleted()) {
                long reactionTime = answer.creation() - creation();
                long lag = !answer.isEternal() ? nar.time() - answer.start() : 0;
                //nar.logger.info("Prediction:\t{}\n\t{}\tlag={},{}", this, answer, reactionTime, lag);
            }
            return answer;
        }
        */
    }

}
