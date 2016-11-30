package nars.nal.meta.op;

import nars.nal.meta.BoolCondition;
import nars.nal.meta.Derivation;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class MatchOneSubterm extends MatchTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;

    public MatchOneSubterm(@NotNull Term id, int subterm, @NotNull Term pattern, @Nullable MatchConstraint constraints, @Nullable BoolCondition eachMatch) {
        super(id, pattern, constraints, eachMatch);
        this.subterm = subterm;
    }

    @Override
    public final boolean run(@NotNull Derivation p, int now) {
        p.matchAll(pattern, subterm == 0 ? p.taskTerm : p.beliefTerm /* current term */, eachMatch, constraints, 1);
        return true;
    }
}
