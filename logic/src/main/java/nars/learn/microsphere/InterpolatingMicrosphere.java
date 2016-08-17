/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nars.learn.microsphere;


import nars.util.data.list.FasterList;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * Utility class for the {@link MicrosphereProjectionInterpolator} algorithm.
 *
 * @since 3.6
 */
public class InterpolatingMicrosphere {
    /**
     * Microsphere.
     */
    @NotNull
    public final List<float[]> microsphere; /* n-element (size) */
    /**
     * Microsphere data.
     */
    @NotNull
    public final List<float[]> microsphereData; /* 2-element illumination, value pairs */
    /**
     * Space dimension.
     */
    private final int dimension;
    /**
     * Number of surface elements.
     */
    private final int size;
//    /** Maximum fraction of the facets that can be dark. */
//    private float maxDarkFraction;
//    /** Lowest non-zero illumination. */
//    private float darkThreshold;
//    /** Background value. */
//    private float background;
//    private float backgroundConfidence;

    /**
     * Create an unitialiazed sphere.
     * Sub-classes are responsible for calling the {@code add(float[]) add}
     * method in order to initialize all the sphere's facets.
     *
     * @param dimension       Dimension of the data space.
     * @param size            Number of surface elements of the sphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     *                        If the fraction of "non-illuminated" facets is larger, no estimation
     *                        of the value will be performed, and the {@code background} value will
     *                        be returned instead.
     * @param darkThreshold   Value of the illumination below which a facet is
     *                        considered dark.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     *                                      or {@code size <= 0}.
     * @throws NotPositiveException         if {@code darkThreshold < 0}.
     * @throws OutOfRangeException          if {@code maxDarkFraction} does not
     *                                      belong to the interval {@code [0, 1]}.
     */
    protected InterpolatingMicrosphere(int dimension,
                                       int size) {
        if (dimension <= 0) {
            throw new NotStrictlyPositiveException(dimension);
        }
        if (size <= 0) {
            throw new NotStrictlyPositiveException(size);
        }


        this.dimension = dimension;
        this.size = size;
        //this.backgroundConfidence = 1.0f;
        microsphere = new FasterList(size);
        microsphereData = new FasterList(size);


    }

//    public void setBackground(float background, float confidence) {
//        this.background = background;
//        this.backgroundConfidence = confidence;
//    }

    /**
     * Create a sphere from randomly sampled vectors.
     *
     * @param dimension       Dimension of the data space.
     * @param size            Number of surface elements of the sphere.
     * @param rand            Unit vector generator for creating the microsphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     *                        If the fraction of "non-illuminated" facets is larger, no estimation
     *                        of the value will be performed, and the {@code background} value will
     *                        be returned instead.
     * @param darkThreshold   Value of the illumination below which a facet
     *                        is considered dark.
     * @param background      Value returned when the {@code maxDarkFraction}
     *                        threshold is exceeded.
     * @throws DimensionMismatchException   if the size of the generated
     *                                      vectors does not match the dimension set in the constructor.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     *                                      or {@code size <= 0}.
     * @throws NotPositiveException         if {@code darkThreshold < 0}.
     * @throws OutOfRangeException          if {@code maxDarkFraction} does not
     *                                      belong to the interval {@code [0, 1]}.
     */
    public InterpolatingMicrosphere(int dimension,
                                    int size,
                                    @Nullable UnitSphereRandomVectorGenerator rand) {
        this(dimension, size);

        if (dimension == 1) {
            if ((size != 2) || (rand != null))
                throw new RuntimeException("there are only 2 possible unit vectors in 1D");
            addNormal(new float[]{-1});
            addNormal(new float[]{+1});
        } else {

            throw new UnsupportedOperationException("TODO support float[] conversion from UnitSphereRandomVectorGenerator.nextVector()");

//            // Generate the microsphere normals, assuming that a number of
//            // randomly generated normals will represent a sphere.
//            for (int i = 0; i < size; i++) {
//                addNormal(rand.nextVector());
//            }
        }
    }

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

//    /**
//     * Perform a copy.
//     *
//     * @return a copy of this instance.
//     */
//    public InterpolatingMicrosphere copy() {
//        return new InterpolatingMicrosphere(this);
//    }

