package nars.nal.meta.op;

import nars.nal.meta.PremiseEval;
import nars.nal.meta.ProcTerm;
import nars.nal.meta.constraint.MatchConstraint;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class MatchOneSubterm extends MatchTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;
    private final int matchFactor;

    public MatchOneSubterm(@NotNull Term id, int subterm, @NotNull Term pattern, @Nullable MatchConstraint constraints, @Nullable ProcTerm eachMatch, int matchFactor) {
        super(id, pattern, constraints, eachMatch);
        this.subterm = subterm;
        this.matchFactor = matchFactor;
    }

    @Override
    public final boolean booleanValueOf(@NotNull PremiseEval p) {
        p.matchAll(pattern, subterm == 0 ? p.taskTerm : p.beliefTerm /* current term */, eachMatch, constraints, matchFactor);
        return true;
    }
}
