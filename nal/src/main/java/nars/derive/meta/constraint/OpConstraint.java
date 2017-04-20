package nars.derive.meta.constraint;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpConstraint extends MatchConstraint {

    @NotNull
    private final Op op;

    public OpConstraint(Term target, @NotNull Op o) {
        super("op", target, $.quote(o.toString()));
        op = o;
    }


    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {

        return value.op()!=op;
    }

}

