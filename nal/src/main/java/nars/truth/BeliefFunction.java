package nars.truth;

import nars.Global;
import nars.Memory;
import nars.Symbols;
import nars.nal.meta.AllowOverlap;
import nars.nal.meta.SinglePremise;
import nars.nal.meta.TruthOperator;
import nars.term.Term;
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
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.intersection(B, defaultTruth(m), minConf);
        }
    },


    StructuralAbduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (B != null) ? TruthFunctions.abduction(B, defaultTruth(m), minConf) : null;
        }
    },

    //@AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@Nullable Truth T, @Nullable Truth B, @NotNull Memory m, float minConf) {
            return (T == null || B == null) ? null : TruthFunctions.deduction(T, B, minConf);
        }
    },

    @SinglePremise
    //@AllowOverlap
    StructuralDeduction() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, final Truth B, @NotNull Memory m, float minConf) {
            return (T == null) ? null : TruthFunctions.deduction1(T, defaultConfidence(m), minConf);
        }
    },


    Induction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T, B, minConf);
        }
    },

    /** task frequency negated induction */
    InductionNeg() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T.negated(), B, minConf);
        }
    },
    /** belief frequency negated induction */
    InductionNegB() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.induction(T, B.negated(), minConf);
        }
    },

    Abduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T, B, minConf);
        }
    },

    AbductionNeg() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T.negated(), B, minConf);
        }
    },

    Comparison() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.comparison(T, B, minConf);
        }
    },

    ComparisonNeg() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.comparison(T, B, true, minConf);
        }
    },

    Conversion() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (B == null) ? null : TruthFunctions.conversion(B, minConf);
        }
    },

    @SinglePremise
    Negation() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, /* nullable */ final Truth B, @NotNull Memory m, float minConf) {
            return TruthFunctions.negation(T, minConf);
        }
    },

//    @SinglePremise
//    Contraposition() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, /* nullable */ final Truth B, @NotNull Memory m, float minConf) {
//            return (T == null) ? null : TruthFunctions.contraposition(T, minConf);
//        }
//    },

    //@AllowOverlap
    Resemblance() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.resemblance(T, B, minConf);
        }
    },

    Union() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.union(T, B, minConf);
        }
    },


    Intersection() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.intersection(T, B, minConf);
        }
    },

    IntersectionNeg() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.intersection(T, B, true, minConf);
        }
    },

    //@AllowOverlap
    Difference() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.difference(T, B, minConf);
        }
    },

    //@AllowOverlap
    Analogy() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return ((B == null) || (T == null)) ? null : TruthFunctions.analogy(T, B, minConf);
        }
    },
    ReduceConjunction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.reduceConjunction(T,B,minConf);
        }
    },
    ReduceDisjunction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.reduceDisjunction(T, B,minConf);
        }
    },
    ReduceConjunctionNeg() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.reduceConjunctionNeg(T, B,minConf);
        }
    },
    AnonymousAnalogy() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.anonymousAnalogy(T,B,minConf);
        }
    },
    Exemplification() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.exemplification(T, B, minConf);
        }
    },


    DecomposeNegativeNegativePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T, B, false, false, true, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T, B, false, false, false, minConf);
        }
    },
    DecomposePositiveNegativePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T,B, true, false, true, minConf);
        }
    },
    DecomposeNegativePositivePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T,B, false, true, true, minConf);
        }
    },
    DecomposePositivePositivePositive() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T,B, true, true, true, minConf);
        }
    },
    DecomposePositiveNegativeNegative() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.decompose(T,B, true, false, false, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, final Truth B, @NotNull Memory m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    BeliefIdentity() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return TruthOperator.identity(B, minConf);
        }


    },

    //@AllowOverlap
    BeliefStructuralDeduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralDifference() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            Truth res =  TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
            return (res != null) ? t(1.0f - res.freq(), res.conf()) : null;
        }
    },

    BeliefNegation() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (B == null) ? null : TruthFunctions.negation(B, minConf);
        }
    };


    @NotNull
    private static Truth defaultTruth(@NotNull Memory m) {
        return m.truthDefault(Symbols.BELIEF);
    }

    @NotNull
    private static float defaultConfidence(@NotNull Memory m) {
        return m.confidenceDefault(Symbols.BELIEF);
    }



//    /**
//     * @param T taskTruth
//     * @param B beliefTruth (possibly null)
//     * @return
//     */
//    @Override
//    abstract public Truth apply(Truth T, Truth B, @NotNull Memory m);



    //TODO use an enum map with terms bound to the enum values directly
    static final Map<Term, TruthOperator> atomToTruthModifier = Global.newHashMap(BeliefFunction.values().length);



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
    public boolean single() {
        return single;
    }

    @Override
    public boolean allowOverlap() {
        return overlap;
    }

}
