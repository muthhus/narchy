package nars.term.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.transform.FindSubst;
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
        return value.op().isA(op);
    }
    @NotNull
    @Override
    public String toString() {
        return "notOp:" + Integer.toHexString(op);
    }
}

