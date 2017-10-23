package nars.control;

import com.google.common.collect.TreeBasedTable;
import jcog.Util;
import jcog.learn.deep.RBM;
import jcog.learn.ql.HaiQAgent;
import jcog.list.FasterList;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatNormalized;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.NAgent;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import static jcog.Texts.n4;

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


        int goals = goal.length;
//        float[] goalFactor = new float[goals];
//        for (int j = 0; j < goals; j++) {
//            float m = 1;
//                        // causeSummary[j].magnitude();
//            //strength / normalization_magnitude
//            goalFactor[j] = goal[j] / ( Util.equals(m, 0, epsilon) ? 1 : m );
//        }

        for (int i = 0, causesSize = cc; i < causesSize; i++) {
            Cause c = causes.get(i);

            Traffic[] cg = c.goalValue;

            //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
            float next = 0;
            for (int j = 0; j < goals; j++) {
                next += goal[j] * cg[j].current;
            }

            float prev = c.value();
            final float momentum =
//                    0f;
                    0.98f;
//                    0.99f * (1f - Util.unitize(
//                            Math.abs(next) / (1 + Math.max(Math.abs(next), Math.abs(prev)))));

            //c.setValue(Util.lerp(momentum, next, prev));
            c.setValue(0.9f * (next + prev));

            //TODO
            //variation of volume weighted moving average
//            float prev = c.value();
//            float aPrev = Math.abs(prev);
//            float av = Math.abs(v);
//            float momentum = 0.5f;
//            float momDenom = Math.max(av, momentum * aPrev);
//            if (momDenom > 0)
//                c.setValue(Util.lerp((av / momDenom), prev, v));
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

    public static void learn(MetaGoal p, short[] effects, float strength, NAR nar) {
        learn(p, effects, strength, nar.causes);
    }


    /**
     * learn that the given effects have a given value
     */
    public static void learn(MetaGoal p, short[] effects, float strength, FasterList<Cause> causes) {

        //assert(strength >= 0);

        int numCauses = effects.length;


        if (Math.abs(strength) < Prioritized.EPSILON) return; //no change

        for (int i = 0; i < numCauses; i++) {
            short c = effects[i];
            Cause cc = causes.get(c);
//            if (cc == null)
//                continue; //ignore, maybe some edge case where the cause hasnt been registered yet?

            assert (cc != null) : "cause " + c + " missing";

            //trace decay curve
            //linear triangle increasing to inc, warning this does not integrate to 100% here
            float vPer = (((float) (i + 1)) / numCauses) * strength;

            cc.learn(p, vPer);

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


            value += cause.value();
        }


        return value / effects;
    }

    /**
     * creates an unsupervised network to learn and propagate co-occurring value between coherent Causes
     */
    public static CycleService newValueSynergizer(NAR n) {
        return new CycleService(n) {

            public double[] next;
            public int learning_iters = 5;
            public double learning_rate = 0.01f;

            public double[] cur;
            public RBM rbm;


            @Override
            protected void run(NAR nar) {
                int numCauses = nar.causes.size();
                if (numCauses < 2)
                    return;

                if (rbm == null || rbm.n_visible != numCauses) {
                    int numHidden = numCauses / 4;

                    rbm = new RBM(numCauses, numHidden, null, null, null, nar.random()) {
                        @Override
                        public double activate(double a) {
                            return super.activate(a);
                            //return Util.tanhFast((float) a);
                            //return Util.sigmoidBipolar((float) a, 5);
                        }
                    };
                    cur = new double[numCauses];
                    next = new double[numCauses];
                }

                FasterList<Cause> cause = nar.causes;
                for (int i = 0; i < numCauses; i++)
                    cur[i] = cause.get(i).value();

                rbm.reconstruct(cur, next);
                rbm.contrastive_divergence(cur, learning_rate, learning_iters);

                //float momentum = 0.5f;
                Random rng = nar.random();
                float noise = 0.1f;
                for (int i = 0; i < numCauses; i++) {
                    //float j = Util.tanhFast((float) (cur[i] + next[i]));
                    float j = /*((rng.nextFloat()-0.5f)*2*noise)*/ +
                            0.5f * (float) (cur[i]) + 0.5f * ((float) (next[i]));
                    cause.get(i).setValue(j);
                }

            }
        };

    }

    public static AgentService newController(NAgent a) {
        NAR n = a.nar;
        AgentService.AgentBuilder b = new AgentService.AgentBuilder(
                //DQN::new,
                HaiQAgent::new,
                //() -> Util.tanhFast(a.dexterity())) //reward function
                () -> a.dexterity() * Util.tanhFast(a.rewardCurrent) /* - lag */) //reward function

                .in(a::dexterity)
                .in(a.happy)
//                .in(new FloatNormalized(
//                        ((Emotivation) n.emotion).cycleDTRealMean::getValue)
//                        .decay(0.9f)
//                )
                .in(new FloatNormalized(
                        //TODO use a Long-specific impl of this:
                        new FloatFirstOrderDifference(n::time, () -> n.emotion.taskDerived.getValue().longValue())
                ).relax(0.1f))
                .in(new FloatNormalized(
                                //TODO use a Long-specific impl of this:
                                new FloatFirstOrderDifference(n::time, () -> n.emotion.conceptFirePremises.getValue().longValue())
                        ).relax(0.1f)
                ).in(new FloatNormalized(
                                () -> n.emotion.busyVol.getSum()
                        ).relax(0.1f)
                );

        Arrays.fill(n.want, 0);

        for (MetaGoal g : values()) {
            final int gg = g.ordinal();
            float min = -2;
            float max = +2;
            b.in(new FloatNormalized(() -> n.want[gg], min, max));

            float step = 0.25f;

            b.out(2, (w) -> {
                float str = 0.05f + step * Math.abs(n.want[gg] / 4f);
                switch (w) {
                    case 0:
                        n.want[gg] = Math.min(max, n.want[gg] + str);
                        break;
                    case 1:
                        n.want[gg] = Math.max(min, n.want[gg] - str);
                        break;
                }
            });
        }

//        .out(
//                        new StepController((x) -> n.time.dur(Math.round(x)), 1, n.dur(), n.dur() * 2)
//                ).out(
//                        StepController.harmonic(n.confMin::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(n.truthResolution::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(a.curiosity::setValue, 0.01f, 0.16f)
//                ).get(n);

        return b.get(n);

    }

    public static class Report extends ObjectDoubleHashMap<ObjectBytePair<Cause>> {

        public TreeBasedTable<Cause,MetaGoal,Double> table() {
            TreeBasedTable<Cause, MetaGoal, Double> tt = TreeBasedTable.create();
            MetaGoal[] mv = MetaGoal.values();
            synchronized(this) {
                forEachKeyValue((k, v) -> {
                    Cause c = k.getOne();
                    MetaGoal m = mv[k.getTwo()];
                    tt.put(c, m, v);
                });
            }
            return tt;
        }

        public Report add(Report r) {
            synchronized(this) {
                r.forEachKeyValue(this::addToValue);
            }
            return this;
        }

        public Report add(Iterable<Cause> cc) {

            cc.forEach(c -> {

                int i = 0;
                for (Traffic t : c.goalValue) {
                    MetaGoal m = MetaGoal.values()[i];
                    double tt = t.total;
                    if (tt != 0) {
                        synchronized(this) {
                            addToValue(PrimitiveTuples.pair(c, (byte) i), tt);
                        }
                    }
                    i++;
                }

            });
            return this;
        }

        public void print(PrintStream out) {
            synchronized (this) {
                keyValuesView().toSortedListBy(x -> -x.getTwo()).forEach(x ->
                  out.println(
                      n4(x.getTwo()) + "\t" + MetaGoal.values()[x.getOne().getTwo()] + "\t" + x.getOne().getOne()
                  )
               );
            }
        }
    }

}
