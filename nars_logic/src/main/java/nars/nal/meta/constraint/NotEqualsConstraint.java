package nars.nal.meta.constraint;

import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class NotEqualsConstraint implements MatchConstraint {

    private final Term b;

    public NotEqualsConstraint(Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(Term x, @NotNull Term y, @NotNull FindSubst f) {
        Term canNotEqual = f.xy.getXY(b);
        return (canNotEqual != null) && y.equals(canNotEqual);
    }

    @NotNull
    @Override
    public String toString() {
        return "neq:" + b;
    }
}
