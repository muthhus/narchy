package nars;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.list.FasterList;
import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import jcog.net.UDP;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.term.Term;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static nars.$.*;
import static nars.Op.*;
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
    private final Term id;

    public final NAR nar;

    public final List<SensorConcept> sensors = newArrayList();
    public final List<ActionConcept> actions = newArrayList();


    float lookAhead = 12;

    public float alpha, gamma;

    float curiosityFreqMin = 0.5f;
    float curiosityFreqMax = 1.5f;

    public void stop() {
        nar.stop();
    }

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

    final List<CuriosityPhasor> curiosityPhasor = newArrayList();

    public final FloatParam epsilonProbability = new FloatParam(0.1f);

    public final FloatParam gammaEpsilonFactor = new FloatParam(0.1f);

    //final int curiosityMonitorDuration; //frames
    final DescriptiveStatistics avgActionDesire;
    final DescriptiveStatistics rewardWindow;


    public float rewardValue;

    float predictorProbability = 1f;

    public final List<Task> predictors = newArrayList();
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
        this(id.isEmpty() ? null : the(id), nar, frameRate);
    }

    public NAgent(@Nullable Term id, @NotNull NAR nar, int frameRate) {

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
                id == null ? p("happy") : func("happy", id),
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
            ((FrameTime) time).cycle(t); //resume clock for the last cycle before repeating
    }

    private void doFrame() {
        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));



        float r = rewardValue = act();
        if (r == r) {
            rewardSum += r;
            rewardWindow.addValue(rewardValue);
        }


        /** safety valve: if overloaded, enter shock / black out and do not receive sensor input */
//        float load = nar.exe.load();
//        if (load < 1) {

            predict();

            updateActions();

            curiosity();

            nar.input(
                Streams.concat(Stream.of(happy), sensors.stream(), actions.stream()).map(f -> f.apply(nar))
            );


//        } else {
//            logger.warn("sensor overwhelm: load={}",load);
//        }


        if (trace)
            logger.info(summary());
    }


    final UDP udp = new UDP();

    /*
    https://docs.influxdata.com/influxdb/v0.9/write_protocols/udp/
    To write, just send newline separated line protocol over UDP. For better performance send batches of points rather than multiple single points.
    $ echo "cpu value=1"> /dev/udp/localhost/8089
         */
    public void sendInfluxDB(String host, int port) {
        //String s = "cpu,host=server01,region=uswest load=" + (Math.random() * 100) +  " " + System.currentTimeMillis();

        //SELECT mean("hapy") FROM "cpu"
        //SELECT mean("busyVol") FROM "cpu" WHERE $timeFilter GROUP BY time($interval) fill(null)

        @NotNull Emotion e = nar.emotion;
        String s = "cpu " +
                 "busyVol=" + e.busyVol.getSum() +
                ",busyPri=" + e.busyPri.getSum() +
                ",hapy=" + e.happy.getSum() +
                ",sad=" + e.sad.getSum();
                //" " + System.nanoTime();

        udp.out(s, host, port);
    }

    @NotNull
    public String summary() {

        //sendInfluxDB("localhost", 8089);

        return "rwrd=" + n2(rewardValue) +
                " motv=" + n4(desireConf()) +
                " var=" + n4(varPct(nar)) + "\t" + nar.concepts.summary() + " " +
                nar.emotion.summary();
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

        predictors.add(
            goal(happiness, t(1f, nar.confidenceDefault(GOAL)), ETERNAL)
        );


//        predictors.addAll(
//                //what will imply reward
//                new TaskBuilder($.equi(what, dt, happiness), '?', null).time(now, now),
//                //new TaskBuilder($.equi(sth, dt, happiness), '.', null).time(now,now),
//
//                //what will imply non-reward
//                //new TaskBuilder($.equi(what, dt, $.neg(happiness)), '?', null).time(now, now),
//                //new TaskBuilder($.equi(sth, dt, $.neg(happiness)), '.', null).time(now,now),
//
//                //what co-occurs with reward
//                new TaskBuilder($.parallel(what, happiness), '?', null).time(now, now)
//
//                //what co-occurs with non-reward
//                //new TaskBuilder($.parallel(what, $.neg(happiness)), '?', null).time(now, now)
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

                    question(impl(action, dur, happiness), nar.time()),
                    question(impl(neg(action), dur, happiness), nar.time()),
                    //question(impl(neg(action), dur, varQuery(1)), nar.time()),
                    question(seq(action, dur, happiness), nar.time()),
                    question(seq(neg(action), dur, happiness), nar.time()),
                    question(seq(action, dur, neg(happiness)), nar.time()),
                    question(seq(neg(action), dur, neg(happiness)), nar.time())

//                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq(action, dur, happiness)), '?').eternal(),
//                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq($.neg(action), dur, happiness)), '?').eternal()



//                    new PredictionTask($.seq(action, dur, varQuery(1)), '@')
//                        .present(nar),
//
//
//                    new PredictionTask($.seq($.neg(action), dur, varQuery(1)), '@')
//                        .present(nar)

//                    new TaskBuilder($.impl(action, dur, happiness), '?', null)
//                            .present(nar),
//                            //.eternal(),
//                    new TaskBuilder($.impl($.neg(action), dur, happiness), '?', null)
//                            .present(nar)
//                            //.eternal()


                    //new TaskBuilder($.seq($.varQuery(0), dur, action), '?', null).eternal(),
                    //new TaskBuilder($.impl($.varQuery(0), dur, action), '?', null).eternal(),

                    //new TaskBuilder($.impl($.parallel($.varDep(0), action), dur, happiness), '?', null).time(now, now + dur),
                    //new TaskBuilder($.impl($.parallel($.varDep(0), $.neg( action )), dur, happiness), '?', null).time(now, now + dur)
            );

        }

