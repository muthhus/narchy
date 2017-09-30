package nars.control;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Prioritized;
import nars.NAR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * high-level reasoner control parameters
 */
public enum MetaGoal {

    /**
     * neg: perceived as input, can be measured for partial and complete tasks
     * in their various stages of construction.
     * <p>
     * information is considered negative value by default (spam).
     * see: http://mattmahoney.net/costofai.pdf
     * <p>
     * satisfying other goals is necessary to compensate for the
     * perceptual cost.
     */
    Perceive,

    /**
     * pos: accepted as belief
     */
    Believe,

    /**
     * pos: accepted as goal
     */
    Desire,

    /**
     * pos: anwers a question
     */
    Answer,

    /**
     * pos: actuated a goal concept
     */
    Action,

    /**
     * pos: prediction confirmed a sensor input
     */
    Accurate,

    /**
     * neg: contradicted a sensor input
     */
    Inaccurate;

    /**
     * goal and goalSummary instances correspond to the possible MetaGoal's enum
     * however summary has an additional instance for the global normalization step
     */
    public static void update(FasterList<Cause> causes, float[] goal, RecycledSummaryStatistics[] causeSummary) {

        for (RecycledSummaryStatistics r : causeSummary) {
            r.clear();
        }

        int cc = causes.size();
        for (int i = 0, causesSize = cc; i < causesSize; i++) {
            causes.get(i).commit(causeSummary);
        }

        final float epsilon = 0.01f;

        final float momentum = 0.95f;

        int goals = goal.length;
        float[] goalMagnitude = new float[goals];
        for (int i = 0; i < goals; i++) {
            float m = causeSummary[i].magnitude();
            goalMagnitude[i] = Util.equals(m, 0, epsilon) ? 1 : m;
        }

        //RecycledSummaryStatistics goalCausePreNorm = causeSummary[goals /* the extra one */];

        for (int i = 0, causesSize = cc; i < causesSize; i++) {
            Cause c = causes.get(i);
            float v = 0;
            //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
            for (int j = 0; j < goals; j++) {
                v += goal[j] * c.goalValue[j].current / goalMagnitude[j];
            }

            float nextValue = Util.lerp(momentum, v, c.value());
            c.setValue(nextValue);

        }

//        float max = (float) goalCausePreNorm.getMax();
//        float min = (float) goalCausePreNorm.getMin();
//
//        if (Util.equals(max, min, epsilon)) {
//            causes.forEach(Cause::setValueZero); //flat
//        } else {
//
////            boolean bipolar = !(min <= 0 ^ max < 0);
////            float mid = bipolar ? 0 : (max+min)/2f;
////            float rangePos = max - mid;
////            float rangeNeg = mid - min;
//
//            float valueMag =
//                    //Math.max(Math.abs(max), Math.abs(min)); //normalized to absolute range
//                    1; //no normalization
//
////            for (int i = 0, causesSize = cc; i < causesSize; i++) {
////                Cause c = causes.get(i);
////
////                float n = c.valuePreNorm;
//////                float v = n >= 0 ?
//////                        (n - mid) / rangePos :
//////                        (mid - n) / rangeNeg
//////                        ;
////                float v = n / valueMag; //normalize to -1..+1
//////
////                float nextValue =
////                        Util.lerp(momentum, v, c.value());
////
////                c.setValue(nextValue);
////            }
//        }


//        System.out.println("WORST");
//        causes.stream().map(x -> PrimitiveTuples.pair(x, x.value())).sorted(
//                (x,y) -> Doubles.compare(x.getTwo(), y.getTwo())
//        ).limit(20).forEach(x -> {
//            System.out.println("\t" + x);
//        });
//        System.out.println();

    }

    public static void value(MetaGoal p, short[] effects, float strength, NAR nar) {
        value(p, effects, strength, nar.causes);
    }

    /**
     * learn that the given effects have a given value
     */
    public static void value(MetaGoal p, short[] effects, float strength, FasterList<Cause> causes) {

        //assert(strength >= 0);
        if (Math.abs(strength) < Prioritized.EPSILON) return; //no change

        int numCauses = effects.length;

        float vPer =
                strength / numCauses; //fair

        for (int i = 0; i < numCauses; i++) {
            short c = effects[i];
            Cause cc = causes.get(c);
            if (cc == null)
                continue; //ignore, maybe some edge case where the cause hasnt been registered yet?
            /*assert(cc!=null): c + " missing from: " + n.causes.size() + " causes";*/

            //float vPer = (((float) (i + 1)) / sum) * value; //linear triangle increasing to inc, warning this does not integrate to 100% here
            if (vPer != 0) {
                cc.learn(p, vPer);
            }
        }
    }

    /**
     * contributes the value to a particular goal in a cause's goal vector
     */
    public void learn(Traffic[] goalValue, float v) {
        goalValue[ordinal()].addAndGet(v);
    }

    /**
     * sets the desired level for a particular MetaGoal.
     * the value may be positive or negative indicating
     * its desirability or undesirability.
     * the absolute value is considered relative to the the absolute values
     * of the other MetaGoal's
     */
    public void want(float[] wants, float v) {
        wants[ordinal()] = v;
    }

    /**
     * creates a new goal vector
     */
    public static float[] newWants() {
        return new float[MetaGoal.values().length];
    }


    public static final Logger logger = LoggerFactory.getLogger(MetaGoal.class);

    /**
     * estimate the priority factor determined by the current value of priority-affecting causes
     */
    public static float privaluate(FasterList<Cause> values, short[] effect) {

        int effects = effect.length;
        if (effects == 0) return 0;

        float value = 0;
        for (short c : effect) {
            Cause cause = values.getSafe(c);
            if (cause == null) {
                logger.error("cause id={} missing", c);
                continue;
            }

            if (cause.valuePrioritizes)
                value += cause.value();
        }


        return value / effects;
    }


}
