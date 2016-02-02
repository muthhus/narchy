package nars.truth;

import nars.$;
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

/**
 * http://aleph.sagemath.org/?q=qwssnn
 <patham9> only strong rules are allowing overlap
 <patham9> except union and revision
 <patham9> if you look at the graph you see why
 <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum BeliefFunction implements TruthOperator {

    @SinglePremise
    Revision() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, @NotNull final Truth B, Memory m) {
            //if (B == null) return null;
            return TruthFunctions.revision(T, B, 1f, 0f);
        }
    },

    StructuralIntersection() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m) {
            if (B == null) return null;
            return TruthFunctions.intersection(B, newDefaultTruth(m));
        }
    },

    @SinglePremise
    StructuralDeduction() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, final Truth B, @NotNull Memory m) {
            return TruthFunctions.deduction1(T, defaultConfidence(m));
        }
    },

    StructuralAbduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m) {
            if (B == null) return null;
            return TruthFunctions.abduction(B, newDefaultTruth(m));
        }
    },

    @AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.deduction(T, B);
        }
    },

    Induction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.induction(T, B);
        }
    },

    Abduction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.abduction(T, B);
        }
    },

    Comparison() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.comparison(T, B);
        }
    },

    Conversion() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.conversion(B);
        }
    },

    @SinglePremise
    Negation() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m) {
            return TruthFunctions.negation(T);
        }
    },

    @SinglePremise
    Contraposition() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m) {
            return TruthFunctions.contraposition(T);
        }
    },

    @AllowOverlap
    Resemblance() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.resemblance(T,B);
        }
    },

    Union() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.union(T,B);
        }
    },

    @AllowOverlap
    Intersection() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.intersection(T,B);
        }
    },

    @AllowOverlap
    Difference() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.difference(T,B);
        }
    },

    @AllowOverlap
    Analogy() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.analogy(T,B);
        }
    },
    ReduceConjunction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.reduceConjunction(T,B);
        }
    },
    ReduceDisjunction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.reduceDisjunction(T, B);
        }
    },
    ReduceConjunctionNeg() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.reduceConjunctionNeg(T, B);
        }
    },
    AnonymousAnalogy() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.anonymousAnalogy(T,B);
        }
    },
    Exemplification() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.exemplification(T,B);
        }
    },
    DecomposeNegativeNegativeNegative() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativeNegativeNegative(T,B);
        }
    },
    DecomposePositiveNegativePositive() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.decomposePositiveNegativePositive(T,B);
        }
    },
    DecomposeNegativePositivePositive() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativePositivePositive(T,B);
        }
    },
    DecomposePositivePositivePositive() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativePositivePositive(TruthFunctions.negation(T), B);
        }
    },
    DecomposePositiveNegativeNegative() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.decomposePositiveNegativeNegative(T,B);
        }
    },

    @SinglePremise
    Identity() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable*/ final Truth B, Memory m) {
            return T;
            //return new DefaultTruth(T.freq(), T.conf());
        }
    },

    BeliefIdentity() {
        @Nullable
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, Memory m) {
            //if (B == null) return null;
            //return new DefaultTruth(B.freq(), B.conf());
            return B;
        }
    },

    BeliefStructuralDeduction() {
        @Nullable
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, @NotNull Memory m) {
            if (B == null) return null;
            return TruthFunctions.deduction1(B, defaultConfidence(m));
        }
    },
    BeliefStructuralDifference() {
        @Nullable
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, @NotNull Memory m) {
            if (B == null) return null;
            Truth res =  TruthFunctions.deduction1(B, defaultConfidence(m));
            return new DefaultTruth(1.0f-res.freq(), res.conf());
        }
    },
    BeliefNegation() {
        @Nullable
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.negation(B);
        }
    };


    @Nullable
    public static Truth newDefaultTruth(@NotNull Memory m) {
        return m.getTruthDefault(Symbols.JUDGMENT);
    }

    public static float defaultConfidence(@NotNull Memory m) {
        return m.getDefaultConfidence(Symbols.JUDGMENT);
    }



//    /**
//     * @param T taskTruth
//     * @param B beliefTruth (possibly null)
//     * @return
//     */
//    @Override
//    abstract public Truth apply(Truth T, Truth B, Memory m);



    static final Map<Term, BeliefFunction> atomToTruthModifier = Global.newHashMap(BeliefFunction.values().length);

    static {
        for (BeliefFunction tm : BeliefFunction.values())
            atomToTruthModifier.put($.the(tm.toString()), tm);
    }

    public static BeliefFunction get(Term a) {
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
