package nars.experiment;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.util.Util;
import nars.util.math.PolarRangeNormalizedFloat;
import nars.util.signal.FuzzyConceptSet;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.nal.Tense.ETERNAL;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAREnvironment {

    private final SensorConcept happy;
    private final float reinforcementAttention;
    public NAR nar;

    public final FuzzyConceptSet reward;
    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<MotorConcept> motors = $.newArrayList();
    public float alpha, gamma, epsilon;
    private float rewardValue;


    public NAREnvironment(NAR nar) {
        this.nar = nar;
        alpha = this.nar.confidenceDefault(Symbols.BELIEF);
        gamma = this.nar.confidenceDefault(Symbols.GOAL);
        epsilon = 0.05f;
        this.reinforcementAttention = gamma;

        reward =  new FuzzyConceptSet(
                new PolarRangeNormalizedFloat(()->rewardValue),
                //new RangeNormalizedFloat(input),
                nar,
                //"(I --> sad)", "(I --> neutral)", "(I --> happy)").resolution(0.02f);
                //"(" + nar.self + " --> [sad])", "(" + nar.self + " --> [happy])").resolution(0.05f);
                //nar.self + "(sad)", nar.self + "(happy)"
                //nar.self + "(happy)"
                "(happy)"
        ).resolution(0.02f);

        this.happy = reward.iterator().next(); //the only one HACK
        happy.desire($.t(1f, gamma));

    }

    /** install motors and sensors in the NAR */
    abstract public void init(NAR n);

    /** interpret env state into sensor states */
    public void perceive() {
        /* by default nothing should need done */
    }

    /** interpret motor states into env actions */
    abstract public float act();

    public void next() {

        perceive();

        reinforce();

        nar.next();

        rewardValue = act();

    }

    public void run(int cycles, int frameDelayMS) {

        init(nar);

        for (int t = 0; t < cycles; t++) {
            next();

            if (frameDelayMS > 0)
                Util.pause(frameDelayMS);
        }
    }

    public void reinforce() {
        long now = nar.time();

        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));
        if (reinforcementAttention > 0) {

            //boost(happy);
            boost(happy);
            //boost(sad);


            for (MotorConcept c : motors) {
                if (Math.random() < epsilon) {
                    nar.inputLater(new MutableTask(c, '!',
                            Math.random() > 0.5f ? 1f : 0f,
                            nar).present(now).log("Curiosity"));
                }
                boost(c);
            }

//            for (MutableTask x : predictors)
//                boost(x);

            //nar.goal(RewardAttentionPerFrame.pri(), sad, now+1, 0f, gamma);
            //nar.goal(RewardAttentionPerFrame.pri(), happy, now+1, 1f, gamma);
        }



    }

    public @Nullable Concept boost(Concept c) {

        return nar.activate(c, UnitBudget.One, nar.inputActivation.floatValue() * reinforcementAttention, reinforcementAttention, null);
    }



    private void boost(@NotNull MutableTask t) {
        BudgetMerge.max.apply(t.budget(), UnitBudget.One, reinforcementAttention);
        if (t.isDeleted())
            throw new RuntimeException();

        if (t.occurrence()!=ETERNAL) {
            nar.inputLater(new GeneratedTask(t.term(), t.punc(), t.truth()).time(nar.time(), nar.time())
                    .budget(reinforcementAttention, 0.5f, 0.5f).log("Predictor Clone"));
        } else {
            //just reactivate the existing eternal
            nar.activate(t);
        }

    }
}
