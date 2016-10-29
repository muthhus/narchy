package nars.task;

import com.google.common.base.Joiner;
import nars.$;
import nars.Param;
import nars.Task;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.eternalize;
import static nars.truth.TruthFunctions.w2c;
import static org.eclipse.collections.impl.block.factory.Functions2.min;
import static org.eclipse.collections.impl.block.factory.Functions2.minBy;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation extends InterpolatingMicrosphere {

    @NotNull final float[][] times;
    @NotNull final float[] freq;
    @NotNull final float[] conf;

    //private final static boolean clipToBeliefs = true;


    //@Nullable private static final Truth EterNull = $.t(0.5f, Param.TRUTH_EPSILON);

    public TruthPolation(int size) {
        super(1, 2 /* must be 2 for 1-D microsphere */, null);

        times = new float[size][1];
        freq = new float[size];
        conf = new float[size];

    }

//    public static float temporalConfidenceLoss(float dt, float evidence, float decayPeriod) {
//        if (dt <= 0.5f) {
//            return evidence;
//        } else {
////            float eternalized =
////                    //c2w(TruthFunctions.eternalize(w2c(evidence)));
////                    evidence/8f;
//
//
//            float newEvidence = evidence * 1f / (1f + (dt*dt) / (decayPeriod/2f));
//            //System.out.println(dt + "," + evidence + "\t" + decayPeriod + ","+decayFactor + "\t --> " + newEvidence);
//            //return Math.max(eternalized, newEvidence);
//            return newEvidence;
//        }
//    }

//    public static final InterpolatingMicrosphere.LightCurve evidentialDecayThroughTime = (dt, evidence) -> {
//        return TruthPolation.temporalConfidenceLoss(dt, evidence, 1f);
//    };

    @Nullable
    public Truth truth(long when, float dur, @NotNull Task... tasks) {
        return truth(when, tasks, lightCurve(dur));
    }

    @Nullable
    public Truth truth(long when, float dur, @NotNull Collection<Task> tasks) {
        return truth(when, tasks.toArray(new Task[tasks.size()]), lightCurve(dur));
    }

    @Nullable
    public Truth truth(long when, @NotNull Task[] tasks, LightCurve lightCurve) {

        assert(tasks.length > 2);

        long minT = Long.MAX_VALUE, maxT = Long.MIN_VALUE;
        float minTaskConf = Float.POSITIVE_INFINITY;

        //int volume = tasks[0].term().volume();
        int i = 0;
        for (Task t : tasks) {

            long o = t.occurrence();
            times[i][0] = (o != ETERNAL) ? o : when;
            freq[i] = t.freq();
            float ci = t.conf();
            if (ci < minTaskConf)
                minTaskConf = ci;
            conf[i] = c2w(ci);

            if (minT > o) minT = o;
            if (maxT < o) maxT = o;

            i++;
        }


//        //clip to out-of-range temporal margin, ie. beyond which confidence begins diminishing with distance
//        //final int dtTolerance;
//        if (clipToBeliefs) {
//            int window = 1;
//            if (minT - when >= window) {
//                //past looking into future
//                when = minT;
//            } else if (when - maxT >= window) {
//                //present/future looking into past
//                when = maxT;
//            } else {
//            }
//        }

        float[] v = this.value(
                new float[] { when },
                times,
                freq, conf,
                lightCurve,
                i);
        float c1 = w2c(v[1]);

        float minConf = Param.TRUTH_EPSILON; //HACK take value from NAR

        float F = v[0];

        if (c1 >= minConf) {
            return $.t(F, c1);
        } else {
            //attempt to use the calculated frequency with the eternalized confidence of the minimum task (conservative heuristic)
            float c2 = eternalize(minTaskConf);
            return (c2 >= minConf) ? $.t(F, c2) : null;
        }
    }


//    LightCurve lightCurve = (dt, evidence) -> {
//
//        //if (dt > dtTolerance) dt -= dtTolerance;
//
//
//        //return 1f / (1f + (dt*dt)/(duration*duration));
//        //return 1f / (1f + (dt/duration)*(dt/duration));
//        //return 1f / (1f + (dt / duration));
//        //return 1f / (float)Math.log(dt/duration + Math.E);
//        return 1f / (dt/duration + 1f);
//        //return 1f / ((dt*dt)/durationSq + 1f);
//        //return (float)Math.sqrt(1f / (dt/durationSq + 1f));
//        //return (1f / (1f + (float)log(dt + 1f)));
//    };

//    public final WeakHashMap<Task,Float> credit =new WeakHashMap();
//
//    protected void updateCredit() {
//
//        for (double[] ss : s.microsphereData) {
//            int sample = (int)ss[3];
//            if (sample >= 0) {
//                credit.compute(tasks.get(sample), (tt,v) -> {
//                    float ill = (float)ss[0];
//                    if (v == null)
//                        return ill;
//                    else
//                        return v+ill;
//                });
//            }
//
//        }
//
//    }


    public int capacity() {
        return times.length;
    }

    public void print(@NotNull PrintStream out) {
        out.println(Joiner.on("\n").join(this.microsphereData.stream().map(FloatArrayList::new).collect(Collectors.toList())));
    }

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
