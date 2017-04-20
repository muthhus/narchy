package nars.derive.meta.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpNotConstraint extends MatchConstraint {

    private final int op;

//    public OpNotConstraint(@NotNull Op o) {
//        this(o.bit);
//    }

    public OpNotConstraint(Term target, int opVector) {
        super("opNot", target, $.the(Integer.toBinaryString(opVector)));
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {
        return value.op().in(op);
    }


}

