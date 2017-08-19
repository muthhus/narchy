package nars;

import jcog.Loop;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.event.ArrayTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static nars.$.impl;
import static nars.$.newArrayList;
import static nars.$.p;
import static nars.$.parallel;
import static nars.$.quote;
import static nars.$.t;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent extends DurService implements NSense, NAct {

    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    /**
     * identifies this environment instance
     **/
    public final Term id;

    public final Map<SensorConcept,CauseChannel<Task>> sensors = new LinkedHashMap();

    public final Map<ActionConcept,CauseChannel<Task>> actions = new LinkedHashMap();

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
    private final CauseChannel<Task> predict;




    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public final FloatParam curiosity;


    /**
     * prediction templates
     */
    public final List<Supplier<Task>> predictors = newArrayList();

    public final AtomicBoolean enabled = new AtomicBoolean(true);

    public boolean trace = false;

    protected long now;

    public float rewardSum = 0;

    /**
     * range: -1..+1
     */
    public float reward;
    private Loop loop;
    public NAR nar;
    //final private ConceptFire fireHappy;


    public NAgent(@NotNull NAR nar) {
        this("", nar);
    }

    public NAgent(@NotNull String id, @NotNull NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    public NAgent(@Nullable Term id, @NotNull NAR nar) {
        super(nar);

        this.nar = nar;

        this.id = id;
        this.now = ETERNAL; //not started

        this.happy = new SensorConcept(
                id == null ?
                        $.the("happy") : //generally happy
                        p(id, $.the("happy")), //happy in this environment
                nar,
                //new FloatPolarNormalized(() -> reward),
                () -> reward /* -1..+1 */,

                (x) -> t(0.5f * (Util.tanhFast(x * 2) + 1), alpha())

//                (x) -> {
//                    if (x > 0.5f + Param.TRUTH_EPSILON) {
//                        return t(1f, alpha() * (x - 0.5f) * 2f);
//                    } else if (x < 0.5f - Param.TRUTH_EPSILON) {
//                        return t(0f, alpha() * (0.5f - x) * 2f);
//                    } else {
//                        return t(0.5f, alpha());
//                    }
//                }
        );
        //nar.goal(happy, 1f, nar.confDefault(GOAL)); //ETERNAL <- not safe to use yet

        //fireHappy = Activation.get(happy, 1f, new ConceptFire(happy, 1f);

        curiosity = new FloatParam(0.10f, 0f, 1f);


        if (id==null) id = quote(getClass().toString());
        this.predict = nar.newCauseChannel(id + " predict");
    }

    @Override
    public FloatParam curiosity() {
        return curiosity;
    }

    @NotNull
    @Override
    public final Map<SensorConcept,CauseChannel<Task>> sensors() {
        return sensors;
    }

    @NotNull @Override
    public final Map<ActionConcept, CauseChannel<Task>> actions() {
        return actions;
    }

    @Override
    public final NAR nar() {
        return nar;
    }

    public void stop(NAR nar) {
        nar.stop();
        loop = null;
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


        predict.input(
                /*Stream.of(*/happy.apply(nar)/*, fireHappy)*/
        );

        //contribution of this agent to the NAR's global happiness measurement
        //nar.emotion.happy(Util.sigmoid(reward));
        nar.emotion.happy(dexterity() /* /nar.confDefault(GOAL) */);

//              float dxm = c2w(dexterity());
//            nar.emotion.happy(
//                    Math.signum(dxm - lastDexterity) *
//                            (float) Math.sqrt( Math.abs(dxm - lastDexterity) ));
//            this.lastDexterity = dxm;

        sensors.forEach((s,c)->{
            c.input(s.apply(nar));
        });

        actions.forEach((a,c)->{
            c.input(a.apply(nar));
        });

        eventFrame.emitAsync(this, nar.exe);

        if (trace)
            logger.info(summary());


    }

    protected void predict() {
        predict.input(predictions(now));
        int dur = nar.dur();
        int horizon = Math.round(this.predictAheadDurs.floatValue()) * dur;
//        if (sensors.size() > 0) {
//            actions.forEach(a -> {
//                for (Compound t : new Compound[] {
//                        $.impl(a, dur, randomSensor()),
//                        $.impl($.neg(a), dur, randomSensor())} )
//                    predict.input(question(t, now + nar.random().nextInt(horizon) ));
//            });
//        }
    }

//    /**
//     * provides the stream of the environment's next sensory percept tasks
//     */
//    public Stream<ITask> sense(NAR nar, long when) {
//        return sensors.stream().map(s -> s.apply(nar));
//    }


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
                //"\t" + Op.cache.summary() +
                /*" var=" + n4(varPct(nar)) + */ "\t" + nar.terms.summary() + " " +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void start(NAR nar) {

        this.nar = nar;

        super.start(nar);

        now = nar.time();


        nar.runLater(()-> {
            //this.curiosityAttention = reinforcementAttention / actions.size();

            /** set the sensor budget policy */

            @NotNull Term happy = this.happy.term();
            predictors.add(
                    goal(happy,
                            t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL), nar.confDefault(/*BELIEF*/ BELIEF)))
                            //ETERNAL
                    )
            );

            //        p.add(
            //            question(seq($.varQuery(1), dur, happiness),
            //                now)
            //                //ETERNAL)
            //        );


            //        predictors.add( question((Compound)$.parallel(happiness, $.varDep(1)), now) );
            //        predictors.add( question((Compound)$.parallel($.neg(happiness), $.varDep(1)), now) );

            for (Concept a : actions.keySet()) {
                Term action = a.term();

                Variable what = $.varQuery(1);
                Term notAction = action.neg();

                ((FasterList) predictors).addAll(

                        question(impl(action, happy)),
                        question(impl(notAction, happy)),
                        question(impl(action, what)),
                        question(impl(notAction, what)),

                        question(impl(parallel(what, action), happy)),
                        question(impl(parallel(what, notAction), happy)),

                        //question(seq(action, dur, happiness), now),
                        //question(seq(neg(action), dur, happiness), now),

                        //question(seq(action, dur, $.varQuery(1)), now),
                        //question(seq(neg(action), dur, $.varQuery(1)), now),

                        //dangerous: may lead to immobilizing self-fulfilling prophecy
                        //quest((Compound) (action.term()),now+dur)

                        //                            //ETERNAL)

                        //question((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now),
                        quest(parallel(what, action.term()))

                        //quest((Compound)$.parallel(varQuery(1), happy.term(), (Compound) (action.term())), now)


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
        });
    }

//    public SensorConcept randomSensor() {
//        //quest(parallel((Compound) (action.term()), randomSensor()), now+dur),
//        return sensors.get(nar.random().nextInt(sensors.size()));
//    }


//    public NAgent runCycles(final int totalCycles) {
//        return runCycles(nar.dur(), totalCycles);
//    }



    @Override
    protected void runDur(NAR nar) {
        if (enabled.get() && busy.compareAndSet(false, true)) {
            this.now = now;
            try {
                //only execute at most one agent frame per duration
                senseAndMotor();
                predict();
            } finally {
                busy.set(false);
            }
        }
    }

//    /**
//     * synchronous execution
//     */
//    public synchronized NAgent runCycles(final int cyclesPerFrame, final int totalFrames) {
//
//        //init();
//
//        @NotNull On active = nar.onCycle((n) -> {
//            if (enabled.get()) {
//                next(cyclesPerFrame);
//            }
//        });
//
//        nar.run(totalFrames);
//
//        active.off();
//
//        return this;
//    }


    public Loop running() {
        return loop;
    }

    protected Stream<Task> predictions(long now) {
        return predictors.stream().map(x -> {
            return x.get().budget(nar);
        });
    }

    /**
     * average confidence of actions
     * see: http://www.dictionary.com/browse/dexterity?s=t
     */
    public float dexterity() {
        int n = actions.size();
        if (n == 0)
            return 0;

        final float[] m = {0};
        int dur = nar.dur();
        long now = nar.time();
        actions.keySet().forEach(a -> {
            Truth g = nar.goalTruth(a, now);
            float c;
            if (g != null) {
                c = g.evi();
            } else {
                c = 0;
            }
            m[0] += c;
        });
        return w2c(m[0] / n /* avg */);
    }


//    private Task predict(@NotNull Supplier<Task> t, long next, int horizon /* future time range */) {
//
//        Task result;
////        if (t.start() != ETERNAL) {
////
////            //only shift for questions
////            long shift = //horizon > 0 && t.isQuestOrQuestion() ?
////                    nar.random().nextInt(horizon)
////                    //: 0
////            ;
////
////            long range = t.end() - t.start();
////            result = prediction(t.term(), t.punc(), t.truth(), next + shift, next + shift + range);
////
////        } else if (t.isDeleted()) {
////
////            result = prediction(t.term(), t.punc(), t.truth(), ETERNAL, ETERNAL);
////
////        } else {
//            //rebudget non-deleted eternal
////            result = t;
////        }
//
//        return result
//                .budget(nar)
//                ;
//    }

    public float rewardSum() {
        return rewardSum;
    }

    public static float varPct(NAR nar) {
        if (nar instanceof NAR) {
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


    public Supplier<Task> goal(@NotNull Term term, Truth truth) {
        return prediction(term, GOAL, new DiscreteTruth(truth.freq(), truth.conf()));
    }

    public Supplier<Task> question(@NotNull Term term) {
        return prediction(term, QUESTION, null);
    }

    public Supplier<Task> quest(@NotNull Term term) {
        return prediction(term, QUEST, null);
    }

    public Supplier<Task> prediction(@NotNull Term _term, byte punct, DiscreteTruth truth) {
        Term term = _term.normalize();
        return () -> {

//        if (truth == null && !(punct == QUESTION || punct == QUEST))
//            return null; //0 conf or something

            long now = nar.time();
            long start = now;
            long end = now + Math.round(predictAheadDurs.floatValue() * nar.dur());

            NALTask t = new NALTask(term, punct, truth, now,
                    start, end,
                    new long[]{nar.time.nextStamp()});
            return t;
        };
    }

    public final float alpha() {
        return nar.confDefault(BELIEF);
    }


    private final Topic<NAgent> eventFrame = new ArrayTopic();

    public On onFrame(Consumer each) {
        return eventFrame.on(each);
    }

    public On<NAgent> onFrame(Runnable each) {
        return eventFrame.on((n) -> each.run());
    }

}
