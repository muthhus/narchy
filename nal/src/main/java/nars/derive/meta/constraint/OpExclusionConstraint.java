package nars.derive.meta.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpExclusionConstraint extends MatchConstraint {

    private final int op;

//    public OpNotConstraint(@NotNull Op o) {
//        this(o.bit);
//    }

    public OpExclusionConstraint(Term target, int opVector) {
        super("opExcl", target, $.the(Integer.toBinaryString(opVector)));
        this.op = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term y, @NotNull Unify f) {
        return y.op().in(op);
    }


}

