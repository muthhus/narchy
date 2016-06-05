package nars.nal.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class OpConstraint implements MatchConstraint {

    @NotNull
    private final Op op;

    public OpConstraint(@NotNull Op o) {
        op = o;
    }


    @Override
    public boolean invalid(Term assignee, @NotNull Term value, FindSubst f) {

        return value.op()!=op;
    }

    @NotNull
    @Override
    public String toString() {
        return "op:\"" + op.str + '"';
    }
}