    /**
     * Get the space dimensionality.
     *
     * @return the number of space dimensions.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get the size of the sphere.
     *
     * @return the number of surface elements of the microspshere.
     */
    public int getSize() {
        return size;
    }

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
     * @param exponent     Exponent used in the power law that computes
     *                     the weights (distance dimming factor) of the sample data.
     * @return the estimated value at the given {@code point}.
     * @throws NotPositiveException if {@code exponent < 0}.
     */
    @Nullable
    public float[] value(@NotNull float[] targetPoint,
                         float[][] samplePoints,
                         float[] sampleValues,
                         float[] sampleWeights,
                         float exponent,
                         float minWeight,
                         float darkThreshold,
                         int numSamples) {


//
//        if (maxDarkFraction < 0 ||
//                maxDarkFraction > 1) {
//            throw new OutOfRangeException(maxDarkFraction, 0, 1);
//        }
        if (darkThreshold < 0) {
            throw new NotPositiveException(darkThreshold);
        }

        assert (exponent >= 0);


        clear();


        // Contribution of each sample point to the illumination of the
        // microsphere's facets.
        illuminate(targetPoint, samplePoints, sampleValues, sampleWeights, exponent, numSamples, darkThreshold);

        return interpolate(minWeight);

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
        double floatn = (double) v.length;
        double agiant = rgiant / floatn;

        for (int norm = 0; norm < v.length; ++norm) {
            double xabs = FastMath.abs(v[norm]);
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

    public void illuminate(@NotNull float[] targetPoint, float[][] samplePoints, float[] sampleValues, @Nullable float[] sampleWeights, float exponent, int numSamples, float darkThreshold) {
        float epsilon = 0.01f;

        for (int i = 0; i < numSamples; i++) {
            // Vector between interpolation point and current sample point.
            final float[] diff = ebeSubtract(samplePoints[i], targetPoint);
            final float diffNorm = safeNorm(epsilon, diff);

            /*if (noInterpolationTolerance>0 && Math.abs(diffNorm) < noInterpolationTolerance) {
                // No need to interpolate, as the interpolation point is
                // actually (very close to) one of the sampled points.
                return new float[] { sampleValues[i], sampleWeights == null ? 1f : sampleWeights[i] };
            } else {*/

            float weight = pow(1f + diffNorm, -exponent);

            illuminate(i, diffNorm > 0 ? diff : null, sampleValues[i],
                    weight,
                    sampleWeights == null ? 1f : sampleWeights[i],
                    darkThreshold);
            //}
        }
    }

    /**
     * Illumination.
     *
     * @param sampleDirection Vector whose origin is at the interpolation
     *                        point and tail is at the sample location.
     * @param sampleValue     Data value of the sample.
     * @param weight          Weight.
     */
    private void illuminate(int sampleNum, @Nullable float[] sampleDirection,
                            float sampleValue,
                            float weight,
                            float conf, float darkThreshold) {

        float visibleThreshold = 0; // darkThreshold * backgroundConfidence;


        for (int i = 0; i < size; i++) {
            final float[] n = microsphere.get(i);
            final float cos = sampleDirection != null ? cosAngleNormalized(n, sampleDirection) : 1f;

            if (cos > 0) {
                final float illumination = cos * weight;


                if (illumination > visibleThreshold) {
                    //if (phase) {
//                        if (illumination > dd[0]) {
//                            maxData(dd, illumination, sampleValue, sampleNum);
//                        }
                    float[] dd = microsphereData.get(1);

                    record(dd, illumination, sampleValue, conf);

                    //} else {
//                        if (dd[0] > 0) {
//                            confData(i, illumination / dd[0], sampleValue, conf);
//                        }
                    //}
                }
            }
        }
    }

    protected static float pow(float x, float y) {
        if (y == 0) {
            return 1;
        } else if (y == -1) {
            return 1.0f / x;
        } else if (y == -2) {
            return 1.0f / (x * x);
        } else {
            return (float) Math.pow(x, y);
        }
    }

    /**
     * Replace {@code i}-th facet of the microsphere.
     * Method for initializing the microsphere facets.
     *
     * @param normal Facet's normal vector.
     * @param copy   Whether to copy the given array.
     * @throws DimensionMismatchException if the length of {@code n}
     *                                    does not match the space dimension.
     * @throws MaxCountExceededException  if the method has been called
     *                                    more times than the size of the sphere.
     */
    protected void addNormal(@NotNull float[] normal) {
        if (microsphere.size() >= size) {
            throw new MaxCountExceededException(size);
        }
        if (normal.length > dimension) {
            throw new DimensionMismatchException(normal.length, dimension);
        }

        microsphere.add(normal);
        microsphereData.add(new float[4]);
    }


    /**
     * Interpolation.
     *
     * @return the value estimated from the current illumination of the
     * microsphere.
     */
    @Nullable
    private float[] interpolate(float minWeight) {

        int size = this.size;

        int darkCount = 0;

        float value = 0;
        float totalWeight = 0, totalConf = 0;
        //float totalConfDen = 0, totalConfNum = 0;
        //float maxConf = 0;

        for (int i = 0; i < size; i++) {
            float[] fd = microsphereData.get(i);

            float ill = fd[0]; /* weighted illumination */
            float conf = fd[2];


            //float conf = fd[2];
            if (ill != 0d) {

                value += fd[1]; /* sample */
                totalConf += (conf);
                totalWeight += (ill);


                //maxConf = Math.max(conf*iV, maxConf);
                //totalConfNum += conf * iV; //how much this confidence actually applied to the outcome
            } else {
                ++darkCount;
            }

        }

        //final float darkFraction = darkCount / (float) size;


        if ((size == darkCount /*&& maxDarkFraction >= 1.0f*/)/* || (mdf < 1.0 && !float.isFinite(background)))*/) {
            throw new RuntimeException("no illumination accepted or background value not used or invalid");
        }

        if (totalWeight < minWeight)
            return null;

        float v = value / totalWeight;

        float c = totalConf;
        //float c = totalConfDen!=0 ? totalConfNum / totalConfDen : this.backgroundConfidence;
        //float c = totalWeight /                 (size);
        //(microsphereData.size());

        return new float[]{v, c};
    }


    /**
     * assumes input vectors already normalized
     */
    protected static float cosAngleNormalized(@NotNull float[] x, @NotNull float[] y) {
        if (x.length == 1) {
            float x0 = x[0];
            float y0 = y[0];
            return (x0 > 0 && y0 > 0) || (x0 < 0 && y0 < 0) ? 1.0f : -1.0f;
        } else
            return cosAngle(x, y);
    }

    private static float cosAngle(@NotNull float[] x, @NotNull float[] y) {
        throw new UnsupportedOperationException("TODO copy from MathArrays's double[] version");
    }

    public static float linearCombination(float[] a, float[] b) throws DimensionMismatchException {
        //checkEqualLength(a, b);
        int len = a.length;
        if (len == 1) {
            return a[0] * b[0];
        } else {
            throw new UnsupportedOperationException("TODO copy from MathArrays's double[] version");
        }
    }

    protected void maxData(float[] d, float illumination, float sampleValue, int sampleNum) {

        d[0] = illumination;

        d[1] = sampleValue;

        d[3] = sampleNum; /* winner */
    }

    /**
     * assumes sampleValue in range 0..1
     */
    static float valueIntersection(float a, float b) {
        float s = 1f - Math.abs(a - b);
        return s;
        //return s*s;
    }

    /**
     * accumulate a measure of relevant evidence
     */
    protected void record(float[] d, float illuminationProportion, float sampleValue, float conf) {

        //d[0] illumination doesnt change

        //float existingValue = d[1];

        //add the amount of confidence in proportion to how equal the frequency (sampleValue) is
        /*if (conf!=conf) { //!Float.isFinite(conf)) {
            throw new RuntimeException("?");
        }*/


        d[0] += illuminationProportion;
        d[1] += (illuminationProportion) * sampleValue; //(conf * illuminationProportion) * sampleValue; //weighted value
        d[2] += conf * illuminationProportion;
        //d[2] += conf; //valueIntersection(existingValue, sampleValue);
    }

    /**
     * Reset the all the {@link Facet facets} data to zero.
     */
    private void clear() {
        for (int i = 0; i < size; i++) {
            float[] d = microsphereData.get(i);
            d[0] = d[1] = d[2] = 0;
            d[3] = -1; //DEPRECATED
        }
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

}