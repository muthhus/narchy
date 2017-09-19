package nars.derive.op;

import nars.$;
import nars.Param;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.derive.rule.PremiseRule;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/26/16.
 */
abstract public class UnifyTerm extends AbstractPred<Derivation> {

    @NotNull public final Term pattern;

    UnifyTerm(@NotNull Term id, @NotNull Term pattern) {
        super(id);
        this.pattern = pattern;
    }

    public static Atomic label(int subterm) {
        return (subterm==0? PremiseRule.Task : PremiseRule.Belief);
    }

    /** this will be called prior to UnifySubtermThenConclude.
     * so as part of an And condition, it is legitimate for this
     * to return false and interrupt the procedure when unification fails
     * in the first stage.
     */
    public static final class UnifySubterm extends UnifyTerm {

        /** which premise component, 0 (task) or 1 (belief) */
        private final int subterm;

        public UnifySubterm(int subterm, @NotNull Term pattern) {
            super($.func("unify", UnifyTerm.label(subterm), pattern), pattern);
            this.subterm = subterm;
        }

        @Override
        public final boolean test(@NotNull Derivation d) {
            return d.use(Param.TTL_UNIFY) && pattern.unify(subterm == 0 ? d.taskTerm : d.beliefTerm, d);
        }

    }

    /** returns false if the deriver is noticed to have depleted TTL,
     *  thus interrupting all further work being done by it. */
    public static final class UnifySubtermThenConclude extends UnifyTerm {

        /** which premise component, 0 (task) or 1 (belief) */
        public final int subterm;
        public final @Nullable PrediTerm<Derivation> eachMatch;

        public UnifySubtermThenConclude(int subterm, @NotNull Term pattern, @NotNull PrediTerm<Derivation> eachMatch) {
            super($.func("unify", label(subterm), pattern, eachMatch), pattern);
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
}
