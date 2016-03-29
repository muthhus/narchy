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

    /** "default" zipping config: prefer newest */
    @NotNull static long[] zip(@NotNull long[] a, @NotNull long[] b) {
        return zip(a, b,
                Global.MAXIMUM_EVIDENTAL_BASE_LENGTH,
                true);
    }

    /***
     * zips two evidentialBase arrays into a new one
     * assumes a and b are already sorted in increasing order
     * the later-created task should be in 'b'
     */
    @NotNull
    static long[] zip(@NotNull long[] a, @NotNull long[] b, int maxLen, boolean newToOld) {

        int aLen = a.length, bLen = b.length;
        int baseLength = Math.min(aLen + bLen, maxLen);
        long[] c = new long[baseLength];


        if (newToOld) {
            //"forward" starts with newes, oldest are trimmed
            int ib = bLen-1, ia = aLen-1;
            for (int i = baseLength-1; i >= 0; ) {
                boolean ha = (ia >=0), hb = (ib >= 0);

                c[i--] = ((ha && hb) ?
                            ((i & 1) > 0) : ha) ?
                            a[ia--] : b[ib--];
            }
        } else {
            //"reverse" starts with oldest, newest are trimmed
            int ib = 0, ia = 0;
            for (int i = 0; i < baseLength; ) {

                boolean ha = ia < aLen, hb = ib < bLen;
                c[i++] = ((ha && hb) ?
                            ((i & 1) > 0) : ha) ?
                            a[ia++] : b[ib++];
            }
        }

        return toSetArray(c, maxLen);
    }

    @NotNull
    static long[] toSetArray(@NotNull long[] x) {
        return toSetArray(x, x.length);
    }

    @NotNull
    static long[] toSetArray(@NotNull long[] x, final int outputLen) {
        int l = x.length;

        if (l < 2)
            return x;

        //1. copy evidentialBase and sort it
        long[] sorted = Arrays.copyOf(x, l);
        Arrays.sort(sorted);

        //2. count unique elements
        long lastValue = -1;
        int uniques = 0; //# of unique items
        int sLen = outputLen;

        for (long v : sorted) {
            if (lastValue != v)
                uniques++;
            lastValue = v;
        }

        if ((uniques == sLen) && (sorted.length == sLen)) {
            //if no duplicates and it's the right size, just return it
            return sorted;
        }

        //3. de-duplicate
        int outSize = Math.min(uniques, sLen);
        long[] dedupAndTrimmed = new long[outSize];
        int uniques2 = 0;
        long lastValue2 = -1;
        for (long v : sorted) {
            if (lastValue2 != v)
                dedupAndTrimmed[uniques2++] = v;
            if (uniques2 == outSize)
                break;
            lastValue2 = v;
        }
        return dedupAndTrimmed;
    }

    static boolean overlapping(@NotNull Stamp a, @NotNull Stamp b) {
        return ((a == b) || overlapping(a.evidence(), b.evidence()));
    }

    /**
     * true if there are any common elements; assumes the arrays are sorted and contain no duplicates
     *
     * @param a evidence stamp in sorted order
     * @param b evidence stamp in sorted order
     */
    static boolean overlapping(@NotNull long[] a, @NotNull long[] b) {

        if (Global.DEBUG) {
//            if (a == null || b == null)
//                throw new RuntimeException("null evidence");
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

    /** originality monotonically decreases with evidence length increase. it must always be < 1 (never equal to one) due to its use in the or(conf, originality) ranking */
    default float originality() {
        return 1.0f / (evidence().length + 1);
    }

    /**
     * deduplicated and sorted version of the evidentialBase.
     * this can always be calculated deterministically from the evidentialBAse
     * since it is the deduplicated and sorted form of it.
     */
    @NotNull
    long[] evidence();

    //Stamp setEvidence(long... evidentialSet);

    @NotNull
    static long[] zip(@NotNull Task a, @NotNull Task b) {
        @Nullable long[] bb = b.evidence();
        @Nullable long[] aa = a.evidence();
        return (a.creation() > b.creation()) ?
                Stamp.zip(bb, aa) :
                Stamp.zip(aa, bb);
    }

    static int evidenceLength(int aLen, int bLen) {
        return Math.max(Global.MAXIMUM_EVIDENTAL_BASE_LENGTH, aLen + bLen);
    }
    static int evidenceLength(Task a, Task b) {
        return evidenceLength(a.evidence().length, b.evidence().length);
    }
}