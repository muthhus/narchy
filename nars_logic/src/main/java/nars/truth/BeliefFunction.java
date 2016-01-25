package nars.truth;

import nars.$;
import nars.Global;
import nars.Memory;
import nars.Symbols;
import nars.nal.meta.TruthOperator;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * http://aleph.sagemath.org/?q=qwssnn
 <patham9> only strong rules are allowing overlap
 <patham9> except union and revision
 <patham9> if you look at the graph you see why
 <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum BeliefFunction implements TruthOperator {

    Revision() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, @NotNull final Truth B, Memory m) {
            //if (B == null) return null;
            return TruthFunctions.revision(T, B);
        }
    },
    StructuralIntersection() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m) {
            if (B == null) return null;
            return TruthFunctions.intersection(B, newDefaultTruth(m));
        }
    },
    StructuralDeduction() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, final Truth B, @NotNull Memory m) {
            //if (B == null) return null;
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
    Deduction(true) {
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
    Negation() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m) {
            return TruthFunctions.negation(T);
        }
    },
    Contraposition() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m) {
            return TruthFunctions.contraposition(T);
        }
    },
    Resemblance(true) {
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
    Intersection(true) {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.intersection(T,B);
        }
    },
    Difference(true) {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.difference(T,B);
        }
    },
    Analogy(true) {
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
    Identity() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* nullable*/ final Truth B, Memory m) {
            return new DefaultTruth(T.freq(), T.conf());
        }
    },
    BeliefIdentity() {
        @Nullable
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return new DefaultTruth(B.freq(), B.conf());
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
        return m.newDefaultTruth(Symbols.JUDGMENT);
    }

    public static float defaultConfidence(@NotNull Memory m) {
        return m.getDefaultConfidence(Symbols.JUDGMENT);
    }


    public final boolean allowOverlap;


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

    BeliefFunction() {
        this(false);
    }

    BeliefFunction(boolean allowOverlap) {
        this.allowOverlap = allowOverlap;
    }


    @Override
    public final boolean allowOverlap() {
        return allowOverlap;
    }
}
