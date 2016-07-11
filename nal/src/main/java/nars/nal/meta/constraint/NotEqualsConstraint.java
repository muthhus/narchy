package nars.nal.meta.constraint;

import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import static nars.term.Terms.equalsAnonymous;


public final class NotEqualsConstraint implements MatchConstraint {

    private final Term b;

    public NotEqualsConstraint(Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(Term x, @NotNull Term y, @NotNull FindSubst f) {
        Term canNotEqual = f.xy.get(b);
        return canNotEqual!=null &&
                equalsAnonymous(y, canNotEqual);
                //y.equals(canNotEqual);
    }

    @NotNull
    @Override
    public String toString() {
        return "neq:" + b;
    }
}
