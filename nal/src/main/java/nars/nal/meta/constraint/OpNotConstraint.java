package nars.nal.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class OpNotConstraint implements MatchConstraint {

    private final int op;

    public OpNotConstraint(@NotNull Op o) {
        this(o.bit);
    }

    public OpNotConstraint(int opVector) {
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull FindSubst f) {
        return value.op().in(op);
    }

    @NotNull
    @Override
    public String toString() {
        return "opNot:" + Integer.toHexString(op);
    }
}

