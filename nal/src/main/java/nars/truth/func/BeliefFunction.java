package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.$.t;

/**
 * http://aleph.sagemath.org/?q=qwssnn
 <patham9> only strong rules are allowing overlap
 <patham9> except union and revision
 <patham9> if you look at the graph you see why
 <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum BeliefFunction implements TruthOperator {

//    @SinglePremise
//    Revision() {
//        @Nullable
//        @Override public Truth apply(final Truth T, final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.revision(T, B, 1f, -1f);
//        }
//    },

    StructuralIntersection() {
        @Override public @Nullable Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            return B != null ? TruthFunctions.intersection(B, defaultTruth(m), minConf) : null;
        }
    },


    StructuralAbduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            return (B != null) ? TruthFunctions.abduction(B, defaultTruth(m), minConf, m.dur()) : null;
        }
    },

    //@AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
            return (T == null || B == null) ? null : TruthFunctions.deduction(T, B, minConf);
        }
    },

    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, final Truth B, @NotNull NAR m, float minConf) {
            return T != null ? TruthFunctions.deduction1(T, defaultConfidence(m), minConf) : null;
        }
    },



    Induction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T, B, minConf, m.dur());
        }
    },

//    /** task frequency negated induction */
//    InductionNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T.negated(), B, minConf);
//        }
//    },
//    /** belief frequency negated induction */
//    InductionNegB() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T, B.negated(), minConf);
//        }
//    },

    Abduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T, B, minConf, m.dur());
        }
    },

//    AbductionNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T.negated(), B, minConf);
//        }
//    },

    Comparison() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.comparison(T, B, minConf, m.dur());
        }
    },

//    ComparisonNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.comparison(T, B, true, minConf);
//        }
//    },

    Conversion() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (B == null) ? null : TruthFunctions.conversion(B, minConf, m.dur());
        }
    },

//    @SinglePremise
//    Negation() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, /* nullable */ final Truth B, @NotNull Memory m, float minConf) {
//            return TruthFunctions.negation(T, minConf);
//        }
//    },

    @SinglePremise
    Contraposition() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (T == null) ? null : TruthFunctions.contraposition(T, minConf, m.dur());
        }
    },

    //@AllowOverlap
    Resemblance() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.resemblance(T, B, minConf);
        }
    },

    Union() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.union(T, B, minConf);
        }
    },

    Intersection() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.intersection(T, B, minConf);
        }
    },

    Difference() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.difference(T, B, minConf);
        }
    },

    Analogy() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.analogy(T, B, minConf);
        }
    },
    ReduceConjunction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.reduceConjunction(T,B,minConf);
        }
    },

//    ReduceDisjunction() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceDisjunction(T, B,minConf);
//        }
//    },

//    ReduceConjunctionNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceConjunctionNeg(T, B,minConf);
//        }
//    },

    AnonymousAnalogy() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.anonymousAnalogy(T,B,minConf, m.dur());
        }
    },
    Exemplification() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.exemplification(T, B, minConf, m.dur());
        }
    },


    DecomposePositiveNegativeNegative() {
        @Nullable @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, false, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Nullable @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, false, false, minConf);
        }
    },

    DecomposePositiveNegativePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T,B, true, false, true, minConf);
        }
    },

    DecomposeNegativePositivePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T,B, false, true, true, minConf);
        }
    },

    DecomposePositivePositivePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T,B, true, true, true, minConf);
        }
    },



    @SinglePremise
    Identity() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    /** same as identity but allows overlap */
    @SinglePremise
    @AllowOverlap
    IdentityTransform() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    BeliefIdentity() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(B, minConf);
        }


    },

    //@AllowOverlap
    BeliefStructuralDeduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralAbduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.abduction(B, $.t(1f, defaultConfidence(m)), minConf, m.dur());
        }
    },

    //@AllowOverlap
    BeliefStructuralAnalogy() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.analogy(B, $.t(1f, defaultConfidence(m)), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralDifference() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            if (B == null) return null;
            Truth res =  TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
            return (res != null) ? t(1.0f - res.freq(), res.conf()) : null;
        }
    },

//    BeliefNegation() {
//        @Nullable
//        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return (B == null) ? null : TruthFunctions.negation(B, minConf);
//        }
//    }

    ;

    @NotNull
    private static Truth defaultTruth(@NotNull NAR m) {
        return m.truthDefault(Op.BELIEF);
    }

    private static float defaultConfidence(@NotNull NAR m) {
        return m.confDefault(Op.BELIEF);
    }



//    /**
//     * @param T taskTruth
//     * @param B beliefTruth (possibly null)
//     * @return
//     */
//    @Override
//    abstract public Truth apply(Truth T, Truth B, @NotNull Memory m);



    //TODO use an enum map with terms bound to the enum values directly
    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(BeliefFunction.values().length);



    static {
        TruthOperator.permuteTruth(BeliefFunction.values(), atomToTruthModifier);
    }

    @Nullable
    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }


    private final boolean single;
    private final boolean overlap;

    BeliefFunction() {

        try {
            Field enumField = getClass().getField(name());
            this.single = enumField.isAnnotationPresent(SinglePremise.class);
            this.overlap = enumField.isAnnotationPresent(AllowOverlap.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public final boolean single() {
        return single;
    }

    @Override
    public final boolean allowOverlap() {
        return overlap;
    }

}