//        predictors.add(
//                new TaskBuilder($.seq($.varQuery(0 /*"what"*/), dur, happiness), '?', null).time(now, now)
//        );

        //System.out.println(Joiner.on('\n').join(predictors));
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
                Truth t = t(f, cc);

                //action.biasDesire(t);

                if (t!=null) {
                    nar.input(
                            goal(action.term(), t, now, now - Math.round(nar.time.dur()))
                                //.budgetByTruth(action.pri.asFloat(), nar)
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

            //boost(action, actionBoost);
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

    }


    protected void predict() {

        float pri =
                predictorPriority.floatValue();
                //UtilityFunctions.aveAri(nar.priorityDefault('.'), nar.priorityDefault('!'))
                       ///* / (predictors.size()/predictorProbability)*/ * predictorPriFactor;

        if (pri > 0) {
            //long frameDelta = now-prev;
            float dur = nar.time.dur();
            nar.input(
                IntStream.range(0, predictors.size()).mapToObj(i -> {
                    Task x = predictors.get(i);
                    Task y = boost(x, pri, dur, lookAhead);
                    if (x!=y) {
                        predictors.set(i, y); //predictor changed or needs re-input
                        //return y;
                    }
                    return y;
                    //return null; //dont re-input this predictor
                })
            );
        }

//        Task beHappy = new TaskBuilder(happy, '!', 1f, nar).time(Tense.Present, nar).budgetByTruth(1f);
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

//    @Nullable
//    protected Concept boost(Concept c, float amount) {
//
//        //HACK
//        if (nar instanceof Default) {
//            ((Default) nar).core.active.add(c, amount);
//        }
//
////        new Activation(boostBudget, c, nar, 1) {
////
////            @Override
////            public void commit(float scale) {
////                linkTermLinks(c, scale); //activate all termlinks of this concept
////                super.commit(scale);
////            }
////        };
//
//        return c;
//    }


    private Task boost(@NotNull Task t, float p, float dur, float lookAhead /* in multiples of dur */) {

        if (nar.random.nextFloat() > predictorProbability)
            return t;

        byte pp = t.punc();
        if (t.start() != ETERNAL) {

            return prediction(t.term(), t.punc(), t.truth(), now, now + Math.round(dur * lookAhead * nar.random.nextFloat()));

        } else if (t.isDeleted()) {

            return prediction(t.term(), t.punc(), t.truth(), ETERNAL, ETERNAL);

        } else {
            t.budget(p, nar).log("Agent Predictor");
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


    public Task goal(@NotNull Compound term, Truth truth, long when) {
        return prediction(term, GOAL, truth, when, when);
    }

    public Task goal(@NotNull Compound term, Truth truth, long start, long end) {
        return prediction(term, GOAL, truth, start, end);
    }

    public Task question(@NotNull Compound term, long when) {
        return prediction(term, QUESTION, null, when, when);
    }

    public Task prediction(@NotNull Compound term, byte punct, Truth truth, long start, long end) {
        return new ImmutableTask(term, punct, truth, nar.time(), start, end, new long[] { nar.time.nextStamp() } )
                .budget(predictorPriority.floatValue(), nar)
                .log("Agent Predictor");
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
