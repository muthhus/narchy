package nars.control;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;

/** high-level reasoner control parameters */
public enum MetaGoal {

    /**
     * neg: perceived as input, can be measured for partial and complete tasks
     * in their various stages of construction.
     *
     * information is considered negative value by default (spam).
     * see: http://mattmahoney.net/costofai.pdf
     *
     * satisfying other goals is necessary to compensate for the
     * perceptual cost.
     * */
    Perceive,

    /** pos: accepted into the memory */
    Accept,

    /** pos: anwers a question */
    Answer,

    /** pos: actuated a goal concept */
    Action,

    /** pos: confirmed a sensor input */
    Accurate,

    /** neg: contradicted a sensor input */
    Inaccurate;

    /** goal and goalSummary instances correspond to the possible MetaGoal's enum
     * however summary has an additional instance for the global normalization step
     */
    public static void update(FasterList<Cause> causes, float[] goal, RecycledSummaryStatistics[] goalSummary) {

        for (RecycledSummaryStatistics r : goalSummary) {
            //double m = r.getMax();
            r.clear();
            //r.setMax(m * 0.9f);
        }

        int cc = causes.size();
        for (int i = 0, causesSize = cc; i < causesSize; i++) {
            causes.get(i).commit(goalSummary);
        }

        final float epsilon = 0.01f;

        //final float LIMIT = +1f;
        final float momentum = 0.9f;

        int goals = goal.length;
        float[] goalMagnitude = new float[goals];
        for (int i = 0; i < goals; i++) {
            float m = goalSummary[i].magnitude();
            goalMagnitude[i] = Util.equals(m, 0, epsilon) ? 1 : m;
        }

        RecycledSummaryStatistics goalPreNorms = goalSummary[goals /* the extra one */];

        for (int i = 0, causesSize = cc; i < causesSize; i++) {
            Cause c = causes.get(i);
            float v = 0;
            //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
            for (int j = 0; j < goals; j++) {
                v += goal[j] * c.goalValue[j].current /goalMagnitude[j];
            }
            goalPreNorms.accept( c.valuePreNorm = v );
        }

        float max = (float) goalPreNorms.getMax();
        float min = (float) goalPreNorms.getMin();

        if (Util.equals(max, min, epsilon)) {
            causes.forEach(Cause::setValueZero); //flat
        } else {

//            boolean bipolar = !(min <= 0 ^ max < 0);
//            float mid = bipolar ? 0 : (max+min)/2f;
//            float rangePos = max - mid;
//            float rangeNeg = mid - min;

            float valueMag = Math.max(Math.abs(max),Math.abs(min));

            for (int i = 0, causesSize = cc; i < causesSize; i++) {
                Cause c = causes.get(i);

                float n = c.valuePreNorm;
//                float v = n >= 0 ?
//                        (n - mid) / rangePos :
//                        (mid - n) / rangeNeg
//                        ;
                float v = n / valueMag; //normalize to -1..+1
//
                float nextValue =
                        Util.lerp(momentum, v, c.value);

                c.value = nextValue;
            }
        }



//        System.out.println("WORST");
//        causes.stream().map(x -> PrimitiveTuples.pair(x, x.value())).sorted(
//                (x,y) -> Doubles.compare(x.getTwo(), y.getTwo())
//        ).limit(20).forEach(x -> {
//            System.out.println("\t" + x);
//        });
//        System.out.println();

    }

    /** contributes the value to a particular goal in a cause's goal vector */
    public void learn(Traffic[] goalValue, float v) {
        goalValue[ordinal()].addAndGet(v);
    }

    /** sets the desired level for a particular MetaGoal.
     * the value may be positive or negative indicating
     * its desirability or undesirability.
     * the absolute value is considered relative to the the absolute values
     * of the other MetaGoal's
     */
    public void want(float[] wants, float v) {
        wants[ordinal()] = v;
    }

    /** creates a new goal vector  */
    public static float[] newWants() {
        return new float[MetaGoal.values().length];
    }

}
