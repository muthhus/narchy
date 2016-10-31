package nars.task;

import com.google.common.base.Joiner;
import nars.$;
import nars.Task;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.eternalize;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation extends InterpolatingMicrosphere {

    /* points x { 0=time, 1=freq, 2=conf } */
    @NotNull final float[][] data;
//    @NotNull final float[] freq;
//    @NotNull final float[] conf;

    //private final static boolean clipToBeliefs = true;


    //@Nullable private static final Truth EterNull = $.t(0.5f, Param.TRUTH_EPSILON);

    public TruthPolation(int size) {
        super(1, 2 /* must be 2 for 1-D microsphere */, null);

        data = new float[size][3 /* 0=time, 1=freq, 2=conf */];

    }

    public static float timeDecay(float evi, float dur, float dt) {
        //assert(dt > 0);
//        if (dt < 0)
//            throw new UnsupportedOperationException();

        return evi / (1f +
            dt / dur //1st-order linear decay
        );
    }

    @Deprecated private static Focus decayCurve(float dur) {
        return (dt, evi) -> timeDecay(evi, dur, dt);
    }

    /** only used by Tests for now */
    @Deprecated @Nullable
    public Truth truth(long when, float dur, @NotNull Task... tasks) {
        return truth(when, tasks, decayCurve(dur));
    }

    /** only used by Tests for now */
    @Deprecated @Nullable
    public Truth truth(long when, float dur, @NotNull Collection<Task> tasks) {
        return truth(when, tasks.toArray(new Task[tasks.size()]), decayCurve(dur));
    }

    @Nullable
    public Truth truth(long when, @NotNull Task[] tasks, Focus focus) {

        assert(tasks.length > 2);

        //long minT = Long.MAX_VALUE, maxT = Long.MIN_VALUE;

        int i = 0;
        for (Task t : tasks) {

            float[] f = data[i];

            long o = t.occurrence();
            f[0] = (o != ETERNAL) ? o : when;
            f[1] = t.freq();
            f[2] = t.confWeight();

            //if (minT > o) minT = o;
            //if (maxT < o) maxT = o;

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
                data,
                focus,
                i);

        return $.t(v[0], w2c(v[1]));
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
        return data.length;
    }

    public void print(@NotNull PrintStream out) {
        out.println(Joiner.on("\n").join(this.microsphereData.stream().map(FloatArrayList::new).collect(Collectors.toList())));
    }

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
