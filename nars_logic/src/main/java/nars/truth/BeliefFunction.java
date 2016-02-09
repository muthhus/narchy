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
        @Override public Truth apply(@NotNull final Truth T, @NotNull final Truth B, Memory m, float minConf) {
            //if (B == null) return null;
            return TruthFunctions.revision(T, B, 1f, -1f);
        }
    },

    StructuralIntersection() {
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.intersection(B, newDefaultTruth(m));
        }
    },

    @SinglePremise
    StructuralDeduction() {
        @Override public Truth apply(@NotNull final Truth T, final Truth B, @NotNull Memory m, float minConf) {
            return TruthFunctions.deduction1(T, defaultConfidence(m));
        }
    },

    StructuralAbduction() {
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.abduction(B, newDefaultTruth(m), minConf);
        }
    },

    Deduction() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.deduction(T, B);
        }
    },

    Induction() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.induction(T, B, minConf);
        }
    },

    @AllowOverlap
    InductionOverlappable() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.induction(T, B, minConf);
        }
    },

    Abduction() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.abduction(T, B, minConf);
        }
    },

    Comparison() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.comparison(T, B);
        }
    },

    Conversion() {
        @Override public Truth apply(final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.conversion(B);
        }
    },

    @SinglePremise
    Negation() {
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m, float minConf) {
            return TruthFunctions.negation(T);
        }
    },

    @SinglePremise
    Contraposition() {
        @Override public Truth apply(@NotNull final Truth T, /* nullable */ final Truth B, Memory m, float minConf) {
            return TruthFunctions.contraposition(T,minConf);
        }
    },

    @AllowOverlap
    Resemblance() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.resemblance(T,B);
        }
    },

    Union() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.union(T,B,minConf);
        }
    },

    @AllowOverlap
    Intersection() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.intersection(T,B);
        }
    },

    @AllowOverlap
    Difference() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.difference(T,B);
        }
    },

    @AllowOverlap
    Analogy() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.analogy(T,B);
        }
    },
    ReduceConjunction() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.reduceConjunction(T,B);
        }
    },
    ReduceDisjunction() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.reduceDisjunction(T, B);
        }
    },
    ReduceConjunctionNeg() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.reduceConjunctionNeg(T, B);
        }
    },
    AnonymousAnalogy() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.anonymousAnalogy(T,B);
        }
    },
    Exemplification() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.exemplification(T,B);
        }
    },
    DecomposeNegativeNegativeNegative() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativeNegativeNegative(T,B);
        }
    },
    DecomposePositiveNegativePositive() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.decomposePositiveNegativePositive(T,B);
        }
    },
    DecomposeNegativePositivePositive() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativePositivePositive(T,B);
        }
    },
    DecomposePositivePositivePositive() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.decomposeNegativePositivePositive(TruthFunctions.negation(T), B);
        }
    },
    DecomposePositiveNegativeNegative() {
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.decomposePositiveNegativeNegative(T,B);
        }
    },

    @SinglePremise
    Identity() {
        @Override public Truth apply(@NotNull final Truth T, /* nullable*/ final Truth B, Memory m, float minConf) {
            return T;
            //return new DefaultTruth(T.freq(), T.conf());
        }
    },

    BeliefIdentity() {
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, Memory m, float minConf) {
            //if (B == null) return null;
            //return new DefaultTruth(B.freq(), B.conf());
            return B;
        }
    },

    BeliefStructuralDeduction() {
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.deduction1(B, defaultConfidence(m));
        }
    },
    BeliefStructuralDifference() {
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            Truth res =  TruthFunctions.deduction1(B, defaultConfidence(m));
            return new DefaultTruth(1.0f-res.freq(), res.conf());
        }
    },
    BeliefNegation() {
        @Override public Truth apply(final Truth T, /* nullable*/ @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.negation(B);
        }
    };


    @Nullable
    public static Truth newDefaultTruth(@NotNull Memory m) {
        return m.getTruthDefault(Symbols.BELIEF);
    }

    public static float defaultConfidence(@NotNull Memory m) {
        return m.getDefaultConfidence(Symbols.BELIEF);
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
