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

public enum DesireFunction implements TruthOperator {

    @SinglePremise
    Negation() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return TruthFunctions.negation(T, minConf);
        }
    },

    Strong() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (T == null || B == null) ? null : TruthFunctions.desireStrong(T, B, minConf);
        }
    },

    Weak() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (T == null || B == null) ? null : TruthFunctions.desireWeak(T, B, minConf);
        }
    },

    Induction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return B == null ? null : TruthFunctions.desireInd(T, B, minConf);
        }
    },

    //@AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return (T == null || B == null) ? null : TruthFunctions.desireDed(T, B, minConf);
        }
    },

//    //EXPERIMENTAL
//    Abduction() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return ((B == null) || (T == null)) ? null : TruthFunctions.abduction(T, B, minConf);
//        }
//    },



    @SinglePremise
    Identity() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    @AllowOverlap @SinglePremise
    StructuralStrong() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            return TruthFunctions.desireStrong(T, defaultTruth(m), minConf);
        }
    },

    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @NotNull
        @Override public Truth apply(@Nullable final Truth T, final Truth B, @NotNull Memory m, float minConf) {
            return T != null ? TruthFunctions.deduction1(T, defaultTruth(m).conf(), minConf) : null;
        }
    },

//    @AllowOverlap @SinglePremise
//    StructuralStrongNeg() {
//        @Nullable
//        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
//            return TruthFunctions.desireStrong(T, defaultTruth(m).negated(), minConf);
//        }
//    },

    Intersection() {
        @Nullable
        @Override public Truth apply(@Nullable final Truth T, @Nullable final Truth B, @NotNull Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.intersection(T,B,minConf);
        }
    },


    ;

    @Nullable
    private static Truth defaultTruth(@NotNull Memory m) {
        return m.truthDefault(Symbols.GOAL /* goal? */);
    }


    static final Map<Term, TruthOperator> atomToTruthModifier = Global.newHashMap(DesireFunction.values().length);

    static {
        TruthOperator.permuteTruth(DesireFunction.values(), atomToTruthModifier);
    }


    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }


    private final boolean single;
    private final boolean overlap;

    DesireFunction() {

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