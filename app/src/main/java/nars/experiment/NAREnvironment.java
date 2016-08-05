package nars.experiment;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.util.Util;
import nars.util.data.list.FasterList;
import nars.util.math.RangeNormalizedFloat;
import nars.util.signal.Emotion;
import nars.util.signal.FuzzyConceptSet;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.agent.NAgent.varPct;
import static nars.nal.Tense.ETERNAL;
import static nars.util.Texts.n4;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAREnvironment {


    static final Logger logger = LoggerFactory.getLogger(NAREnvironment.class);

    public final SensorConcept happy;
    private final float reinforcementAttention;
    public NAR nar;

    public final FuzzyConceptSet reward;
    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<MotorConcept> actions = $.newArrayList();

    public float alpha, gamma, epsilon;
    @Deprecated public static final float gammaEpsilonFactor = 0.35f;

    public float rewardValue;
    private final FasterList<MutableTask> predictors = $.newArrayList();
    private boolean trace = true;

    int ticksBeforeObserve = 1, ticksBeforeDecide = 1;
    protected long now;

    public NAREnvironment(NAR nar) {
        this.nar = nar;
        alpha = this.nar.confidenceDefault(Symbols.BELIEF);
        gamma = this.nar.confidenceDefault(Symbols.GOAL);

        float rewardGamma =
                1.0f
                //gamma
        ;

        epsilon = 0.05f;
        this.reinforcementAttention = gamma;

        reward = new FuzzyConceptSet(
                //new PolarRangeNormalizedFloat(()->rewardValue),
                new RangeNormalizedFloat(() -> rewardValue),
                nar,
                //"(I --> sad)", "(I --> neutral)", "(I --> happy)").resolution(0.02f);
                //"(" + nar.self + " --> [sad])", "(" + nar.self + " --> [happy])").resolution(0.05f);
                //nar.self + "(sad)", nar.self + "(happy)"
                //nar.self + "(happy)"
                "(happy)"
        ).resolution(0.02f);

        this.happy = reward.iterator().next(); //the only one HACK
        happy.desire($.t(1f, rewardGamma));

    }

    /**
     * install motors and sensors in the NAR
     */
    abstract protected void init(NAR n);


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();

    public void next() {

        for (int i = 0; i < ticksBeforeObserve; i++)
            nar.clock.tick();

        reinforce();

        for (int i = 0; i < ticksBeforeDecide - 1; i++)
            nar.clock.tick();

        nar.next();


        now = nar.time();

        rewardValue = act();

        if (trace)
            System.out.println(summary());

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
                  "rwrd=" + n4(rewardValue) + "\t" +
                  "hapy=" + n4(emotion.happy()) + " "
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
        int dt = ticksBeforeObserve + ticksBeforeDecide;


        @NotNull Term what = $.$("?w"); //#w

        predictors.add(
                //what imples reward R
                (MutableTask) nar.ask($.impl(what, happy.term()), '?', ETERNAL)
        );

        //what co-occurs with reward R
        predictors.add(
                (MutableTask) nar.ask($.conj(what, dt, happy.term()), '?', ETERNAL)
        );
        predictors.add(
                (MutableTask) nar.ask($.conj(what, dt, happy.term()), '?', nar.time())
        );


        for (Concept x : actions) {

            //quest for each action
            predictors.add((MutableTask) nar.ask(x, '@', ETERNAL));
            predictors.add((MutableTask) nar.ask(x, '@', nar.time()));

            //does action A imply reward R?
            predictors.add(
                    (MutableTask) nar.ask($.conj(x.term(), dt, happy.term()), '?', ETERNAL)
            );


        }

    }

    public void run(int cycles, int frameDelayMS) {

        init(nar);

        mission();
        nar.next(); //step one for any mission() results to process first

        for (int t = 0; t < cycles; t++) {
            next();

            if (frameDelayMS > 0)
                Util.pause(frameDelayMS);
        }
    }

    protected void reinforce() {
        long now = nar.time();

        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));
        if (reinforcementAttention > 0) {

            //boost(happy);
            boost(happy);
            //boost(sad);


            for (MotorConcept c : actions) {
                if (Math.random() < epsilon) {
                    nar.inputLater(new GeneratedTask(c, '!',
                            $.t(nar.random.nextFloat()
                            //Math.random() > 0.5f ? 1f : 0f
                            , gamma * gammaEpsilonFactor)) {
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
                    }.
                            present(now).log("Curiosity"));
                }
                boost(c);
            }

            for (MutableTask x : predictors)
                boost(x);


        }


    }

    @Nullable
    protected Concept boost(Concept c) {

        return nar.activate(c, UnitBudget.One, nar.inputActivation.floatValue() * reinforcementAttention, reinforcementAttention, null);
    }


    private void boost(@NotNull MutableTask t) {
        BudgetMerge.max.apply(t.budget(), UnitBudget.One, reinforcementAttention);
        if (t.isDeleted())
            throw new RuntimeException();

        if (t.occurrence() != ETERNAL) {
            nar.inputLater(new GeneratedTask(t.term(), t.punc(), t.truth()).time(nar.time(), nar.time())
                    .budget(reinforcementAttention, 0.5f, 0.5f).log("Predictor Clone"));
        } else {
            //just reactivate the existing eternal
            try {
                nar.activate(t);
            } catch (Exception e) {
                logger.warn("{}", e.toString());
            }
        }

    }
}
