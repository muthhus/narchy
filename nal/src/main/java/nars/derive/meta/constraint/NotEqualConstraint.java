package nars.derive.meta.constraint;

import nars.term.Term;
import nars.term.Terms;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class NotEqualConstraint implements MatchConstraint {

    private final Term b;

    public NotEqualConstraint(Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull Unify f) {
        Term canNotEqual = f.xy.get(b);
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

    @NotNull
    @Override
    public String toString() {
        return "neq:" + b;
    }
}
