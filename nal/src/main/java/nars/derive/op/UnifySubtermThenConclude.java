package nars.derive.op;

import nars.$;
import nars.Param;
import nars.control.Derivation;
import nars.derive.PrediTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** returns false if the deriver is noticed to have depleted TTL,
 *  thus interrupting all further work being done by it. */
public final class UnifySubtermThenConclude extends UnifyTerm {

    /** which premise component, 0 (task) or 1 (belief) */
    private final int subterm;
    public final @Nullable PrediTerm<Derivation> eachMatch;

    public UnifySubtermThenConclude(int subterm, @NotNull Term pattern, @NotNull PrediTerm<Derivation> eachMatch) {
        super($.func("unify", $.the(subterm), pattern, eachMatch), pattern);
        this.subterm = subterm;
        this.eachMatch = eachMatch;
    }

    @Override
    public final boolean test(@NotNull Derivation d) {
        if (!d.use(Param.TTL_UNIFY))
            return false;

        Term target = subterm == 0 ? d.taskTerm : d.beliefTerm;
        d.forEachMatch = eachMatch;
        d.unify(pattern, target /* current term */, true);
        d.forEachMatch = null;

        return d.live();
    }
}
