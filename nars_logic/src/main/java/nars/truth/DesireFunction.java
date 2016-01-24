package nars.truth;

import nars.Global;
import nars.Memory;
import nars.Symbols;
import nars.nal.meta.TruthOperator;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public enum DesireFunction implements TruthOperator {

    Negation() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, final Truth B, Memory m) {
            return TruthFunctions.negation(T); }
    },

    Strong() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.desireStrong(T,B);
        }
    },
    Weak() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.desireWeak(T, B);
        }
    },
    Induction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B == null) return null;
            return TruthFunctions.desireInd(T,B);
        }
    },
    Deduction() {
        @Nullable
        @Override public Truth apply(@NotNull final Truth T, @Nullable final Truth B, Memory m) {
            if (B==null) return null;
            return TruthFunctions.desireDed(T,B);
        }
    },
    Identity() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, /* N/A: */ final Truth B, Memory m) {
            return new DefaultTruth(T.freq(), T.conf());
        }
    },
    StructuralStrong() {
        @NotNull
        @Override public Truth apply(@NotNull final Truth T, final Truth B, @NotNull Memory m) {
            return TruthFunctions.desireStrong(T, newDefaultTruth(m));
        }
    };


    @Nullable
    private static Truth newDefaultTruth(@NotNull Memory m) {
        return m.newDefaultTruth(Symbols.JUDGMENT /* goal? */);
    }


    static final Map<Term, DesireFunction> atomToTruthModifier = Global.newHashMap(DesireFunction.values().length);

    static {
        for (DesireFunction tm : DesireFunction.values())
            atomToTruthModifier.put(Atom.the(tm.toString()), tm);
    }

    @Override
    public final boolean allowOverlap() {
        return false;
    }

    public static DesireFunction get(Term a) {
        return atomToTruthModifier.get(a);
    }

}