package nars;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import jcog.Loop;
import jcog.data.FloatParam;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.math.FloatPolarNormalized;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.mix.PSink;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.control.ConceptFire;
import nars.nar.Default;
import nars.table.EternalTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
abstract public class NAgent implements NSense, NAct {

    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    /**
     * identifies this environment instance
     **/
    public final Term id;

    public final NAR nar;


    public final List<SensorConcept> sensors = newArrayList();

    public final List<ActionConcept> actions = newArrayList();

    /**
     * the general reward signal for this agent
     */
    @NotNull
    public final SensorConcept happy;


    /**
     * lookahead time in durations (multiples of duration)
     */
    public final FloatParam predictAheadDurs = new FloatParam(2, 1, 32);


    //public final FloatParam predictorProbability = new FloatParam(1f);
    private final PSink<Object, ITask> sense;
    private final PSink<Object, ITask> ambition;
    private final PSink<Object, ITask> predict;
    private final PSink<Object, ITask> motor;


    private boolean initialized;


    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public final FloatParam curiosity;


    /** prediction templates */
    public final List<Task> p = newArrayList();

    public AtomicBoolean enabled = new AtomicBoolean(true);

    public boolean trace = false;

    protected long now;

    public float rewardSum = 0;

    /**
     * range: -1..+1
     */
    public float reward;
    private Loop senseAndMotorLoop;
    private Loop predictLoop;
    final private ConceptFire fireHappy;

    public NAgent(@NotNull NAR nar) {
        this("", nar);
    }

