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

public enum DesireFunction implements TruthOperator {

    @SinglePremise
    Negation() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, final Truth B, Memory m, float minConf) {
            return TruthFunctions.negation(T);
        }
    },

    Strong() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.desireStrong(T, B, minConf);
        }
    },
    Weak() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.desireWeak(T, B);
        }
    },

    Induction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.desireInd(T,B);
        }
    },

    @AllowOverlap
    Deduction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B==null) return null;
            return TruthFunctions.desireDed(T,B);
        }
    },

    @SinglePremise
    Identity() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* N/A: */ final Truth B, Memory m, float minConf) {
            //return new DefaultTruth(T.freq(), T.conf());
            return T;
        }
    },

    @SinglePremise
    StructuralStrong() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, final Truth B, @NotNull Memory m, float minConf) {
            return TruthFunctions.desireStrong(T, newDefaultTruth(m), minConf);
        }
    },

    Intersection() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.intersection(T,B);
        }
    };

    @Nullable
    private static Truth newDefaultTruth(@NotNull Memory m) {
        return m.getTruthDefault(Symbols.GOAL /* goal? */);
    }


    static final Map<Term, DesireFunction> atomToTruthModifier = Global.newHashMap(DesireFunction.values().length);

    static {
        for (DesireFunction tm : DesireFunction.values())
            atomToTruthModifier.put($.the(tm.toString()), tm);
    }


    public static DesireFunction get(Term a) {
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