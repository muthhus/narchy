package nars.derive.meta.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class StructureExclusionConstraint extends MatchConstraint {

    private final int structure;

//    public OpNotContainedConstraint(@NotNull Op o) {
//        this(o.bit);
//    }

    public StructureExclusionConstraint(Term target, int opVector) {
        super("StructExcl", target, $.the(Integer.toHexString(opVector)));
        this.structure = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {
        return value.hasAny(structure);
    }

}

