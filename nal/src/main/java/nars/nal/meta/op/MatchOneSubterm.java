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

    public MatchOneSubterm(@NotNull Term pid, @NotNull Term id, int subterm, @NotNull Term pattern, @Nullable MatchConstraint constraints, @Nullable ProcTerm eachMatch) {
        super(pid, id, pattern, constraints, eachMatch);
        this.subterm = subterm;
    }

    @Override
    public final boolean booleanValueOf(PremiseEval p) {
        p.matchAll(pattern, subterm == 0 ? p.taskTerm : p.beliefTerm /* current term */, eachMatch, constraints);
        return true;
    }
}