    public NAgent(@NotNull String id, @NotNull NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    public NAgent(@Nullable Term id, @NotNull NAR nar) {

        this.id = id;
        this.nar = nar;

        this.now = ETERNAL; //not started

        this.happy = new SensorConcept(
                id == null ? p("happy") : $.inh( Atomic.the("happy"), id),
                nar,
                new FloatPolarNormalized(() -> reward),

                (x) -> t(x, alpha())

//                (x) -> {
//                    if (x > 0.5f + Param.TRUTH_EPSILON) {
//                        return t(1f, alpha() * (x - 0.5f) * 2f);
//                    } else if (x < 0.5f - Param.TRUTH_EPSILON) {
//                        return t(0f, alpha() * (0.5f - x) * 2f);
//                    } else {
//                        return t(0.5f, alpha());
//                    }
//                }
            ) {
            @Override
            public EternalTable newEternalTable(int eCap) {
                return new EternalTable(1); //for storing the eternal happiness goal
            }
        };
        fireHappy = new ConceptFire(happy, 1f);

        curiosity = new FloatParam( 0.10f);


        this.sense = nar.in.stream(id + " sensor");
        this.ambition = nar.in.stream(id + " ambition");
        this.predict = nar.in.stream(id + " predict");
        this.motor = nar.in.stream(id + " motor");
    }

    @Override
    public FloatParam curiosity() {
        return curiosity;
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

    public void stop() {
        nar.stop();
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    protected void sense(SensorConcept... s) {
        sense(Lists.newArrayList(s));
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    protected void sense(@NotNull Iterable<? extends SensorConcept> s) {
        Iterables.addAll(sensors, s);
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    protected void action(ActionConcept... s) {
        action(Lists.newArrayList(s));
    }

    /**
     * should only be invoked before agent has started TODO check for this
     */
    protected void action(@NotNull Iterable<? extends ActionConcept> s) {
        Iterables.addAll(actions, s);
    }


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();

    int actionFrame = 0;

    final AtomicBoolean busy = new AtomicBoolean(false);


    protected void senseAndMotor() {
        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));


            float r = reward = act();
            if (r == r) {
                rewardSum += r;
            }


            /** safety valve: if overloaded, enter shock / black out and do not receive sensor input */
            //        float load = nar.exe.load();
            //        if (load < 1) {


            long next = now + nar.dur()
                    //+(dur * 3 / 2);
                    ;


            ambition.input(
                Stream.of(happy.apply(nar), fireHappy)
            );


            motor.input(actions.stream().flatMap(a -> a.apply(nar)));
            //motor.input(curious(next), nar::input);

            sense.input(sense(nar, next));

            eventFrame.emitAsync(this, nar.exe);

            if (trace)
                logger.info(summary());


    }

    protected void predict() {
        predict.input(predictions(now));
        int dur = nar.dur();
        int horizon = Math.round(this.predictAheadDurs.floatValue()) * dur;
        if (sensors.size() > 0) {
            actions.forEach(a -> {
                for (Compound t : new Compound[] {
                        $.impl(a, dur, randomSensor()),
                        $.impl($.neg(a), dur, randomSensor())} )
                    predict.input(question(t, now + nar.random().nextInt(horizon) ));
            });
        }
    }

    /** provides the stream of the environment's next sensory percept tasks */
    public Stream<ITask> sense(NAR nar, long when) {
        return sensors.stream().map(s -> s.apply(nar));
    }


//    protected Stream<Task> curious(long next) {
//        float conf = curiosityConf.floatValue();
//        float confMin = nar.confMin.floatValue();
//        if (conf < confMin)
//            return Stream.empty();
//
//        float curiPerMotor = curiosityProb.floatValue() / actions.size();
//        return actions.stream().map(action -> {
//
//            if (nar.random().nextFloat() < curiPerMotor) {
//                return action.curiosity(conf, next, nar);
//            }/* else {
//                nar.activate(action, 1f);
//            }*/
//            return null;
//
//        }).filter(Objects::nonNull);
//
//    }


    @NotNull
    public String summary() {

        //sendInfluxDB("localhost", 8089);

        return id + " rwrd=" + n2(reward) +
                " dex=" + /*n4*/(dexterity()) +
                " var=" + n4(varPct(nar)) + "\t" + nar.terms.summary() + " " +
                nar.emotion.summary();
    }


    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    public synchronized void init() {

        if (initialized)
            return;

        initialized = true;

        now = nar.time();


        //this.curiosityAttention = reinforcementAttention / actions.size();

        /** set the sensor budget policy */
        int numSensors = sensors.size();
        int numActions = actions.size();

        @NotNull Compound happiness = happy.term();

        int dur = nar.dur();

        p.add(
                goal(happiness,
                        t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL),nar.confDefault(/*BELIEF*/ BELIEF))),
                        //ETERNAL
                        now
                )
        );

        p.add(
            question(seq($.varQuery(1), dur, happiness),
                now)
                //ETERNAL)
        );


//        predictors.add( question((Compound)$.parallel(happiness, $.varDep(1)), now) );
//        predictors.add( question((Compound)$.parallel($.neg(happiness), $.varDep(1)), now) );

        for (Concept a : actions) {
            Term action = a.term();

            ((FasterList) p).addAll(

                    question(impl(action, dur, happiness), now),
                    question(impl(neg(action), dur, happiness), now),

                    question(seq(action, dur, happiness), now),
                    question(seq(neg(action), dur, happiness), now),

                    question(seq(action, dur, $.varQuery(1)), now),
                    question(seq(neg(action), dur, $.varQuery(1)), now)

                    //dangerous: may lead to immobilizing self-fulfilling prophecy
                    //quest((Compound) (action.term()),now+dur)

//                            //ETERNAL)

                    //,question((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now)
                    //,quest((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now)

                    //quest((Compound)$.conj(varQuery(1), happy.term(), (Compound) (action.term())), now)




//                    question(impl(conj(varQuery(0),action), dur, happiness), now),
//                    question(impl(conj(varQuery(0),neg(action)), dur, happiness), now)

//                    new PredictionTask($.impl(action, dur, happiness), '?').time(nar, dur),
//                    new PredictionTask($.impl($.neg(action), dur, happiness), '?').time(nar, dur),

//                    new PredictionTask($.impl($.parallel(action, $.varQuery(1)), happiness), '?')
//                            .eternal(),
//                            //.time(nar, dur),
//                    new PredictionTask($.impl($.parallel($.neg(action), $.varQuery(1)), happiness), '?')
//                            .eternal(),
//                            //.time(nar, dur)

                    //question(impl(neg(action), dur, varQuery(1)), nar.time()),

//                    question(impl(happiness, -dur, conj(varQuery(1),action)), now),
//                    question(impl(neg(happiness), -dur, conj(varQuery(1),action)), now)

//                    question(impl(happiness, -dur, action), now),
//                    question(impl(neg(happiness), -dur, action), now)


//                    question(seq(action, dur, happiness), now),
//                    question(seq(neg(action), dur, happiness), now),
//                    question(seq(action, dur, neg(happiness)), now),
//                    question(seq(neg(action), dur, neg(happiness)), now)


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
//        predictors.add(
//                goal(happiness,
//                        t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL),nar.confDefault(/*BELIEF*/ BELIEF))),
//                        ETERNAL
//                )
//        );


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


        //System.out.println(Joiner.on('\n').join(predictors));
    }

    public SensorConcept randomSensor() {
        //quest(parallel((Compound) (action.term()), randomSensor()), now+dur),
        return sensors.get(nar.random().nextInt(sensors.size()));
    }


    public NAgent runCycles(final int totalCycles) {
        return runCycles(nar.dur(), totalCycles);
    }

    protected void next() {

}
    /**
     * synchronous execution
     */
    public synchronized NAgent runCycles(final int cyclesPerFrame, final int totalFrames) {

        init();

        @NotNull On active = nar.onCycle((n) -> {
            if (enabled.get()) {
                long lastNow = this.now;
                long now = nar.time();
                if (now - lastNow >= cyclesPerFrame) {
                    this.now = now;
                    //only execute at most one agent frame per duration
                    senseAndMotor();
                    predict();
                }
            }
        });

        nar.run(totalFrames);

        active.off();

        return this;
    }

    public NARLoop startRT(float fps) {
        return runRT(fps, -1);
    }



    /**
     * synchronous execution which runs a NAR directly at a given framerate
     */
    public NARLoop runRT(float fps, long stopTime) {
        init();

        NARLoop loop = nar.startFPS(fps);

        this.senseAndMotorLoop = nar.exe.loop(fps,()->{
            if (enabled.get()) {
                this.now = nar.time();
                senseAndMotor();
                predict();
            }
        });

        return loop;
    }

    protected Stream<ITask> predictions(long now) {


        int dur = nar.dur();

        int horizon = Math.round(this.predictAheadDurs.floatValue()) * dur;

        //long frameDelta = now-prev;


        long next = now + dur/2;

        return p.stream().map(x -> {
            if (x != null) {
                Task y = predict(x, next, horizon);
//                if (x != y && x!=null) {
//                    x.budget().priMult(0.5f);
//                }
                return y;
            } else {
                return null;
            }
        });
    }

    /**
     * average confidence of actions
     * see: http://www.dictionary.com/browse/dexterity?s=t
     */
    public float dexterity() {
        final float[] m = {0};
        int n = actions.size();
        int dur = nar.dur();
        long now = nar.time();
        for (int i = 0; i < n; i++) {
            actions.get(i).goals().forEach(x -> {
                //System.out.println(x.proof());
                m[0] += x.evi(now, dur);
            });
        }
        float dex = w2c(m[0] / n);
        return dex;
    }


    private Task predict(@NotNull Task t, long next, int horizon /* future time range */) {

        Task result;
        if (t.start() != ETERNAL) {

            //only shift for questions
            long shift = //horizon > 0 && t.isQuestOrQuestion() ?
                    nar.random().nextInt(horizon)
                    //: 0
            ;

            long range = t.end() - t.start();
            result = prediction(t.term(), t.punc(), t.truth(), next + shift, next + shift + range);

        } else if (t.isDeleted()) {

            result = prediction(t.term(), t.punc(), t.truth(), ETERNAL, ETERNAL);

        } else {
            //rebudget non-deleted eternal
            result = t;
        }

        return result
                .budget(nar)
                ;
    }

    public float rewardSum() {
        return rewardSum;
    }

    public static float varPct(NAR nar) {
        if (nar instanceof Default) {
            RecycledSummaryStatistics is = new RecycledSummaryStatistics();
            nar.forEachConceptActive(xx -> {
                Term tt = xx.term();
                float v = tt.volume();
                int c = tt.complexity();
                is.accept((v - c) / v);
            });

            return (float) is.getMean();
        }
        return Float.NaN;
    }


    public Task goal(@NotNull Compound term, Truth truth, long when) {
        return prediction(term, GOAL, truth, when, when + nar.dur());
    }

    public Task goal(@NotNull Compound term, Truth truth, long start, long end) {
        return prediction(term, GOAL, truth, start, end);
    }

    public Task question(@NotNull Compound term, long when) {
        return prediction(term, QUESTION, null, when, when + nar.dur());
    }

    public Task quest(@NotNull Compound term, long when) {
        return prediction(term, QUEST, null, when, when);
    }

    public Task prediction(@NotNull Compound term, byte punct, Truth truth, long start, long end) {
        if (truth == null && !(punct == QUESTION || punct == QUEST))
            return null; //0 conf or something

        DiscreteTruth tFinal;
        if (truth!=null) {
            tFinal = new DiscreteTruth(truth.freq(), truth.conf());
            if (tFinal == null)
                return null;
        } else {
            tFinal = null;
        }

        term = nar.terms.normalize(term);

        NALTask t = new NALTask(term, punct, tFinal, now, start, end, new long[]{nar.time.nextStamp()});
        t.setPri(nar.priorityDefault(punct));
        return t;
    }

    public final float alpha() {
        return nar.confDefault(BELIEF);
    }


    private final Topic<NAgent> eventFrame = new ArrayTopic();

    public On<NAgent> onFrame(Consumer<NAgent> each) {
        return eventFrame.on(each);
    }


}
