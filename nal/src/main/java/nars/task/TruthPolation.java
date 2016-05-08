package nars.task;

import nars.Global;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;

import java.util.List;

import static nars.nal.UtilityFunctions.w2c;
import static nars.truth.TruthFunctions.c2w;

/**
 * Interpolation and Extrapolation of Temporal Belief Values
 */
public class TruthPolation {

    final InterpolatingMicrosphere s;
    double[][] times;
    double[] freq;
    double[] conf;
    int count;
    private List<Task> tasks;

    public TruthPolation(int size, float eternalization) {
        s = new InterpolatingMicrosphere(1, size * 4,
                1f - eternalization,  //ratio of dark before eternal is used
                eternalization != 0 ? Global.TRUTH_EPSILON : 0,
                0.5f,
                new UnitSphereRandomVectorGenerator(1));

        times = new double[size][];
        for (int i = 0; i < size; i++) {
            times[i] = new double[1];
        }
        freq = new double[size];
        conf = new double[size];

        count = 0;
    }

    public Truth value(long when, List<Task> tasks, Task topEternal /* background */) {
        assert(times.length <= tasks.size());

        int s = tasks.size();

        this.count = s;
        this.tasks = tasks;

        long tmin = Long.MAX_VALUE, tmax = Long.MIN_VALUE;
        for (int i = 0; i < s; i++) {
            Task t = tasks.get(i);
            long o = t.occurrence();
            if (o < tmin) tmin = o;
            if (o > tmax) tmax = o;
        }
        if (tmin == tmax) {  tmax++; } //just expand one unit around

        long range = tmax - tmin;
        for (int i = 0; i < s; i++) {
            Task t = tasks.get(i);
            times[i][0] = (((double)t.occurrence() - tmin) / range); //NORMALIZED TO ITS OWN RANGE
            freq[i] = t.freq();
            conf[i] = c2w(t.conf());
            //TODO dt
        }

        if (topEternal!=null) {
            this.s.setBackground(topEternal.freq(), topEternal.conf());
        } else {
            this.s.setBackground(Float.NaN, 0);
        }

        double whenNormalized = ((double)when - tmin) / range;
        double[] v = this.s.value(new double[]{whenNormalized}, times, freq, conf, 1, 0.5 / range, count);
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



    public static void main(String[] args) {
        TruthPolation p = new TruthPolation(4,
                0f);
                //0.1f);

        List<Task> l = Global.newArrayList();

        //NAR n = new Default();
        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.5f) ).occurr(0).setCreationTime(0) );
        l.add( new MutableTask("a:b", '.', new DefaultTruth(1f, 0.5f) ).occurr(5).setCreationTime(0) );
        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.75f) ).occurr(10).setCreationTime(0) );


        //interpolation (revision) and extrapolation (projection)
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = -4; d < 15; d++) {
            Truth a1 = p.value(d, l, null);
            System.out.println(d + ": " + a1);
        }

    }

    public int capacity() {
        return times.length;
    }

//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
