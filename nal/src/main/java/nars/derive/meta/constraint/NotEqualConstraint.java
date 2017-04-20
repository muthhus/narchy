package nars.derive.meta.constraint;

import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.Terms;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.$.func;


public final class NotEqualConstraint extends MatchConstraint {

    private final Term x;

    public NotEqualConstraint(Term target, Term x) {
        super("neq", target, x);
        this.x = x;
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull Unify f) {
        Term canNotEqual = f.xy.get(this.x);
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        return canNotEqual!=null &&
                Terms.equalAtemporally(y, canNotEqual);
                //y.equals(canNotEqual);
    }


}
