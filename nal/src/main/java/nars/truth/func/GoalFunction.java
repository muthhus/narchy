package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.truth.TruthFunctions.*;

public enum GoalFunction implements TruthOperator {

    Strong() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (T == null || B == null) ? null : desireStrongNew(T, B, minConf);
        }
    },

    Weak() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (T == null || B == null) ? null : desireWeakNew(T, B, minConf);
        }
    },

    @SinglePremise
    Negation() {
        @Override public @Nullable Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return negation(T, minConf);
        }
    },

    Induction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (T == null || B == null) ? null : desireInd(T, B, minConf);
        }
    },

    //@AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return (T == null || B == null) ? null : desireDed(T, B, minConf);
        }
    },

//    //EXPERIMENTAL
//    Abduction() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T, B, minConf);
//        }
//    },


    DecomposePositiveNegativeNegative() {
        @Nullable @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return decompose(T, B, true, false, false, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Nullable @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return decompose(T, B, false, false, false, minConf);
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
    //@AllowOverlap
    IdentityTransform() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    //@AllowOverlap
    @SinglePremise
    StructuralStrong() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return TruthFunctions.desireStrongNew(T, defaultTruth(m), minConf);
        }
    },

    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, final Truth B, @NotNull NAR m, float minConf) {
            return T != null ? deduction1(T, defaultTruth(m).conf(), minConf) : null;
        }
    },

    BeliefStructuralDeduction() {
        @Nullable
        @Override public Truth apply(final Truth T, @Nullable final Truth B, @NotNull NAR m, float minConf) {
            if (B == null) return null;
            return deduction1(B, defaultConfidence(m), minConf);
        }
    },


//    @AllowOverlap @SinglePremise
//    StructuralStrongNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return TruthFunctions.desireStrong(T, defaultTruth(m).negated(), minConf);
//        }
//    },


    Union() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : union(T, B, minConf);
        }
    },


    Intersection() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : intersection(T, B, minConf);
        }
    },

    Difference() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, NAR m, float minConf) {
            return ((B == null) || (T == null)) ? null : difference(T, B, minConf);
        }
    },


    ;

    @NotNull
    private static Truth defaultTruth(@NotNull NAR m) {
        return m.truthDefault(Symbols.GOAL /* goal? */);
    }


    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(GoalFunction.values().length);

    static {
        TruthOperator.permuteTruth(GoalFunction.values(), atomToTruthModifier);
    }


    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }


    private final boolean single;
    private final boolean overlap;

    GoalFunction() {

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

    private static float defaultConfidence(@NotNull NAR m) {
        return m.confidenceDefault(Symbols.GOAL);
    }
}