/*
 * TruthFunctions.java
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
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.truth;

import nars.Global;
import nars.nal.Tense;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.abs;

/**
 * All truth-value (and desire-value) functions used in logic rules
 */
public final class TruthFunctions extends UtilityFunctions {

    /* ----- Single argument functions, called in MatchingRules ----- */
    /**
     * {<A ==> B>} |- <B ==> A>
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth conversion(@NotNull Truth t) {
        float f1 = t.freq();
        float c1 = t.conf();
        float w = and(f1, c1);
        float c = w2c(w);
        return new DefaultTruth(1, c);
    }

    /* ----- Single argument functions, called in StructuralRules ----- */
    /**
     * {A} |- (--A)
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth negation(@NotNull Truth v1) {
        float fPos = v1.freq();

        //if = 0.5, negating will produce same result
        if (Util.equal(fPos, 0.5f, Global.TRUTH_EPSILON))
            return v1;

        float f = 1.0f - fPos;
        float c = v1.conf();
        return new DefaultTruth(f, c);

        /*

        if (t == null) return null;
        final float f = 1 - t.getFrequency();
        final float c = t.getConfidence();

        if (t.isAnalytic())
            return AnalyticTruth.get(f, c, t); //experimental: for cases where analytic is inverted, to preserve analytic state
        else
            return new DefaultTruth(f, c, t);
            */
    }


    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth contraposition(@NotNull Truth v1) {
        float f1 = v1.freq();
        float c1 = v1.conf();
        float w = and(1 - f1, c1);
        float c = w2c(w);
        return new DefaultTruth(0, c);
    }

    /* ----- double argument functions, called in MatchingRules ----- */
    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth revision(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float w1 = c2w(a.conf());
        float w2 = c2w(b.conf());
        float w = w1 + w2;
        return new DefaultTruth(
                (w1 * f1 + w2 * f2) / w,
                w2c(w)
        );
    }
    @NotNull
    public static Truth revision(@NotNull Task ta, @NotNull Task tb, long now) {
        Truth a = ta.truth();
        Truth b = tb.truth();

        if (a.equals(b)) return a;

        float f1 = a.freq();
        float f2 = b.freq();

        float w1 = c2w(a.conf());
        float w2 = c2w(b.conf());

        //temporal proximity metric (similar to projection)
        long at = ta.occurrence();
        long bt = tb.occurrence();
        if (at != bt) {
            long adt = Math.abs(at-now);
            long bdt = Math.abs(bt-now);
            if (adt!=bdt) {
                float p = adt/((float)(adt+bdt));
                w2 *= p;
                w1 *= (1f-p);
            }
        }

        float w = w1 + w2;

        return new DefaultTruth(
            (w1 * f1 + w2 * f2) / w,
            w2c(w) * TruthFunctions.temporalProjection(at, bt, now)
        );
    }

    /**
     * {M, <M ==> P>} |- P
     * @param a Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return AnalyticTruth value of the conclusion, because it is structural
     */
    @NotNull
    public static Truth deduction(@NotNull Truth a, float reliance) {
        float f = a.freq();
        float c = and(f, a.conf(), reliance);
        return new DefaultTruth(f, c);
    }
        /* ----- double argument functions, called in SyllogisticRules ----- */
    /**
     * {<S ==> M>, <M ==> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return (non-Analytic) Truth value of the conclusion - normal truth because this is based on 2 premises
     */
    @NotNull
    public static Truth deduction(@NotNull Truth a, @NotNull Truth b) {
        return deduction(a, b.freq(), b.conf());
    }

    /** assumes belief freq=1f */
    @NotNull
    public static Truth deduction1(@NotNull Truth a, float bC) {
        return deduction(a, 1f, bC);
    }

    @NotNull
    public static Truth deduction(@NotNull Truth a, float bF, float bC) {
        float f = and(a.freq(), bF);
        float c = and(f, a.conf(), bC);
        return new DefaultTruth(f, c);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth analogy(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new DefaultTruth(f, c);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth resemblance(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float f = and(f1, f2);
        float c = and(c1, c2, or(f1, f2));
        return new DefaultTruth(f, c);
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion, or null if either truth is analytic already
     */
    @NotNull
    public static Truth abduction(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new DefaultTruth(f1, c);
    }

    /**
     * {M, <P ==> M>} |- P
     * @param t Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth abduction(@NotNull Truth t, float reliance) {
        float f1 = t.freq();
        float c1 = t.conf();
        float w = and(c1, reliance);
        float c = w2c(w);
        return new DefaultTruth(f1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth induction(@NotNull Truth v1, @NotNull Truth v2) {
        return abduction(v2, v1);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth exemplification(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float w = and(f1, f2, c1, c2);
        float c = w2c(w);
        return new DefaultTruth(1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth comparison(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float f0 = or(f1, f2);
        float f = (f0 == 0) ? 0 : (and(f1, f2) / f0);
        float w = and(f0, c1, c2);
        float c = w2c(w);
        return new DefaultTruth(f, c);
    }

    /* ----- desire-value functions, called in SyllogisticRules ----- */
    /**
     * A function specially designed for desire value [To be refined]
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth desireStrong(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new DefaultTruth(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth desireWeak(@NotNull Truth v1, @NotNull Truth v2) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c1 = v1.conf();
        float c2 = v2.conf();
        float f = and(f1, f2);
        float c = and(c1, c2, f2, w2c(1.0f));
        return new DefaultTruth(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth desireDed(@NotNull Truth v1, @NotNull Truth v2) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c1 = v1.conf();
        float c2 = v2.conf();
        float f = and(f1, f2);
        float c = and(c1, c2);
        return new DefaultTruth(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth desireInd(@NotNull Truth v1, @NotNull Truth v2) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c1 = v1.conf();
        float c2 = v2.conf();
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new DefaultTruth(f1, c);
    }

    /* ----- double argument functions, called in CompositionalRules ----- */
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth union(@NotNull Truth v1, @NotNull Truth v2) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c1 = v1.conf();
        float c2 = v2.conf();
        float f = or(f1, f2);
        float c = and(c1, c2);
        return new DefaultTruth(f, c);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth intersection(@NotNull Truth v1, @NotNull Truth v2) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c1 = v1.conf();
        float c2 = v2.conf();
        float f = and(f1, f2);
        float c = and(c1, c2);
        return new DefaultTruth(f, c);
    }

    /**
     * {(||, A, B), (--, B)} |- A
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth reduceDisjunction(@NotNull Truth a, @NotNull Truth b) {
        return deduction(intersection(a, negation(b)), 1.0f);
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth reduceConjunction(@NotNull Truth v1, @NotNull Truth v2) {
        Truth v0 = intersection(negation(v1), v2);
        return negation(deduction(v0, 1.0f));

//        AnalyticTruth x = deduction(
//                intersection(negation(a), b),
//                1f
//        );
//        if (x!=null)
//            return x.negate();
//        else
//            return null;
    }

    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth reduceConjunctionNeg(@NotNull Truth a, @NotNull Truth b) {
        return reduceConjunction(a, negation(b));
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth anonymousAnalogy(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        Truth v0 = new DefaultTruth(f1, w2c(c1));
        return analogy(b, v0);
    }

    @NotNull
    public static Truth decomposePositiveNegativeNegative(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        float fn = and(f1,1-f2);
        return new DefaultTruth(1-fn, and(fn,c1,c2));
    }

    @NotNull
    public static Truth decomposeNegativePositivePositive(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        float f = and((1-f1),f2);
        return new DefaultTruth(f, and(f,c1,c2));
    }

    @NotNull
    public static Truth decomposePositiveNegativePositive(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        float f = and(f1,(1-f2));
        return new DefaultTruth(f, and(f,c1,c2));
    }

    @NotNull
    public static Truth decomposeNegativeNegativeNegative(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        float fn = and((1-f1),(1-f2));
        return new DefaultTruth(1-fn, and(fn,c1,c2));
    }

    @NotNull
    public static Truth difference(@NotNull Truth a, @NotNull Truth b) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        return new DefaultTruth(and(f1,(1-f2)), and(c1,c2));
    }

    @NotNull
    public static ProjectedTruth eternalize(@NotNull Truth t) {
        return new ProjectedTruth(
                t.freq(),
                eternalize(t.conf()), Tense.ETERNAL
        );
    }

    public static float eternalize(float conf) {
        return w2c(conf);
    }
    
    public static float temporalProjection(long sourceTime, long targetTime, long currentTime) {
        long den = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
        if (den == 0) return 1f;
        return abs(sourceTime - targetTime) / (float)den;
    }

    /**
     * A function to convert confidence to weight
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static float c2w(float c) {
        return Global.HORIZON * c / (1 - c);
    }
}
