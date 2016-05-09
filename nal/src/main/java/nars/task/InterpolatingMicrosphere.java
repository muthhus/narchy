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
package nars.task;

import nars.Global;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;
import org.apache.commons.math3.util.MathArrays;

import java.util.List;


/**
 * Utility class for the {@link MicrosphereProjectionInterpolator} algorithm.
 *
 * @since 3.6
 */
public class InterpolatingMicrosphere {
    /** Microsphere. */
    public final List<double[]> microsphere; /* n-element (size) */
    /** Microsphere data. */
    public final List<double[]> microsphereData; /* 2-element illumination, value pairs */
    /** Space dimension. */
    private final int dimension;
    /** Number of surface elements. */
    private final int size;
    /** Maximum fraction of the facets that can be dark. */
    private final double maxDarkFraction;
    /** Lowest non-zero illumination. */
    private final double darkThreshold;
    /** Background value. */
    private double background;
    private double backgroundConfidence;

    /**
     * Create an unitialiazed sphere.
     * Sub-classes are responsible for calling the {@code add(double[]) add}
     * method in order to initialize all the sphere's facets.
     *
     * @param dimension Dimension of the data space.
     * @param size Number of surface elements of the sphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     * If the fraction of "non-illuminated" facets is larger, no estimation
     * of the value will be performed, and the {@code background} value will
     * be returned instead.
     * @param darkThreshold Value of the illumination below which a facet is
     * considered dark.
     * @param background Value returned when the {@code maxDarkFraction}
     * threshold is exceeded.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     * or {@code size <= 0}.
     * @throws NotPositiveException if {@code darkThreshold < 0}.
     * @throws OutOfRangeException if {@code maxDarkFraction} does not
     * belong to the interval {@code [0, 1]}.
     */
    protected InterpolatingMicrosphere(int dimension,
                                       int size,
                                       double maxDarkFraction,
                                       double darkThreshold,
                                       double background) {
        if (dimension <= 0) {
            throw new NotStrictlyPositiveException(dimension);
        }
        if (size <= 0) {
            throw new NotStrictlyPositiveException(size);
        }
        if (maxDarkFraction < 0 ||
            maxDarkFraction > 1) {
            throw new OutOfRangeException(maxDarkFraction, 0, 1);
        }
        if (darkThreshold < 0) {
            throw new NotPositiveException(darkThreshold);
        }

        this.dimension = dimension;
        this.size = size;
        this.maxDarkFraction = maxDarkFraction;
        this.darkThreshold = darkThreshold;
        this.backgroundConfidence = 1.0;
        this.background = background;
        microsphere = Global.newArrayList(size);
        microsphereData = Global.newArrayList(size);


    }

    public void setBackground(double background, double confidence) {
        this.background = background;
        this.backgroundConfidence = confidence;
    }

