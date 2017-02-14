package nars.derive.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpNotConstraint implements MatchConstraint {

    private final int op;

//    public OpNotConstraint(@NotNull Op o) {
//        this(o.bit);
//    }

    public OpNotConstraint(int opVector) {
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {
        return value.op().in(op);
    }

    @NotNull
    @Override
    public String toString() {
        return "opNot:" + Integer.toHexString(op);
    }
}

