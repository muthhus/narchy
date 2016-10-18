/*
 * TruthValue.java
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
package nars.truth;

import nars.$;
import nars.Param;
import nars.Symbols;
import nars.term.Term;
import nars.util.Texts;
import nars.util.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Comparator;

import static nars.truth.TruthFunctions.*;


/** scalar (1D) truth value "frequency", stored as a floating point value */
public interface Truth extends Truthed {



    Term Truth_TRUE = $.the("TRUE");
    Term Truth_FALSE = $.the("FALSE");
    Term Truth_UNSURE = $.the("UNSURE");
    Comparator<Truthed> compareConfidence = (o1, o2) -> Float.compare(o2.truth().conf(), o1.truth().conf());


    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    @Override
    float freq();


    @Nullable
    @Override
    default Truth truth() { return this; }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    @Override
    default float expectation() {
        return TruthFunctions.expectation(freq(), conf());
    }

    //DONT USE THIS IT IS BIASED AGAINST NEGATIVE FREQUENCY TRUTH
//    /**
//     * Calculate the absolute difference of the expectation value and that of a
//     * given truth value
//     *
//     * @param t The given value
//     * @return The absolute difference
//     */
//    default float getExpDifAbs(@NotNull Truth t) {
//        return Math.abs(expectation() - t.expectation());
//    }

    /**
     * Check if the truth value is negative
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is less than 1/2
     */
    default boolean isNegative() {
        return freq() < 0.5f;
    }

    /**
     * Check if the truth value is negative.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is greater than 1/2
     */
    default boolean isPositive() {
        return freq() > 0.5f;
    }


    /**
     * The hash code of a TruthValue, perfectly condensed,
     * into the two 16-bit words of a 32-bit integer.
     *
     * Since the same epsilon used in other truth
     * resolution here (Truth components do not need the full
     * resolution of a floating point value, and also do not
     * need the full resolution of a 16bit number when discretized)
     * the hash value can be used for equality comparisons
     * as well as non-naturally ordered / non-lexicographic
     * but deterministic compareTo() ordering.
     */
    static int hash(@NotNull Truth t, int hashDiscreteness) {
        return hash(t.freq(), t.conf(), hashDiscreteness);
    }

    static int hash(float freq, float conf) {
        return hash(freq, conf, Param.TRUTH_EPSILON);
    }

    static int hash(float freq, float conf, float epsilon) {
        return hash(freq, conf, (int)(1f/epsilon));
    }

    static int hash(float freq, float conf, int hashDiscreteness) {
        //assuming epsilon is large enough such that: 0 <= h < 2^15:
        int freqHash = Util.hash(freq, hashDiscreteness);
        int confHash = Util.hash(conf, hashDiscreteness);

        return (freqHash << 16) | confHash;
    }

    static Truth unhash(int h, float epsilon) {
        return unhash(h, (int)(1f/epsilon));
    }

    static Truth unhash(int h, int hashDiscreteness) {
        return $.t(
                Util.unhash((h>>16) & 0xffff, hashDiscreteness),
                Util.unhash(h & 0xffff, hashDiscreteness)
        );
    }


    @NotNull
    default Appendable appendString(@NotNull Appendable sb) throws IOException {
        return appendString(sb, 2);
    }


    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     */
    @NotNull
    default Appendable appendString(@NotNull Appendable sb, int decimals) throws IOException {
        /*String s1 = DELIMITER + frequency.toStringBrief() + SEPARATOR;
        String s2 = confidence.toStringBrief();
        if (s2.equals("1.00")) {
            return s1 + "0.99" + DELIMITER;
        } else {
            return s1 + s2 + DELIMITER;
        }*/
        
        //sb.ensureCapacity(3 + 2 * (2 + decimals) );
        return sb
            .append(Symbols.TRUTH_VALUE_MARK)
            .append(Texts.n(freq(), decimals))
            .append(Symbols.VALUE_SEPARATOR)
            .append(Texts.n(conf(), decimals))
            .append(Symbols.TRUTH_VALUE_MARK);
    }


