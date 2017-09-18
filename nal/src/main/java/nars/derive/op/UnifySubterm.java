package nars.derive.op;

import nars.$;
import nars.Param;
import nars.control.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/** this will be called prior to UnifySubtermThenConclude.
 * so as part of an And condition, it is legitimate for this
 * to return false and interrupt the procedure when unification fails
 * in the first stage.
 */
public final class UnifySubterm extends UnifyTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;

    public UnifySubterm(int subterm, @NotNull Term pattern) {
        super($.func("unify", $.the(subterm), pattern), pattern);
        this.subterm = subterm;
    }

    @Override
    public final boolean test(@NotNull Derivation d) {
        return d.use(Param.TTL_UNIFY) && pattern.unify(subterm == 0 ? d.taskTerm : d.beliefTerm, d);
    }

}
