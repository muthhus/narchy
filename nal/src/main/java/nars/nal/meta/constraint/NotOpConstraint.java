package nars.nal.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/13/15.
 */
public final class NotOpConstraint implements MatchConstraint {

    private final int op;

    public NotOpConstraint(@NotNull Op o) {
        this(o.bit());
    }

    public NotOpConstraint(int opVector) {
        this.op = opVector;
    }

    @Override
    public boolean invalid(Term assignee, @NotNull Term value, FindSubst f) {
        return value.op().in(op);
    }
    @NotNull
    @Override
    public String toString() {
        return "notOp:" + Integer.toHexString(op);
    }
}

