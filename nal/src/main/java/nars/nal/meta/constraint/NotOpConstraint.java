package nars.nal.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class NotOpConstraint implements MatchConstraint {

    private final int op;

    public NotOpConstraint(@NotNull Op o) {
        this(o.bit);
    }

    public NotOpConstraint(int opVector) {
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull FindSubst f) {
        return value.op().in(op);
    }

    @NotNull
    @Override
    public String toString() {
        return "notOp:" + Integer.toHexString(op);
    }
}