    /**
     * Create a sphere from randomly sampled vectors.
     *
     * @param dimension Dimension of the data space.
     * @param size Number of surface elements of the sphere.
     * @param rand Unit vector generator for creating the microsphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     * If the fraction of "non-illuminated" facets is larger, no estimation
     * of the value will be performed, and the {@code background} value will
     * be returned instead.
     * @param darkThreshold Value of the illumination below which a facet
     * is considered dark.
     * @param background Value returned when the {@code maxDarkFraction}
     * threshold is exceeded.
     * @throws DimensionMismatchException if the size of the generated
     * vectors does not match the dimension set in the constructor.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     * or {@code size <= 0}.
     * @throws NotPositiveException if {@code darkThreshold < 0}.
     * @throws OutOfRangeException if {@code maxDarkFraction} does not
     * belong to the interval {@code [0, 1]}.
     */
    public InterpolatingMicrosphere(int dimension,
                                    int size,
                                    double maxDarkFraction,
                                    double darkThreshold,
                                    double background,
                                    UnitSphereRandomVectorGenerator rand) {
        this(dimension, size, maxDarkFraction, darkThreshold, background);

        if (dimension == 1) {
            if ((size!=2) || (rand!=null))
                throw new RuntimeException("there are only 2 possible unit vectors in 1D");
            addNormal(new double[] { -1 });
            addNormal(new double[] { +1 });
        } else {

            // Generate the microsphere normals, assuming that a number of
            // randomly generated normals will represent a sphere.
            for (int i = 0; i < size; i++) {
                addNormal(rand.nextVector());
            }
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
//            microsphereData.add(new double[] { fd.illumination(), fd.sample() } );
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
     * @param targetPoint Interpolation point.
     * @param samplePoints Sampling data points.
     * @param sampleValues Sampling data values at the corresponding
     * {@code samplePoints}.
     * @param exponent Exponent used in the power law that computes
     * the weights (distance dimming factor) of the sample data.
     * @return the estimated value at the given {@code point}.
     * @throws NotPositiveException if {@code exponent < 0}.
     */
    public double[]  value(double[] targetPoint,
                           double[][] samplePoints,
                           double[] sampleValues,
                           double[] sampleWeights,
                           double exponent,
                           int numSamples) {
        assert(exponent >= 0);
        if (exponent < 0) {
            throw new NotPositiveException(exponent);
        }

        clear();


        // Contribution of each sample point to the illumination of the
        // microsphere's facets.
        illuminate(targetPoint, samplePoints, sampleValues, sampleWeights, exponent, numSamples, true);
        illuminate(targetPoint, samplePoints, sampleValues, sampleWeights, exponent, numSamples, false);

        return interpolate();

    }

    public void illuminate(double[] targetPoint, double[][] samplePoints, double[] sampleValues, double[] sampleWeights, double exponent, int numSamples, boolean phase) {
        double epsilon = 0.5f;

        for (int i = 0; i < numSamples; i++) {
            // Vector between interpolation point and current sample point.
            final double[] diff = MathArrays.ebeSubtract(samplePoints[i], targetPoint);
            final double diffNorm = Math.max(epsilon, MathArrays.safeNorm(diff));

            /*if (noInterpolationTolerance>0 && Math.abs(diffNorm) < noInterpolationTolerance) {
                // No need to interpolate, as the interpolation point is
                // actually (very close to) one of the sampled points.
                return new double[] { sampleValues[i], sampleWeights == null ? 1f : sampleWeights[i] };
            } else {*/

                double weight = pow(diffNorm, -exponent);

                illuminate(i, diffNorm > 0 ? diff : null, sampleValues[i], weight,
                        sampleWeights == null ? 1f : sampleWeights[i],
                        phase);
            //}
        }
    }

    /**
     * Illumination.
     *
     * @param sampleDirection Vector whose origin is at the interpolation
     * point and tail is at the sample location.
     * @param sampleValue Data value of the sample.
     * @param weight Weight.
     *
     */
    private void illuminate(int sampleNum, double[] sampleDirection,
                            double sampleValue,
                            double weight,
                            double conf, boolean phase) {

        double visibleThreshold = darkThreshold * backgroundConfidence;

        for (int i = 0; i < size; i++) {
            final double[] n = microsphere.get(i);
            final double cos = sampleDirection!=null ? cosAngleNormalized(n, sampleDirection) : 1;

            if (cos > 0) {
                final double illumination = cos * weight;

                double[] dd = microsphereData.get(i);

                if (illumination > visibleThreshold) {
                    if (phase) {
                        if (illumination > dd[0]) {
                            maxData(i, illumination, sampleValue, sampleNum);
                        }
                    } else {
                        if (dd[0] > 0) {
                            confData(i, illumination / dd[0], sampleValue, conf);
                        }
                    }
                }
            }
        }
    }

    protected static double pow(double x, double y) {
        if (y == 0) {
            return 1;
        } else if (y == -1) {
            return 1.0/x;
        } else if (y == -2) {
            return 1.0/(x*x);
        } else {
            return Math.pow(x, y);
        }
    }
    /**
     * Replace {@code i}-th facet of the microsphere.
     * Method for initializing the microsphere facets.
     *
     * @param normal Facet's normal vector.
     * @param copy Whether to copy the given array.
     * @throws DimensionMismatchException if the length of {@code n}
     * does not match the space dimension.
     * @throws MaxCountExceededException if the method has been called
     * more times than the size of the sphere.
     */
    protected void addNormal(double[] normal) {
        if (microsphere.size() >= size) {
            throw new MaxCountExceededException(size);
        }
        if (normal.length > dimension) {
            throw new DimensionMismatchException(normal.length, dimension);
        }

        microsphere.add(normal);
        microsphereData.add(new double[4]);
    }



    /**
     * Interpolation.
     *
     * @return the value estimated from the current illumination of the
     * microsphere.
     */
    private double[] interpolate() {
        // Number of non-illuminated facets.
        int mm = microsphereData.size();
        int size = mm;
        int darkCount = 0;

        double value = 0;
        double totalWeight = 0;
        //double totalConfDen = 0, totalConfNum = 0;
        //double maxConf = 0;
        double confSum = 0;
        for (int i = 0, microsphereDataSize = mm; i < microsphereDataSize; i++) {
            double[] fd = microsphereData.get(i);

            double ill = fd[0]; /* illumination */
            double conf = fd[2];
            if (ill != 0d) {


                ill *= conf;

                value += ill * fd[1]; /* sample */
                totalWeight += ill;

                //maxConf = Math.max(conf*iV, maxConf);
                //totalConfNum += conf * iV; //how much this confidence actually applied to the outcome
            } else {
                ++darkCount;
            }
            confSum += conf;
            //totalConfDen += conf; //total conf contributed
        }

        final double darkFraction = darkCount / (double) size;


        if (((size == darkCount && maxDarkFraction >= 1.0) || (maxDarkFraction < 1.0 && !Double.isFinite(background)))) {
            throw new RuntimeException("no illumination accepted or background value not used or invalid");
        }

        double v = darkFraction <= maxDarkFraction ?
            value / totalWeight :
            background;

        double c = confSum;
        //double c = totalConfDen!=0 ? totalConfNum / totalConfDen : this.backgroundConfidence;
        //double c = totalWeight /                 (size);
                //(microsphereData.size());

        return new double[] {v, c};
    }


    /** assumes input vectors already normalized */
    protected double cosAngleNormalized(double[] x, double[] y) {
        if (x.length == 1) {
            double x0 = x[0];
            double y0 = y[0];
            return (x0 > 0 && y0 > 0) || (x0 < 0 && y0 < 0) ? 1.0 : -1.0;
        } else
            return MathArrays.cosAngle(x, y);
    }

    protected void maxData(int i, double illumination, double sampleValue, int sampleNum) {
        double[] d = microsphereData.get(i);

        d[0] = illumination;

        d[1] = sampleValue;

        d[3] = sampleNum; /* winner */
    }

    /** assumes sampleValue in range 0..1 */
    static double valueIntersection(double a, double b) {
        double s = 1f - Math.abs(a - b);
        return s;
        //return s*s;
    }

    /** accumulate a measure of relevant evidence */
    protected void confData(int i, double illuminationProportion, double sampleValue, double conf) {
        double[] d = microsphereData.get(i);

        //d[0] illumination doesnt change

        double existingValue = d[1];

        //add the amount of confidence in proportion to how equal the frequency (sampleValue) is
        if (!Double.isFinite(conf)) {
            throw new RuntimeException("?");
        }

        d[2] += conf * illuminationProportion * valueIntersection(existingValue, sampleValue);
    }

    /**
     * Reset the all the {@link Facet facets} data to zero.
     */
    private void clear() {
        for (int i = 0; i < size; i++) {
            double[] d = microsphereData.get(i);
            d[0] = d[1] = d[2] = 0; d[3] = -1;
        }
    }

//    /**
//     * Microsphere "facet" (surface element).
//     */
//    private static class Facet {
//        /** Normal vector characterizing a surface element. */
//        private final double[] normal;
//
//        /**
//         * @param n Normal vector characterizing a surface element
//         * of the microsphere. No copy is made.
//         */
//        Facet(double[] n) {
//            normal = n;
//        }
//
//        /**
//         * Return a reference to the vector normal to this facet.
//         *
//         * @return the normal vector.
//         */
//        public double[] getNormal() {
//            return normal;
//        }
//    }
//
//    /**
//     * Data associated with each {@link Facet}.
//     */
//    private static class FacetData {
//        /** Illumination received from the sample. */
//        private final double illumination;
//        /** Data value of the sample. */
//        private final double sample;
//
//        /**
//         * @param illumination Illumination.
//         * @param sample Data value.
//         */
//        FacetData(double illumination, double sample) {
//            this.illumination = illumination;
//            this.sample = sample;
//        }
//
//        /**
//         * Get the illumination.
//         * @return the illumination.
//         */
//        public double illumination() {
//            return illumination;
//        }
//
//        /**
//         * Get the data value.
//         * @return the data value.
//         */
//        public double sample() {
//            return sample;
//        }
//    }

//
//    public double[] value(double[] doubles, double[][] data, double[] value, double exp, double ulp) {
//        return value(doubles, data, value, null, exp, ulp, data.length);
//    }

}