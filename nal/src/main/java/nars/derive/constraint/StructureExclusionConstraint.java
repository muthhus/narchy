package nars.derive.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class StructureExclusionConstraint extends MatchConstraint {

    private final int structure;

    public StructureExclusionConstraint(Term target, int opVector) {
        super("StructExcl", target, $.the(opVector));
        this.structure = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term y, @NotNull Unify f) {
        return y.hasAny(structure);
    }

}

