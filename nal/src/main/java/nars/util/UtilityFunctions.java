/*
 * UtilityFunctions.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.util;

import org.jetbrains.annotations.NotNull;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Common (static) functions on real numbers, mostly in [0,1].
 */
public class UtilityFunctions   {




//    /**
//     * A function where the output is conjunctively determined by the inputs
//     * @param arr The inputs, each in [0, 1]
//     * @return The output that is no larger than each input
//     */
//    public static float and(@NotNull float... arr) {
//        float product = 1;
//        for (float f : arr) {
//            product *= f;
//        }
//        return product;
//    }

    public static float and(float a, float b) {
        return a*b;
    }



    public static float and(float a, float b, float c) {
        return a*b*c;
    }

    public static float and(float a, float b, float c, float d) {
        return a*b*c*d;
    }


    /**
     * A function where the output is the arithmetic average the inputs
     * @param arr The inputs, each in [0, 1]
     * @return The arithmetic average the inputs
     */
    public static float aveAri(@NotNull float... arr) {
        float sum = 0;
        for (float f : arr) {
            sum += f;
        }
        return sum / arr.length;
    }

    /** more efficient version */
    public static float aveAri(float a, float b) {
        return (a + b) / 2.0f;
    }


    /**
     * A function where the output is the geometric average the inputs
     * @param arr The inputs, each in [0, 1]
     * @return The geometric average the inputs
     */
    public static float aveGeo(@NotNull float... arr) {
        float product = 1;
        for (float f : arr) {
            if (f == 0) return 0;
            product *= f;
        }
        return (float) pow(product, 1.00 / arr.length);
    }

    //may be more efficient than the for-loop version above, for 2 params
    public static float aveGeo(float a, float b) {
//        float inner = (a*b);
//        if (inner < Float.MIN_NORMAL) //early test to avoid sqrt()
//            return 0;
//        else
        return (float)sqrt(a*b);
    }



    /*
    curved-sawtooth function:
        the left-half of this wave is convex, like capacitor charging.
        the right-half of the wave is concave, like capacitor discharging

        for efficiency, dont use the exponential function which circuit models
        use but instead use the circle which can be computed with polynomial

            left    0<=x<=0.5:     0.5 * (1-(x-0.5)*(x-0.5)*4)^0.5
            right:  0.5<x<=1.0:   -0.5 * (1-(x-1.0)*(x-1.0)*4)^0.5+0.5
                                     a * sqrt(1-(x+tx)*(x+tx)*4) + b

        hypothesis: the discontinuity at the midpoint represents the threshold point
        where metastable behavior can be attracted

        this value is then used to LERP between the min and max priority limits, mapping
        that range to the 0..1.0 unit range of this sawtooth.

        see doc/CurvedSawtooth_Function.svg
     */
    public static float sawtoothCurved(float x) {
        float tx, a, b;
        if (x < 0.5f) {
            a = +0.5f;
            b = 0f;
            tx = -0.5f;
        } else {
            a = -0.5f;
            b = +0.5f;
            tx = -1f;
        }
        float x0 = (x+tx);
        return (float) (a * Math.sqrt(1f - x0 * x0 * 4) + b);
    }
}

