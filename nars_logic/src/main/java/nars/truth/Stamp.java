/*
 * Stamp.java
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
 * GNU General Pbulic License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.truth;

import nars.Global;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public interface Stamp {

    /***
     * zips two evidentialBase arrays into a new one
     */
    @NotNull
    static long[] zip(@NotNull long[] a, @NotNull long[] b) {

        int baseLength = Math.min(a.length + b.length, Global.MAXIMUM_EVIDENTAL_BASE_LENGTH);

        long[] c = new long[baseLength];

        int firstLength = a.length;
        int secondLength = b.length;

        int i2 = 0, j = 0;
        //https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/entity/Stamp.java#143
        while (i2 < secondLength && j < baseLength) {
            c[j++] = b[i2++];
        }
        int i1 = 0;
        while (i1 < firstLength && j < baseLength) {
            c[j++] = a[i1++];
        }
        return c;
    }

    @NotNull
    static long[] toSetArray(@NotNull long[] x) {
        int l = x.length;

        if (l < 2)
            return x;

        //1. copy evidentialBase and sort it
        long[] sorted = Arrays.copyOf(x, l);
        Arrays.sort(sorted);

        //2. count unique elements
        long lastValue = -1;
        int uniques = 0; //# of unique items
        int sLen = sorted.length;

        for (long v : sorted) {
            if (lastValue != v)
                uniques++;
            lastValue = v;
        }

        if (uniques == sLen) {
            //if no duplicates, just return it
            return sorted;
        }

        //3. de-duplicate
        long[] deduplicated = new long[uniques];
        int uniques2 = 0;
        long lastValue2 = -1;
        for (long v : sorted) {
            if (lastValue2 != v)
                deduplicated[uniques2++] = v;
            lastValue2 = v;
        }
        return deduplicated;
    }

    static boolean overlapping(@NotNull Stamp a, @Nullable Stamp b) {

        if (b == null) return false;
        if (a.equals(b)) return true;

        return overlapping(a.evidence(), b.evidence());
    }

    /**
     * true if there are any common elements; assumes the arrays are sorted and contain no duplicates
     * @param a evidence stamp in sorted order
     * @param b evidence stamp in sorted order
     */
    static boolean overlapping(@NotNull long[] a, @NotNull long[] b) {

        if (Global.DEBUG) {
            if (a.length == 0 || b.length == 0) {
                throw new RuntimeException("missing evidence");
            }
        }

        /** TODO there may be additional ways to exit early from this loop */

        for (long x : a) {
            for (long y : b) {
                if (x == y) {
                    return true; //commonality detected
                } else if (y > x) {
                    //any values after y in b will not be equal to x
                    break;
                }
            }
        }
        return false;
    }


    long creation();

    @NotNull
    Stamp setCreationTime(long t);

    default float getOriginality() {
        return 1.0f / (evidence().length + 1);
    }

    /**
     * deduplicated and sorted version of the evidentialBase.
     * this can always be calculated deterministically from the evidentialBAse
     * since it is the deduplicated and sorted form of it.
     */
    @Nullable
    long[] evidence();

    //Stamp setEvidence(long... evidentialSet);

    @NotNull
    static long[] zip(@NotNull Task parentTask, @NotNull Task parentBelief) {

        long[] as = parentTask.evidence();
        long[] bs = parentBelief.evidence();

        return parentTask.creation() > parentBelief.creation() ?
                Stamp.zip(bs, as) :
                Stamp.zip(as, bs);
    }
}