package nars;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import jcog.data.FloatParam;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.math.FloatPolarNormalized;
import jcog.math.RecycledSummaryStatistics;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.table.EternalTable;
import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.Loop;
import nars.util.data.Mix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
    public final FloatParam predictAheadDurs = new FloatParam(1, 1, 32);


    public final FloatParam predictorProbability = new FloatParam(1f);
    private final Mix.MixStream<String, Task> sense;
    private final Mix.MixStream<String, Task> ambition;
    private final Mix.MixStream<String, Task> motor;


    private boolean initialized;


    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public final FloatParam curiosityConf;
    public final FloatParam curiosityProb;

    public final List<Task> predictors = newArrayList();


    public boolean trace = false;

    protected long now;

    public float rewardSum = 0;

    /**
     * range: -1..+1, mapped directly to the 0..1.0 frequency range
     */
    public float reward;

    public NAgent(@NotNull NAR nar) {
        this("", nar);
    }

    public NAgent(@NotNull String id, @NotNull NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    public NAgent(@Nullable Term id, @NotNull NAR nar) {

        this.id = id;
        this.nar = nar;

        this.now = nar.time();

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

        curiosityConf = new FloatParam( 0.10f);
        curiosityProb = new FloatParam( 0.10f);

        this.sense = nar.mix.stream(id + " sensor");
        this.ambition = nar.mix.stream(id + " ambition");
        this.motor = nar.mix.stream(id + " motor");
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

    public void cycle() {
        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));

        if (!busy.compareAndSet(false, true)) {
            return; //black-out, the last frame didnt even finish yet
        }

        try {

            this.now = nar.time();


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


            ambition.input(predict(next), nar::input);
            ambition.input(Stream.<Task>of(happy.apply(nar)), nar::input);

            motor.input(actions.stream().map(a -> a.apply(nar)), nar::input);
            motor.input(curious(next), nar::input);

            sense.input(sense(nar, next), nar::input);

            eventFrame.emit(this);

            if (trace)
                logger.info(summary());

        } finally {
            busy.set(false);
        }

    }

    /** provides the stream of the environment's next sensory percept tasks */
    public Stream<Task> sense(NAR nar, long when) {
        return sensors.stream().map(s -> s.apply(nar));
    }


    protected Stream<Task> curious(long next) {
        float conf = curiosityConf.floatValue();
        float confMin = nar.confMin.floatValue();
        if (conf < confMin)
            return Stream.empty();

        float curiPerMotor = curiosityProb.floatValue() / actions.size();
        return actions.stream().map(action -> {

            if (nar.random().nextFloat() < curiPerMotor) {
                return action.curiosity(conf, next, nar);
            }/* else {
                nar.activate(action, 1f);
            }*/
            return null;

        }).filter(Objects::nonNull);

    }


    @NotNull
    public String summary() {

        //sendInfluxDB("localhost", 8089);

        return id + " rwrd=" + n2(reward) +
                " dex=" + n4(dexterity()) +
                " var=" + n4(varPct(nar)) + "\t" + nar.concepts.summary() + " " +
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

        predictors.add(
                goal(happiness,
                        t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL),nar.confDefault(/*BELIEF*/ BELIEF))),
                        //ETERNAL
                        now + dur
                )
        );
        predictors.add(
                goal(happiness,
                        t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL),nar.confDefault(/*BELIEF*/ BELIEF))),
                        ETERNAL
                )
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

//                    question(impl($.varQuery(1), happiness), ETERNAL)

        //predictors.add( question((Compound)$.parallel(happiness, $.varDep(1)), now) );
        //predictors.add( question((Compound)$.parallel($.neg(happiness), $.varDep(1)), now) );

        for (Concept a : actions) {
            Term action = a.term();

            ((FasterList) predictors).addAll(

                    quest((Compound) (action.term()),
                            now),
//                            //ETERNAL)

                    //,question((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now)
                    //,quest((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now)

                    //quest((Compound)$.conj(varQuery(1), happy.term(), (Compound) (action.term())), now)


                    question(impl(action, dur, happiness), ETERNAL),
                    question(impl(neg(action), dur, happiness), ETERNAL)

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


        //System.out.println(Joiner.on('\n').join(predictors));
    }


    /**
     * synchronous execution managed by existing NAR's
     */
    public NAgent runCycles(final int cycles) {

        init();

        nar.onCycle((n) -> {

            long lastNow = this.now;
            long now = nar.time();
            int dur = nar.dur();
            if (now - lastNow < dur) {
                return; //only execute at most one agent frame per duration
            }

            cycle();

        });
        nar.run(cycles);

        return this;
    }

    @NotNull
    public Loop runRT(float fps) {
        return runRT(fps, -1);
    }

    final AtomicReference<Timer> timer = new AtomicReference(null);

    /**
     * synchronous execution which runs a NAR directly at a given framerate
     */
    @NotNull
    public Loop runRT(float fps, long stopTime) {

        init();

        Timer t = timer.updateAndGet((x)-> x==null ? new Timer() : x);

        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cycle();
            }
        }, 0, Math.round(1000f / fps));


//        nar.onReset(nn->{
//            t.cancel();
//        });
        return nar.loop();

    }


    protected Stream<Task> predict(long next) {


        int dur = nar.dur();
        int horizon = Math.round(this.predictAheadDurs.floatValue()) * dur;

        //long frameDelta = now-prev;

        int num = predictors.size();

        float pp = predictorProbability.floatValue();

        return IntStream.range(0, num).mapToObj(i -> {
            if (nar.random().nextFloat() > pp)
                return null;

            Task x = predictors.get(i);
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
        for (ActionConcept a : actions) {
            a.goals().forEach(x -> {
                if (!(x instanceof ActionConcept.CuriosityTask)) {
                    //System.out.println(x.proof());
                    m[0] += x.evi(now, dur);
                }
            });
        }
        float dex = w2c(m[0] / n);
        return dex;
    }


    private Task predict(@NotNull Task t, long next, int horizon /* future time range */) {

        Task result;
        if (t.start() != ETERNAL) {

            //only shift for questions
            long shift = horizon > 0 && t.isQuestOrQuestion() ? nar.random().nextInt(horizon) : 0;

            long range = t.end() - t.start();
            //System.out.println(now + " " + nar.time() + " -> " + next + "+" + shift + " = " + (next+shift));
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
            nar.forEachActiveConcept(xx -> {
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
        return prediction(term, GOAL, truth, when, when);
    }

    public Task goal(@NotNull Compound term, Truth truth, long start, long end) {
        return prediction(term, GOAL, truth, start, end);
    }

    public Task question(@NotNull Compound term, long when) {
        return prediction(term, QUESTION, null, when, when);
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

        term = nar.concepts.normalize(term);

        return new ImmutableTask(term, punct, tFinal, nar.time(), start, end, new long[]{nar.time.nextStamp()});
    }

    public final float alpha() {
        return nar.confDefault(BELIEF);
    }


    private final Topic<NAgent> eventFrame = new ArrayTopic();

    public On<NAgent> onFrame(Consumer<NAgent> each) {
        return eventFrame.on(each);
    }


}