    //    /** displays the truth value as a short string indicating degree of true/false */
//    @Nullable
//    default String toTrueFalseString() {
//        //TODO:
//        //  F,f,~,t,T
//        return null;
//    }
//
//    /** displays the truth value as a short string indicating degree of yes/no */
//    @Nullable
//    default String toYesNoString() {
//        //TODO
//        // N,n,~,y,Y
//        return null;
//    }

    
    @NotNull
    default Term expectation(float e, float trueExpectationThreshold) {


        if (e > trueExpectationThreshold) {
            return Truth_TRUE;
        }
        if (e < 1f - trueExpectationThreshold) {
            return Truth_FALSE;
        }
        return Truth_UNSURE;
    }



    static int compare(@NotNull Truth a, @NotNull Truth b) {
        if (a == b) return 0;

        //see how Truth hash() is calculated to know why this works
        return Integer.compare(b.hashCode(), a.hashCode());

//        tc = Float.compare(truth.getFrequency(), otruth.getFrequency());
//        if (tc!=0) return tc;
//        tc = Float.compare(truth.getConfidence(), otruth.getConfidence());
//        if (tc!=0) return tc;
//
//        return 0;
    }


    /** clones a copy with confidence multiplied. null if conf < epsilon */
    @Nullable Truth confMult(float f);

    @Nullable Truth withConf(float f);

//    default @Nullable Truth confMultViaWeight(float f) {
//        return withConf(w2c(c2w(conf()) * f));
//    }
//    default @Nullable Truth confMultViaWeightMaxEternal(float f) {
//        if (f == 1f) {
//            return this;
//        }
//        else {
//            float c = conf();
//            return withConf(
//                    Math.max(eternalize(), w2c(c2w(c) * f)));
//        }
//    }


//    @NotNull
//    default Truth project(long when, long occ, long now, float dur) {
//        if (occ == when || occ == Tense.ETERNAL)
//            return this;
//        return withConf(conf() * temporalIntersection( when, occ, now, dur ));
//    }

//    @NotNull
//    default Truth interpolate(@NotNull Truthed y) {
//        float xc = confWeight();
//        float yc = y.confWeight();
//
//        return new DefaultTruth(
//                //lerp by proportion of confidence contributed
//                lerp(freq(), y.freq(), xc / (xc+yc)),
//
//                //difference in freq means closer to the AND conf, otherwise if they are the same then closer to max
//                lerp(and(xc, yc), max(xc, yc), Math.abs(freq()-y.freq()))
//
//        );
//    }

    /** the negated (1 - freq) of this truth value */
    @NotNull Truth negated();

    default boolean equals(@NotNull Truth x, float tolerance) {
        return Util.equals(freq(), x.freq(), tolerance) && Util.equals(conf(), x.conf(), tolerance);
    }

    default Truth negated(boolean negate) {
        return negate ? negated() : this;
    }

    default int hash(float truthEpsilon) {
        return Truth.hash(freq(), conf(), truthEpsilon);
    }

    default Truth eternalize() {
        return withConf(TruthFunctions.eternalize(conf()));
    }

    static Truth maxConf(Truth a, Truth b) {
        return a.conf() >= b.conf() ? a : b;
    }
//    static <T extends Truthed> T minConf(T a, T b) {
//        return a.conf() <= b.conf() ? a : b;
//    }


    enum TruthComponent {
        Frequency, Confidence, Expectation
    }
    
    default float getComponent(@NotNull TruthComponent c) {
        switch (c) {
            case Frequency: return freq();
            case Confidence: return conf();
            case Expectation: return expectation();
        }
        return Float.NaN;
    }
    
    /** provides a statistics summary (mean, min, max, variance, etc..) of a particular TruthValue component across a given list of Truthables (sentences, TruthValue's, etc..).  null values in the iteration are ignored */
    @NotNull
    static DescriptiveStatistics statistics(@NotNull Iterable<? extends Truthed> t, @NotNull TruthComponent component) {
        DescriptiveStatistics d = new DescriptiveStatistics();
        for (Truthed x : t) {
            Truth v = x.truth();
            if (v!=null)
                d.addValue(v.getComponent(component));
        }
        return d;
    }





}
