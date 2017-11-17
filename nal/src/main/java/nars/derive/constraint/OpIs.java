package nars.derive.constraint;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpIs extends MatchConstraint {

    @NotNull
    private final Op op;

    public OpIs(Term target, /*@NotNull*/ Op o) {
        super(target, "OpIs", $.quote(o.toString()));
        op = o;
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        return y.op()!=op;
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}

