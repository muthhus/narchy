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
import nars.Symbols;
import nars.nal.Tense;
import nars.term.Term;
import nars.util.Texts;
import nars.util.data.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nars.nal.UtilityFunctions.and;
import static nars.nal.UtilityFunctions.or;
import static nars.truth.TruthFunctions.temporalIntersection;


/** scalar (1D) truth value "frequency", stored as a floating point value */
public interface Truth extends Truthed {



    Term Truth_TRUE = $.the("TRUE");
    Term Truth_FALSE = $.the("FALSE");
    Term Truth_UNSURE = $.the("UNSURE");
    Truth Zero = new DefaultTruth(0.5f, 0);
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
        return expectationPositive();
    }
    @Override
    default float expectation(boolean positive) {
        return positive ? expectationPositive() : expectationNegative();
    }

    /** expectation, the expectation of freq=1 */
    default float expectationPositive() {
        return expectation(freq(), conf());
    }


    /** expectation inverse, the expectation of freq=0  */
    default float expectationNegative() {
        return expectation(1.0f - freq(), conf());
    }

    static float expectation(float frequency, float confidence) {
        return (confidence * (frequency - 0.5f) + 0.5f);
    }

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     *
     * @param t The given value
     * @return The absolute difference
     */
    default float getExpDifAbs(@NotNull Truth t) {
        return Math.abs(expectation() - t.expectation());
    }

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

        //assuming epsilon is large enough such that: 0 <= h < 2^15:
        int freqHash = Util.hash(t.freq(), hashDiscreteness);
        int confHash = Util.hash(t.conf(), hashDiscreteness);

        return (freqHash << 16) | confHash;
    }


    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb) {
        return appendString(sb, 2);
    }


    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     */
    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb, int decimals) {
        /*String s1 = DELIMITER + frequency.toStringBrief() + SEPARATOR;
        String s2 = confidence.toStringBrief();
        if (s2.equals("1.00")) {
            return s1 + "0.99" + DELIMITER;
        } else {
            return s1 + s2 + DELIMITER;
        }*/
        
        sb.ensureCapacity(3 + 2 * (2 + decimals) );
        return sb
            .append(Symbols.TRUTH_VALUE_MARK)
            .append(Texts.n(freq(), decimals))
            .append(Symbols.VALUE_SEPARATOR)
            .append(Texts.n(conf(), decimals))
            .append(Symbols.TRUTH_VALUE_MARK);
    }



    @NotNull
    default CharSequence toCharSequence() {
        StringBuilder sb =  new StringBuilder();
        return appendString(sb);
    }
    
    /** displays the truth value as a short string indicating degree of true/false */
    @Nullable
    default String toTrueFalseString() {
        //TODO:
        //  F,f,~,t,T
        return null;
    }

    /** displays the truth value as a short string indicating degree of yes/no */
    @Nullable
    default String toYesNoString() {
        //TODO
        // N,n,~,y,Y
        return null;
    }

    
    @NotNull
    default Term toWordTerm(float trueExpectationThreshold, boolean negated) {
        float e = !negated ? expectationPositive() : expectationNegative();

        if (e > trueExpectationThreshold) {
            return Truth_TRUE;
        }
        if (e < 1 - trueExpectationThreshold) {
            return Truth_FALSE;
        }
        return Truth_UNSURE;
    }



//    /** negation that modifies the truth instance itself */
//    @NotNull
//    default Truth negate() {
//        //final float f = 1 - getFrequency();
//        return setFrequency(1.0f - freq());
//    }

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


    /** clones a copy with confidence multiplied */
    @NotNull Truth confMult(float f);
    @NotNull Truth withConf(float f);


    @NotNull
    default Truth project(long when, long occ, long now, float dur) {
        if (occ == when || occ == Tense.ETERNAL)
            return this;
        return withConf(conf() * temporalIntersection( when, occ, now, dur ));
    }

    default Truth interpolate(Truth y) {
        float xc = conf();
        float yc = y.conf();
        return new DefaultTruth(
                //lerp by proportion of confidence contributed
                Util.lerp(freq(), y.freq(), xc / (xc+yc)),

                //difference in freq means closer to the AND conf, otherwise if they are the same then closer to max
                Util.lerp(and(xc, yc), max(xc, yc), Math.abs(freq()-y.freq()))

        );
    }



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
