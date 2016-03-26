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
import nars.concept.util.BeliefTable;
import nars.nal.Tense;
import nars.nal.UtilityFunctions;
import nars.task.Task;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.StrictMath.abs;

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
    public static Truth conversion(@NotNull Truth t, float minConf) {
        float w = and(t.freq(), t.conf());
        float c = w2c(w);
        return (c < minConf) ? null : new DefaultTruth(1, c);
    }

    /* ----- Single argument functions, called in StructuralRules ----- */
    /**
     * {A} |- (--A)
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth negation(@Nullable Truth v1, float minConf) {
        if (v1 == null) return null;

        float c = v1.conf();
        if (c < minConf) return null;

        float fPos = v1.freq();

        //if = 0.5, negating will produce same result
        if (Util.equals(fPos, 0.5f, Global.TRUTH_EPSILON))
            return v1;

        return new DefaultTruth(1.0f - fPos, c);

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
    @Nullable
    public static Truth contraposition(@NotNull Truth v1, float minConf) {
        float w = and(1 - v1.freq(), v1.conf());
        float c = w2c(w);
        return (c < minConf) ? null : new DefaultTruth(0, c);
    }

    /* ----- double argument functions, called in MatchingRules ----- */
    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth revision(@NotNull Truth a, @NotNull Truth b, float match, float minConf) {
        float w1 = c2w(a.conf());
        float w2 = c2w(b.conf());
        float w = (w1 + w2);
        float newConf = w2c(w) * match;
        if (newConf < minConf)
            return null;

        float f1 = a.freq();
        float f2 = b.freq();

        return new DefaultTruth(
            (w1 * f1 + w2 * f2) / w,
            newConf
        );
    }

    @Nullable
    public static Truth revision(@NotNull Task ta, @NotNull Task tb, long now, float match, float confThreshold) {
        Truth a = ta.truth();
        Truth b = tb.truth();

        long at = ta.occurrence();
        long bt = tb.occurrence();

        //temporal proximity balancing metric (similar to projection)
        long adt = Math.abs(at-now);
        long bdt = Math.abs(bt-now);
        float closeness = (adt!=bdt) ? (bdt/(float)(adt+bdt)) : 0.5f;

        float w1 = c2w(a.conf()) * closeness;
        float w2 = c2w(b.conf()) * (1-closeness);

        final float w = (w1 + w2);
        float newConf = w2c(w)
                * temporalIntersection(now, at, bt)
                //* TruthFunctions.temporalProjectionOld(at, bt, now)
                * match;
        if (newConf < confThreshold)
            return null;


        float f1 = a.freq();
        float f2 = b.freq();
        return new DefaultTruth(
            (w1 * f1 + w2 * f2) / w,
            newConf
        );
    }

    public static float temporalIntersection(long now, long at, long bt) {
        return BeliefTable.relevance(Math.abs(now-at) + Math.abs(now-bt), Math.abs(at-bt));
    }
    public static float temporalIntersection(long now, long at, long bt, float dur) {
        return BeliefTable.relevance(Math.abs(now-at) + Math.abs(now-bt), Math.abs(at-bt)/dur);
    }
    public static float truthProjection(long sourceTime, long targetTime, long currentTime) {
        if (sourceTime == targetTime) {
            return 0;
        } else {
            long den = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
            if (den == 0) {
                return 1f;
            } else {
                return abs(sourceTime - targetTime) / (float) den;
            }
        }
    }


    /**
     * {M, <M ==> P>} |- P
     * @param a Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return AnalyticTruth value of the conclusion, because it is structural
     */
    @Nullable
    public static Truth deductionR(@NotNull Truth a, float reliance, float minConf) {
        if (a == null) return null;

        float f = a.freq();
        float c = and(f, a.conf(), reliance);
        return (c < minConf) ? null : new DefaultTruth(f, c);
    }
        /* ----- double argument functions, called in SyllogisticRules ----- */
    /**
     * {<S ==> M>, <M ==> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return (non-Analytic) Truth value of the conclusion - normal truth because this is based on 2 premises
     */
    @Nullable
    public static Truth deduction(@NotNull Truth a, @NotNull Truth b, float minConf) {
        return deductionB(a, b.freq(), b.conf(), minConf);
    }

    /** assumes belief freq=1f */
    @Nullable
    public static Truth deduction1(@NotNull Truth a, float bC, float minConf) {
        return deductionB(a, 1f, bC, minConf);
    }

    @Nullable
    public static Truth deductionB(@NotNull Truth a, float bF, float bC, float minConf) {
        float f = and(a.freq(), bF);
        float c = and(f, a.conf(), bC);
        return c < minConf ? null : new DefaultTruth(f, c);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth analogy(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float fB = b.freq();
        float c = and(a.conf(), b.conf(), fB);
        if (c < minConf) return null;
        float f = and(a.freq(), fB);
        return new DefaultTruth(f, c);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth resemblance(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c = and(a.conf(), b.conf(), or(f1, f2));
        return (c < minConf) ? null : new DefaultTruth(and(f1, f2), c);

    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion, or null if either truth is analytic already
     */
    @Nullable
    public static Truth abduction(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float c = w2c(and(b.freq(), a.conf(), b.conf()));
        return (c < minConf) ? null : new DefaultTruth(a.freq(), c);
    }

//    /**
//     * {M, <P ==> M>} |- P
//     * @param t Truth value of the first premise
//     * @param reliance Confidence of the second (analytical) premise
//     * @return Truth value of the conclusion
//     */
//    @NotNull
//    public static Truth abduction(@NotNull Truth t, float reliance) {
//        float f1 = t.freq();
//        float c1 = t.conf();
//        float w = and(c1, reliance);
//        float c = w2c(w);
//        return new DefaultTruth(f1, c);
//    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth induction(@NotNull Truth v1, @NotNull Truth v2, float minConf) {
        return abduction(v2, v1, minConf);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth exemplification(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float c = w2c(and(a.freq(), b.freq(), a.conf(), b.conf()));
        return c < minConf ? null : new DefaultTruth(1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth comparison(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float f1 = a.freq();
        float f2 = b.freq();
        float f0 = or(f1, f2);
        float c = w2c(and(f0, a.conf(), b.conf()));
        if (c < minConf)
            return null;

        float f = (Util.equals(f0, 0, Global.TRUTH_EPSILON)) ? 0 : (and(f1, f2) / f0);
        return new DefaultTruth(f, c);
    }

    /* ----- desire-value functions, called in SyllogisticRules ----- */
    /**
     * A function specially designed for desire value [To be refined]
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth desireStrong(@NotNull Truth a, @NotNull Truth b, float minConf) {

        float f2 = b.freq();
        float c1 = a.conf();
        float c2 = b.conf();
        float c = and(c1, c2, f2);
        return (c < minConf) ? null : new DefaultTruth(and(a.freq(), f2), c);

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
    public static Truth desireDed(@NotNull Truth v1, @NotNull Truth v2, float minConf) {
        float c = and(v1.conf(), v2.conf());
        return c < minConf ? null : new DefaultTruth(and(v1.freq(), v2.freq()), c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth desireInd(@NotNull Truth v1, @NotNull Truth v2, float minConf) {
        float c = w2c(and(v2.freq(), v1.conf(), v2.conf()));
        return c < minConf ? null : new DefaultTruth(v1.freq(), c);
    }

    /* ----- double argument functions, called in CompositionalRules ----- */
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth union(@NotNull Truth v1, @NotNull Truth v2, float minConf) {
        float c = and(v1.conf(), v2.conf());
        return (c < minConf) ?
                null :
                new DefaultTruth(or(v1.freq(), v2.freq()), c);

    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth intersection(@Nullable Truth v1, @NotNull Truth v2, float minConf) {
        if ((v1 == null) || (v2 == null)) return null;

        float c1 = v1.conf();
        float c2 = v2.conf();
        float c = and(c1, c2);
        if (c < minConf) return null;

        float f1 = v1.freq();
        float f2 = v2.freq();

        float f = and(f1, f2);

        return new DefaultTruth(f, c);
    }

    /**
     * {(||, A, B), (--, B)} |- A
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth reduceDisjunction(@NotNull Truth a, @NotNull Truth b, float minConf) {
        return deductionR(intersection(a, negation(b, minConf), minConf), 1.0f, minConf);
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth reduceConjunction(@NotNull Truth v1, @NotNull Truth v2, float minConf) {
        if (v2 == null) return null;

        Truth n1 = negation(v1, minConf);
        if (n1 == null) return null;

        Truth i12 = intersection(n1, v2, minConf);
        if (i12 == null) return null;

        Truth v11 = deductionR(i12, 1.0f, minConf);
        if (v11 == null) return null;

        return negation(v11, minConf);


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
    public static Truth reduceConjunctionNeg(@NotNull Truth a, @NotNull Truth b,float minConf) {
        return reduceConjunction(a, negation(b, minConf), minConf);
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @NotNull
    public static Truth anonymousAnalogy(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float f1 = a.freq();
        float c1 = a.conf();
        Truth v0 = new DefaultTruth(f1, w2c(c1));
        return analogy(b, v0,minConf);
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
    public static Truth decomposeNegativeNegativeNegative(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float f1 = a.freq();
        float c1 = a.conf();
        float f2 = b.freq();
        float c2 = b.conf();

        float fn = and((1-f1),(1-f2));
        float c = and(fn, c1, c2);
        if (c < minConf) return null;
        return new DefaultTruth(1-fn, c);
    }

    @NotNull
    public static Truth difference(@NotNull Truth a, @NotNull Truth b, float minConf) {
        float c = and(a.conf(), b.conf());
        return (c < minConf) ? null : new DefaultTruth(and(a.freq(), (1 - b.freq())), c);

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
    
//    public static float temporalProjection(long sourceTime, long targetTime, long currentTime) {
//        long den = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
//        if (den == 0) return 1f;
//        return abs(sourceTime - targetTime) / (float)den;
//    }

    /**
     * A function to convert confidence to weight
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static float c2w(float c) {
        return Global.HORIZON * c / (1 - c);
    }
}
