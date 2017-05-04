package nars.derive.meta.constraint;

import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class NotEqualConstraint extends MatchConstraint {

    private final Term other;

    public NotEqualConstraint(Term target, Term other) {
        super("neq", target, other);
        this.other = other;
    }

    @Override
    public int cost() {
        return 3;
    }

    @Override
    public boolean invalid(@NotNull Term y, @NotNull Unify f) {
        @Nullable Term canNotEqual = f.resolve(other);
        return canNotEqual!=null &&
                //Terms.equalAtemporally(y, canNotEqual);
                y.equals(canNotEqual);
    }


}
