package nars.derive.meta.op;

import nars.control.premise.Derivation;
import nars.derive.meta.BoolPred;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class MatchOneSubterm extends MatchTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;

    public MatchOneSubterm(@NotNull Compound id, int subterm, @NotNull Term pattern, @Nullable BoolPred eachMatch) {
        super(id, pattern, eachMatch);
        this.subterm = subterm;
    }

    @Override
    public final boolean test(@NotNull Derivation p) {
        return p.matchAll(pattern, subterm == 0 ? p.taskTerm : p.beliefTerm /* current term */, eachMatch);
    }
}
