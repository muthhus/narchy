package nars.task;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import nars.$;
import nars.Global;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static nars.nal.UtilityFunctions.w2c;
import static nars.truth.TruthFunctions.c2w;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation extends InterpolatingMicrosphere {

    public static final float[] ZERO = { 0 };

    @NotNull final float[][] times;
    @NotNull final float[] freq;
    @NotNull final float[] conf;
    private static final Truth EterNull = $.t(0.5f, Global.TRUTH_EPSILON);

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
        return truth(when, Lists.newArrayList(tasks), null);
    }

    public Truth truth(long when, @NotNull List<Task> tasks, @Nullable Truthed topEternal) {
        //float ecap = eternal.capacity();
        //float eternalization = ecap / (ecap + tcap));

        float maxDarkFraction = 1 - (0.5f / (1f + tasks.size()));

        float thresh = Global.TRUTH_EPSILON/2f; //c2w(Global.TRUTH_EPSILON);
        if (topEternal == null) {
            return truth(when, tasks, EterNull, maxDarkFraction, thresh);
        } else {
            //TODO maybe weight by relative confidence and the sum of conf in the list of tasks
            return truth(when, tasks, topEternal, maxDarkFraction, thresh);
        }
    }


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
            //times[i][0] = (((double)t.occurrence() - tmin) / range); //NORMALIZED TO ITS OWN RANGE

            //offset the specified occurence time to a small window around the pure occurrence time,
            //so that tasks with equivalent truths but different evidence (and thus different hash) will
            //have a slightly different position on the time axis

            //-when added is shifting relative to the target time, so the queried interpolation time will equal zero below
            //this helps the floating point precision in calculations with numbers close together

            float window = 0.01f;

            times[i][0] = -when + t.occurrence() + (window * (-1f + 2f * (i)/(((float)n-1))  ));  /* keeps occurrence times unique */
            freq[i] = t.freq();
            conf[i] = t.confWeight();

            //TODO dt
        }

        if (topEternal!=null) {
            this.setBackground(topEternal.freq(), topEternal.confWeight());
        } else {
            this.setBackground(Float.NaN, 0);
        }

        float exp = Global.TEMPORAL_MICROSPHERE_EXPONENT;
        float[] v = this.value(
                ZERO, times,
                freq, conf,
                exp,
                maxDarkFraction, darkThresold,
                n);

        return $.t(v[0], w2c(v[1]));
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

    public void print() {
        System.out.println(Joiner.on("\n").join(this.microsphereData.stream().map(FloatArrayList::new).collect(Collectors.toList())));
    }

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
