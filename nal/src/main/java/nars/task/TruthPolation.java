package nars.task;

import com.google.common.base.Joiner;
import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import nars.$;
import nars.Param;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import static nars.nal.UtilityFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation extends InterpolatingMicrosphere {

    public static final float[] ZERO = { 0 };

    @NotNull final float[][] times;
    @NotNull final float[] freq;
    @NotNull final float[] conf;
    @Nullable
    private static final Truth EterNull = $.t(0.5f, Param.TRUTH_EPSILON);

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
    public Truth truth(long when, Task... tasks) {
        return truth(when, null, tasks);
    }

    @Nullable
    public Truth truth(long when, @Nullable Truthed topEternal, Task... tasks) {
        return truth(when, new FasterList<>((Task[]) tasks), topEternal);
    }

    @Nullable
    public Truth truth(long when, @NotNull List<Task> tasks, @Nullable Truthed topEternal) {
        //float ecap = eternal.capacity();
        //float eternalization = ecap / (ecap + tcap));

        float maxDarkFraction = 1f - Param.TRUTH_EPSILON; // - (0.5f / (1f + tasks.size()));

        float thresh = Param.TRUTH_EPSILON/2f; //c2w(Global.TRUTH_EPSILON);

        return truth(when, tasks,
                (topEternal == null) ? EterNull : topEternal,
                maxDarkFraction, thresh);

    }


    @Nullable
    public Truth truth(long when, @NotNull List<Task> tasks, @Nullable Truthed topEternal, /* background */float maxDarkFraction, float darkThresold) {
        assert(times.length <= tasks.size());

        int n = tasks.size();
        assert(n >= 2);

//        float sum = 0;
//        for (int i = 0; i < n; i++) {
//            Task t = tasks.get(i);
//            sum += t.confWeight();
//            //sum += t.conf();
//        }
//        System.out.println(tasks + " sum=" + sum);

        for (int i = 0; i < n; i++) {
            Task t = tasks.get(i);
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

            float window = 0.01f;

            times[i][0] = -when + t.occurrence() + (window * (-1f + 2f * (i)/(((float)n-1))  ));  /* keeps occurrence times unique */
            freq[i] = t.freq();
            conf[i] =
                    t.conf();
                    //t.confWeight();

            //TODO dt
        }

        if (topEternal!=null) {
            this.setBackground(topEternal.freq(), topEternal.confWeight());
        } else {
            this.setBackground(Float.NaN, 0);
        }

        float exp = Param.TEMPORAL_MICROSPHERE_EXPONENT;
        float[] v = this.value(
                ZERO, times,
                freq, conf,
                exp,
                maxDarkFraction, darkThresold,
                n);

        return $.t(v[0], /*w2c*/(v[1]));
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
