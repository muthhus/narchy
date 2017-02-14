package nars.task;

import com.google.common.collect.Lists;
import nars.$;
import nars.Task;
import nars.concept.dynamic.DynamicBeliefTask;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jcog.Util.sqr;
import static nars.Param.TRUTH_EPSILON;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public enum TruthPolation  {
    ;

    /** dt > 0 */
    public static float evidenceDecay(float evi, float dur, float dt) {
        return evi * 1f/( sqr( 1 + dt/dur) ); //inverse square
        //return evi * 1f/( 1f + sqr( dt/dur) ); //inverse square

        //return evi * 1f/( 1 + (dt/dur) ); //inverse
        //return evi * Math.max(0, 1f - dt / dur ); //hard linear
        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    final static float MIN_ILLUMINATION = c2w(TRUTH_EPSILON);

    @Nullable
    public static Truth truth(long when, float dur, @NotNull List<Task> tasks) {
        return truth(null, when, dur, tasks);
    }
    @Nullable
    public static Truth truth(long when, float dur, @NotNull Task... tasks) {
        return truth(null, when, dur, Lists.newArrayList(tasks));
    }

    @Nullable
    public static Truth truth(@Nullable Task topEternal, long when, float dur, @NotNull List<Task> tasks) {

        float weightedValue = 0, illumination = 0;

        int tasksSize = tasks.size();
        if (tasksSize > 0) {


            // Contribution of each sample point to the illumination of the
            // microsphere's facets.
            for (int i1 = 0; i1 < tasksSize; i1++) {
                Task t = tasks.get(i1);
                if (t instanceof DynamicBeliefTask)
                    continue; //ignore dynamic belief tasks

                float tw = t.confWeight(when, dur);
                if (tw > 0) {
                    illumination += tw;
                    weightedValue += tw * t.freq();
                }
            }
        }

        if (topEternal!=null) {
            float ew = topEternal.evi();
            illumination += ew;
            weightedValue += ew * topEternal.freq();
        }

        if (illumination < MIN_ILLUMINATION)
            return null;

        float f = weightedValue / illumination;
        float c = w2c(illumination);

        //System.out.println(when + " " + $.t(f, c) + " (" + weightedValue + " " + illumination + ")\n\t" + Arrays.toString(tasks));

        return $.t(f, c);
    }


//    /**
//     * Microsphere "facet" (surface element).
//     */
//    private static class Facet {
//        /** Normal vector characterizing a surface element. */
//        private final float[] normal;
//
//        /**
//         * @param n Normal vector characterizing a surface element
//         * of the microsphere. No copy is made.
//         */
//        Facet(float[] n) {
//            normal = n;
//        }
//
//        /**
//         * Return a reference to the vector normal to this facet.
//         *
//         * @return the normal vector.
//         */
//        public float[] getNormal() {
//            return normal;
//        }
//    }
//
//    /**
//     * Data associated with each {@link Facet}.
//     */
//    private static class FacetData {
//        /** Illumination received from the sample. */
//        private final float illumination;
//        /** Data value of the sample. */
//        private final float sample;
//
//        /**
//         * @param illumination Illumination.
//         * @param sample Data value.
//         */
//        FacetData(float illumination, float sample) {
//            this.illumination = illumination;
//            this.sample = sample;
//        }
//
//        /**
//         * Get the illumination.
//         * @return the illumination.
//         */
//        public float illumination() {
//            return illumination;
//        }
//
//        /**
//         * Get the data value.
//         * @return the data value.
//         */
//        public float sample() {
//            return sample;
//        }
//    }

//
//    public float[] value(float[] floats, float[][] data, float[] value, float exp, float ulp) {
//        return value(floats, data, value, null, exp, ulp, data.length);
//    }


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



//    /** returns a metric of the usefulness of a given task according to its influence in determining past measurements */
//    public float value(Task t, float valueIfNotKnown) {
//        return credit.getOrDefault(t, valueIfNotKnown);
//    }

}
