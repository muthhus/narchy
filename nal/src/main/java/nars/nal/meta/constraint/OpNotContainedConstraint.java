package nars.nal.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class OpNotContainedConstraint implements MatchConstraint {

    private final int op;

    public OpNotContainedConstraint(@NotNull Op o) {
        this(o.bit);
    }

    public OpNotContainedConstraint(int opVector) {
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull FindSubst f) {
        return value.hasAny(op);
    }

    @NotNull
    @Override
    public String toString() {
        return "opNotContained:" + Integer.toHexString(op);
    }
}

