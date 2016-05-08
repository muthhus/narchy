package nars.task;

import com.google.common.collect.Lists;
import nars.Global;
import nars.truth.DefaultTruth;
import nars.truth.Truth;

import java.util.List;

import static nars.nal.UtilityFunctions.w2c;
import static nars.truth.TruthFunctions.c2w;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public class TruthPolation {

    final InterpolatingMicrosphere s;
    double[][] times;
    double[] freq;
    double[] conf;
    int count;
    private List<Task> tasks;
    final private double exp = 1.1f;

    public TruthPolation(int size, float eternalization) {
        s = new InterpolatingMicrosphere(1, 2,
                1f - eternalization,  //ratio of dark before eternal is used
                eternalization != 0 ? Global.TRUTH_EPSILON : 0,
                0.5f,
                null);

        times = new double[size][];
        for (int i = 0; i < size; i++) {
            times[i] = new double[1];
        }
        freq = new double[size];
        conf = new double[size];

        count = 0;
    }

    public Truth truth(long when, Task... tasks) {
        return truth(when, Lists.newArrayList(tasks), null);
    }

    public Truth truth(long when, List<Task> tasks) {
        return truth(when, tasks, null);
    }

    public Truth truth(long when, List<Task> tasks, Task topEternal /* background */) {
        assert(times.length <= tasks.size());

        int s = tasks.size();
        if (s == 1)
            return tasks.get(0).truth();

        this.count = s;
        this.tasks = tasks;

//        long tmin = Long.MAX_VALUE, tmax = Long.MIN_VALUE;
//        for (int i = 0; i < s; i++) {
//            Task t = tasks.get(i);
//            long o = t.occurrence();
//            if (o < tmin) tmin = o;
//            if (o > tmax) tmax = o;
//        }
//        if (tmin == tmax) {  tmax++; } //just expand one unit around

//        long range = tmax - tmin;
        for (int i = 0; i < s; i++) {
            Task t = tasks.get(i);
            //times[i][0] = (((double)t.occurrence() - tmin) / range); //NORMALIZED TO ITS OWN RANGE
            times[i][0] = t.occurrence();
            freq[i] = t.freq();
            conf[i] = c2w(t.conf());
            //TODO dt
        }

        if (topEternal!=null) {
            this.s.setBackground(topEternal.freq(), topEternal.conf());
        } else {
            this.s.setBackground(Float.NaN, 0);
        }

        //double whenNormalized = ((double)when - tmin) / range;

        double[] v = this.s.value(new double[]{when}, times, freq, conf, exp,
                -1 /* always interpolate, otherwise repeat points wont accumulate confidence */,
                count);
        return new DefaultTruth( (float)v[0], w2c( (float) v[1]));

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

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
