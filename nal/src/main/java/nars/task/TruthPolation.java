package nars.task;

import com.google.common.base.Joiner;
import nars.$;
import nars.Task;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.truth.Truth;
import nars.util.list.FasterList;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public final class TruthPolation  {





    public static float evidenceDecay(float evi, float dur, float dt) {
        //return evi / (1f + (dt*dt) / (dur*dur) );
        return evi / (1f + dt / dur );
    }


    /** only used by Tests for now */
    @Deprecated @Nullable
    public Truth truth(long when, @NotNull Collection<Task> tasks) {
        return truth(when, tasks.toArray(new Task[tasks.size()]));
    }

    @Nullable
    public Truth truth(long when, @NotNull Task... tasks) {

        assert(tasks.length > 2);


        float[] v = this.value( when, tasks);

        return $.t(v[0], w2c(v[1]));
    }

        /**
         * Microsphere data.
         */
        @NotNull
        public final float[] microsphereData = new float[4];



//    public void setBackground(float background, float confidence) {
//        this.background = background;
//        this.backgroundConfidence = confidence;
//    }



//    /**
//     * Copy constructor.
//     *
//     * @param other Instance to copy.
//     */
//    protected InterpolatingMicrosphere(InterpolatingMicrosphere other) {
//        dimension = other.dimension;
//        size = other.size;
//        maxDarkFraction = other.maxDarkFraction;
//        darkThreshold = other.darkThreshold;
//        background = other.background;
//
//        // Field can be shared.
//        microsphere = other.microsphere;
//
//        // Field must be copied.
//        microsphereData = Global.newArrayList(size);
//        for (FacetData fd : other.microsphereData) {
//            microsphereData.add(new float[] { fd.illumination(), fd.sample() } );
//        }
//    }



        /**
         * Estimate the value at the requested location.
         * This microsphere is placed at the given {@code point}, contribution
         * of the given {@code samplePoints} to each sphere facet is computed
         * (illumination) and the interpolation is performed (integration of
         * the illumination).
         *
         * @param targetPoint  Interpolation point.
         * @param samplePoints Sampling data points.
         * @param sampleValues Sampling data values at the corresponding
         *                     {@code samplePoints}.
         * @param curve     Exponent used in the power law that computes
         *                     the weights (distance dimming factor) of the sample data.
         * @return the estimated value at the given {@code point}.
         * @throws NotPositiveException if {@code exponent < 0}.
         */
        @NotNull
        public float[] value(@NotNull long targetPoint, Task[] data) {
            clear();

            // Contribution of each sample point to the illumination of the
            // microsphere's facets.
            illuminate(targetPoint, data);

            return interpolate();
        }

        public static float[] ebeSubtract(float[] a, float[] b) throws DimensionMismatchException {
            //checkEqualLength(a, b);
            float[] result = a.clone();

            int l = a.length;
            for (int i = 0; i < l; ++i) {
                result[i] -= b[i];
            }

            return result;
        }

        public static float safeNorm(float epsilon, float[] v) {
            if (v.length == 1) {
                return Math.abs(v[0]);
            }
            boolean zero = true;
            for (float x : v) {
                if (Math.abs(x) > epsilon) {
                    zero = false;
                    break;
                }
            }
            if (zero)
                return 0;

            double rdwarf = 3.834E-20D;
            double rgiant = 1.304E19D;
            double s1 = 0.0D;
            double s2 = 0.0D;
            double s3 = 0.0D;
            double x1max = 0.0D;
            double x3max = 0.0D;
            double floatn = v.length;
            double agiant = rgiant / floatn;

            for (int norm = 0; norm < v.length; ++norm) {
                double xabs = Math.abs(v[norm]);
                if (xabs >= rdwarf && xabs <= agiant) {
                    s2 += xabs * xabs;
                } else {
                    double r;
                    if (xabs > rdwarf) {
                        if (xabs > x1max) {
                            r = x1max / xabs;
                            s1 = 1.0D + s1 * r * r;
                            x1max = xabs;
                        } else {
                            r = xabs / x1max;
                            s1 += r * r;
                        }
                    } else if (xabs > x3max) {
                        r = x3max / xabs;
                        s3 = 1.0D + s3 * r * r;
                        x3max = xabs;
                    } else if (xabs != 0.0D) {
                        r = xabs / x3max;
                        s3 += r * r;
                    }
                }
            }

            double var24;
            if (s1 != 0.0D) {
                var24 = x1max * Math.sqrt(s1 + s2 / x1max / x1max);
            } else if (s2 == 0.0D) {
                var24 = x3max * Math.sqrt(s3);
            } else if (s2 >= x3max) {
                var24 = Math.sqrt(s2 * (1.0D + x3max / s2 * x3max * s3));
            } else {
                var24 = Math.sqrt(x3max * (s2 / x3max + x3max * s3));
            }

            return (float) var24;
        }

        public void illuminate(@NotNull long targetPoint, Task[] data) {


            for (Task t : data) {
                // Vector between interpolation point and current sample point.
                //HACK this only supports 1D points for now

                Truth tt = t.truth(targetPoint);
                if (tt!=null) {
                    final float illumination = tt.confWeight();
                    if (illumination > 0) {
                        final float[] d = microsphereData;
                        d[0] += illumination;
                        d[1] += illumination * tt.freq();
                    }
                }
                //}
            }
        }

        protected static float pow(float x, float y) {
            if (y == 0) {
                return 1;
            } else if (y == -1) {
                return 1.0f / x;
            } else if (y == -0.5f) {
                return 1f / (float)Math.sqrt(y);
            } else if (y == -2) {
                return 1.0f / (x * x);
            } else {
                return (float) Math.pow(x, y);
            }
        }




        /**
         * Interpolation.
         *
         * @return the value estimated from the current illumination of the
         * microsphere.
         */
        @NotNull
        private float[] interpolate() {


            float weightedValue = 0, illumination = 0;


                float[] fd = microsphereData;

                illumination += fd[0];
                weightedValue += fd[1]; /* sample */




            return new float[] { weightedValue / illumination, illumination };
        }


        protected void maxData(float[] d, float illumination, float sampleValue, int sampleNum) {

            d[0] = illumination;

            d[1] = sampleValue;

            d[3] = sampleNum; /* winner */
        }

//    /**
//     * assumes sampleValue in range 0..1
//     */
//    static float valueIntersection(float a, float b) {
//        float s = 1f - Math.abs(a - b);
//        return s;
//        //return s*s;
//    }

        /**
         * Reset the all the {@link Facet facets} data to zero.
         */
        private void clear() {
                float[] d = microsphereData;
                d[0] = d[1] = d[2] = 0;
                d[3] = -1; //DEPRECATED

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
