package nars.derive.op;

import nars.$;
import nars.control.premise.Derivation;
import nars.derive.PrediTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class MatchOneSubterm extends MatchTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;

    public MatchOneSubterm(int subterm, @NotNull Term pattern, @Nullable PrediTerm eachMatch) {
        super(eachMatch!=null ?
                $.func("unify", $.the(subterm), pattern, eachMatch) :
                $.func("unify", $.the(subterm), pattern),
                pattern, eachMatch);
        this.subterm = subterm;
    }

    @Override
    public final boolean test(@NotNull Derivation p) {
        return p.matchAll(pattern, subterm == 0 ? p.taskTerm : p.beliefTerm /* current term */, eachMatch);
    }
}
