package nars.task;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import nars.$;
import nars.Global;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static nars.nal.UtilityFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public class TruthPolation {

    final @Nullable InterpolatingMicrosphere s;
    @NotNull
    final float[][] times;
    @NotNull
    final float[] freq;
    @NotNull
    final float[] conf;
    int count;

    public TruthPolation(int size) {
        s = new InterpolatingMicrosphere(1, 2 /* must be 2 for 1D */, null);

        times = new float[size][];
        for (int i = 0; i < size; i++) {
            times[i] = new float[1];
        }
        freq = new float[size];
        conf = new float[size];

        count = 0;
    }

    @Nullable
    public Truth truth(long when, Task... tasks) {
        return truth(when, Lists.newArrayList(tasks), null);
    }



    public Truth truth(long when, @NotNull List<Task> tasks, @Nullable Task topEternal) {
        //float ecap = eternal.capacity();
        //float eternalization = ecap / (ecap + tcap));

        float eternalization = topEternal!=null ? 1f/(1+tasks.size()) : 0f; //TODO maybe weight by relative confidence and the sum of conf in the list of tasks

        return truth(when, tasks, topEternal,
                1f-eternalization,
                topEternal!= null ? Global.TRUTH_EPSILON : 0);
    }


    public Truth truth(long when, @NotNull List<Task> tasks, @Nullable Task topEternal, /* background */float maxDarkFraction, float darkThresold) {
        assert(times.length <= tasks.size());

        int s = tasks.size();
        if (s == 1)
            return tasks.get(0).truth();

        this.count = s;
        //this.tasks = tasks;

        float sum = 0;
        for (int i = 0; i < s; i++) {
            Task t = tasks.get(i);
            //sum += t.confWeight();
            sum += t.conf();
        }

        for (int i = 0; i < s; i++) {
            Task t = tasks.get(i);
            //times[i][0] = (((double)t.occurrence() - tmin) / range); //NORMALIZED TO ITS OWN RANGE

            //offset the specified occurence time to a small window around the pure occurrence time,
            //so that tasks with equivalent truths but different evidence (and thus different hash) will
            //have a slightly different position on the time axis

            //-when added is shifting relative to the target time, so the queried interpolation time will equal zero below
            //this helps the floating point precision in calculations with numbers close together

            float window = 0.01f;
            times[i][0] = -when + t.occurrence() + (window * (-1f + 2f * (i)/(((float)s-1))  ));  /* keeps occurrence times unique */

            freq[i] = t.freq();
            conf[i] = t.confWeight()/sum;


            //TODO dt
        }



        if (topEternal!=null) {
            this.s.setBackground(topEternal.freq(), topEternal.conf());
        } else {
            this.s.setBackground(Float.NaN, 0);
        }

        //double whenNormalized = ((double)when - tmin) / range;


        float exp = 2f;
        float[] v = this.s.value(new float[]{
                0
        }, times, freq, conf, exp,
                //(((range == 0) && (when == tmin)) ? -1 : 0.5), /* if no range, always interpolate since otherwise repeat points wont accumulate confidence */
                maxDarkFraction, darkThresold,
                s);

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
        System.out.println(Joiner.on("\n").join(this.s.microsphereData.stream().map(FloatArrayList::new).collect(Collectors.toList())));
    }

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
