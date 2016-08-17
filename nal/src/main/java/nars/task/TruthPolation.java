package nars.task;

import com.google.common.base.Joiner;
import nars.$;
import nars.Param;
import nars.Task;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static nars.nal.UtilityFunctions.w2c;
import static nars.truth.TruthFunctions.c2w;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation extends InterpolatingMicrosphere {

    public static final float[] ZEROTIME = { 0f };

    @NotNull final float[][] times;
    @NotNull final float[] freq;
    @NotNull final float[] conf;
    @Nullable
    //private static final Truth EterNull = $.t(0.5f, Param.TRUTH_EPSILON);

    public TruthPolation(int size) {
        super(1, 2 /* must be 2 for 1D */, null);

        times = new float[size][];
        for (int i = 0; i < size; i++) {
            times[i] = new float[1];
        }
        freq = new float[size];
        conf = new float[size];

    }



    @Nullable
    public Truth truth(long when, @NotNull Collection<Task> tasks) {
        return truth(when, tasks.toArray(new Task[tasks.size()]));
    }

    @Nullable
    public Truth truth(long when, @NotNull Task... tasks) {
        //float ecap = eternal.capacity();
        //float eternalization = ecap / (ecap + tcap));

        @Deprecated float minWeight = 0; //Param.TRUTH_EPSILON; // - (0.5f / (1f + tasks.size()));

        @Deprecated float thresh = 0; //Param.TRUTH_EPSILON;

        return truth(when, tasks, minWeight, thresh);

    }


    @Nullable
    public Truth truth(long when, @NotNull Task[] tasks,  /* background */float minWeight, float darkThresold) {
        int n = tasks.length;
        assert(times.length <= n);

        assert(n >= 2);

//        long minT, maxT;
//        minT = maxT = tasks.get(0).occurrence();
//        for (int i = 1; i < n; i++) {
//            long o = tasks.get(i).occurrence();
//            if (minT > o) minT = o;
//            if (maxT < o) maxT = o;
//
////            sum += t.confWeight();
////            //sum += t.conf();
//        }
//        //clip the target time point to the range of values, so that the value latches at the last known point
//        when = Math.min(when, maxT);
//        when = Math.max(when, minT);

//        System.out.println(tasks + " sum=" + sum);

        int i = 0;
        for (Task t : tasks) {

            if (t == null) {
                n--;
                continue;
            }
            //times[i][0] = (((double)t.occurrence() - tmin) / range); //NORMALIZED TO ITS OWN RANGE

            //offset the specified occurence time to a small window around the pure occurrence time,
            //so that tasks with equivalent truths but different evidence (and thus different hash) will
            //have a slightly different position on the time axis

            //-when added is shifting relative to the target time, so the queried interpolation time will equal zero below
            //this helps the floating point precision in calculations with numbers close together

            //float window = 0.01f;

            times[i][0] = -when + t.occurrence();// + (window * (-1f + 2f * (i)/(((float)n-1))  ));  /* keeps occurrence times unique */
            freq[i] = t.freq();

            float c = Math.min(t.conf(), 1f-Param.TRUTH_EPSILON); //clip maximum confidence
            conf[i] = c2w(c);
            //TODO dt

            i++;
        }

//        if (topEternal!=null) {
//            this.setBackground(topEternal.freq(), topEternal.confWeight());
//
//        } else {
//            this.setBackground(0.5f, 0);
//        }

        float exp = Param.TEMPORAL_MICROSPHERE_EXPONENT;
        float[] v = this.value(
                ZEROTIME,
                times,
                freq, conf,
                exp,
                minWeight, darkThresold,
                n);
        if (v!=null)
            return $.t(v[0], w2c(v[1]));
        return null;
    }


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
